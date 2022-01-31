package Services;

import DTOs.ExcelDTO;
import DTOs.QuestionnaireDTO;
import DTOs.UserResponseDTO;
import Models.User;
import Utils.Enums;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

@Service
public class ExcelService implements IExcelService {

    @Autowired
    public IRegistrationNumberService registrationNumberService;
    @Autowired
    public IUnionMembershipNumService unionMemberNumberService;
    @Autowired
    public ITokenService tokenService;

    public ExcelDTO readFromUploadedExcel(ExcelDTO request) throws Exception {
        ExcelDTO response = new ExcelDTO();

        // 1. Check token is valid
        UserResponseDTO userValidateResponse = tokenService.checkToken(request.getTokenUUID());
        response.setAuthSuccess(userValidateResponse.isAuthSuccess());
        response.setSuccessful(userValidateResponse.isSuccessful());
        response.setResponseText(userValidateResponse.getResponseText());

        // If not valid, return the response!
        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        User currentUser = userValidateResponse.getUser();

        // 2. Check the current user of the page is equals to user of token
        if (!currentUser.getId().equals(request.getUser().getId())) {
            response.setExpiredPage(true);
            return response;
        }

        // 3. Check the role is compatible for the requested action
        if (currentUser.getRole().equals(Enums.Role.USER) || currentUser.getRole().equals(Enums.Role.UNION_MEMBER_USER)) {
            response.setExpiredPage(true);
            return response;
        }
        // 4. Refresh token
        userValidateResponse = tokenService.refreshToken(userValidateResponse.getTokenUUID(), currentUser);
        response.setAuthSuccess(userValidateResponse.isAuthSuccess());
        response.setSuccessful(userValidateResponse.isSuccessful());
        response.setResponseText(userValidateResponse.getResponseText());

        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        response.setTokenUUID(userValidateResponse.getTokenUUID());
        response.setUser(userValidateResponse.getUser());

        MultipartFile file = request.getFile();
        try {
            InputStream in = file.getInputStream();
            File currDir = new File(".");
            String path = currDir.getAbsolutePath();
            String fileLocation = path.substring(0, path.length() - 1) + file.getOriginalFilename();
            FileOutputStream f = new FileOutputStream(fileLocation);
            int ch = 0;
            while ((ch = in.read()) != -1) {
                f.write(ch);
            }
            f.flush();
            f.close();

            boolean isForUnionMembers = false;
            if (currentUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN)) isForUnionMembers = true;

            response = readFromExcel(response, fileLocation, isForUnionMembers);
        }
        catch (Exception e) {
            response.setResponseText(getActualResponseText(e, file.getOriginalFilename(), false));
        }
        return response;
    }

    private ExcelDTO readFromExcel(ExcelDTO response, String filePath, boolean forUnionMembers) {

        Exception exception = null;
        Map<Integer, List<String>> dataUnionMembNums = new HashMap<>();
        Map<String, List<String>> dataRegNums = new HashMap<>();
        String[] filePathElements = filePath.split("\\\\");
        String fileName = filePathElements[filePathElements.length - 1];

        try {
            FileInputStream file = new FileInputStream(filePath);
            Workbook workbook = new XSSFWorkbook(file);

            int numberOfSheets = workbook.getNumberOfSheets();

            for (int i = 0; i < numberOfSheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                if (forUnionMembers) dataUnionMembNums.put(i, new ArrayList<>());
                else dataRegNums.put(sheetName, new ArrayList<>());

                for (Row row : sheet) {
                    for (Cell cell : row) {

                        try {
                            String cellValue = cell.getStringCellValue();
                            // Ignore empty cells:
                            if (cellValue.isEmpty()) break;
                            else {
                                // Check can we parse the item to Long. If can, add to the value Map.
                                Long.parseLong(cellValue);
                                if (forUnionMembers) dataUnionMembNums.get(i).add(cellValue);
                                else dataRegNums.get(sheetName).add(cellValue);
                                break;
                            }
                        }
                        catch (Exception e) {
                            if (e.getClass().equals(NumberFormatException.class)) break;
                        }

                        try {
                            if (DateUtil.isCellDateFormatted(cell)) {
                                if (forUnionMembers) dataUnionMembNums.get(i).add(cell.getDateCellValue() + "");
                                else dataRegNums.get(sheetName).add(cell.getDateCellValue() + "");
                                break;
                            } else {
                                Long numericValue = (long) cell.getNumericCellValue();
                                if (forUnionMembers) dataUnionMembNums.get(i).add(numericValue.toString());
                                else dataRegNums.get(sheetName).add(numericValue.toString());
                                break;
                            }
                        }
                        catch (IllegalStateException e) { }

                        try {
                            if (forUnionMembers) dataUnionMembNums.get(i).add(cell.getBooleanCellValue() + "");
                            else dataRegNums.get(sheetName).add(cell.getBooleanCellValue() + "");
                            break;
                        }
                        catch (IllegalStateException e) { }

                        try {
                            dataUnionMembNums.get(i).add(cell.getCellFormula() + "");
                            if (forUnionMembers) dataUnionMembNums.get(i).add(cell.getCellFormula() + "");
                            else dataRegNums.get(sheetName).add(cell.getCellFormula() + "");
                            break;
                        }
                        catch (IllegalStateException e) { }

                    }
                }
            }
        }
        catch (Exception e) {
            exception = e;
        }
        if (exception == null) {
            try {
                if (forUnionMembers) unionMemberNumberService.saveNumberWithCheck(dataUnionMembNums);
                else registrationNumberService.saveNumberWithCheck(dataRegNums);

            } catch (Exception e) {
                exception = e;
            }
        }
        // check again if exception is null
        if (exception == null) {
            response.setResponseText(getActualResponseText(exception, fileName, true));
            response.setSuccessful(true);
        }
        else {
            response.setResponseText(getActualResponseText(exception, fileName, false));
            response.setSuccessful(false);
        }

        return response;
    }

    private String getActualResponseText(Exception e, String fileName, boolean success) {
        if (success) return "A(z) " + fileName + " nevű fájl adatai sikeresen feltöltésre kerültek.";
        else {
            if (e.getMessage() != null) return "A(z) " + fileName + " nevű fájl adatainak feltöltése sikertelen a következő hiba miatt: "
                    + e.getMessage();
            else return "A(z) " + fileName + " nevű fájl adatainak feltöltése sikertelen a következő hiba miatt: "
                    + e;
        }
    }

    /* switch (cell.getCellTypeEnum()) {
                    case STRING:
                        data.get(j).add(cell.getRichStringCellValue().getString());
                    break;
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            data.get(j).add(cell.getDateCellValue() + "");
                        } else {
                            data.get(j).add(cell.getNumericCellValue() + "");
                        };
                    break;
                    case BOOLEAN:
                        data.get(j).add(cell.getBooleanCellValue() + "");
                        break;
                    case FORMULA:
                        data.get(j).add(cell.getCellFormula() + "");
                        break;
                    default: data.get(j).add(" ");

                } */

    public void downloadExcelTest(HttpServletResponse response) throws Exception {
        // First, we will create and style a header row that contains “Name” and “Age” cells:
        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Persons");
        sheet.setColumnWidth(0, 6000);
        sheet.setColumnWidth(1, 4000);

        Row header = sheet.createRow(0);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        headerStyle.setFont(font);

        Cell headerCell = header.createCell(0);
        headerCell.setCellValue("Name");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(1);
        headerCell.setCellValue("Age");
        headerCell.setCellStyle(headerStyle);

        // Next, let's write the content of the table with a different style:
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);

        Row row = sheet.createRow(2);
        Cell cell = row.createCell(0);
        cell.setCellValue("John Smith");
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue(20);
        cell.setCellStyle(style);

        // Finally, let's write the content to a “temp.xlsx” file in the current directory and close the workbook:
        /* File currDir = new File(".");
        String path = currDir.getAbsolutePath();
        String fileLocation = path.substring(0, path.length() - 1) + "temp.xlsx";

        FileOutputStream outputStream = new FileOutputStream(fileLocation);
        workbook.write(outputStream);
        workbook.close(); */

        ServletOutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        workbook.close();
    }
}
