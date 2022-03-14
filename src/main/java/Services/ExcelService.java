package Services;

import DTOs.ExcelDTO;
import DTOs.QuestionnaireDTO;
import DTOs.UserResponseDTO;
import Models.ExcelUploadStatics;
import Models.RegistrationNumber;
import Models.UnionMembershipNumber;
import Models.User;
import Repositories.ExcelUploadStaticsRepository;
import Repositories.RegNumberRepository;
import Repositories.UnionMembershipNumRepository;
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
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExcelService implements IExcelService {

    @Autowired
    public IRegistrationNumberService registrationNumberService;
    @Autowired
    public IUnionMembershipNumService unionMemberNumberService;
    @Autowired
    public ITokenService tokenService;
    @Autowired
    public RegNumberRepository regNumberRepository;
    @Autowired
    public UnionMembershipNumRepository unionMembershipNumRepo;
    @Autowired
    public ExcelUploadStaticsRepository excelUploadStaticsRepository;


    public ExcelDTO readFromUploadedExcel(ExcelDTO request) {
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
            String[] fileNamePartsArray = file.getOriginalFilename().split("\\.");
            String fileType = "";

            if (fileNamePartsArray != null && fileNamePartsArray.length > 0) fileType = fileNamePartsArray[fileNamePartsArray.length - 1];

            String fileName = "uploadedExcel.";
            if (currentUser.getRole().equals(Enums.Role.ADMIN)) fileName = "uploadedAdminExcel." + fileType;
            else if (currentUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN)) fileName = "uploadedUnionAdminExcel." + fileType;

            InputStream in = file.getInputStream();
            File currDir = new File(".");
            String path = currDir.getAbsolutePath();
            String fileLocation = path.substring(0, path.length() - 1) + fileName;
            FileOutputStream f = new FileOutputStream(fileLocation);
            int ch = 0;
            while ((ch = in.read()) != -1) {
                f.write(ch);
            }
            f.flush();
            f.close();

            boolean isForUnionMembers = false;
            if (currentUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN)) isForUnionMembers = true;

            response = readFromExcel(response, fileLocation, isForUnionMembers, file.getOriginalFilename());
        }
        catch (Exception e) {
            response.setResponseText(getActualResponseText(e, file.getOriginalFilename(), false));
        }
        return response;
    }

    private ExcelDTO readFromExcel(ExcelDTO response, String filePath, boolean forUnionMembers, String originalFileName) {

        Exception exception = null;
        Map<Integer, List<UnionMembershipNumber>> dataUnionMembNums = new HashMap<>();
        Map<String, List<String>> dataRegNums = new HashMap<>();

        try {
            FileInputStream file = new FileInputStream(filePath);
            Workbook workbook = new XSSFWorkbook(file);

            int numberOfSheets = workbook.getNumberOfSheets();

            for (int i = 0; i < numberOfSheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                if (forUnionMembers) dataUnionMembNums.put(i, new ArrayList<>());
                dataRegNums.put(sheetName, new ArrayList<>());

                for (Row row : sheet) {
                    int cellCounter = -1;
                    UnionMembershipNumber unionMembershipNumber = new UnionMembershipNumber();
                    for (Cell cell : row) {
                        cellCounter++;

                        // String Cell values
                        try {
                            String cellValue = cell.getStringCellValue();
                            // Ignore empty cells:
                            if (cellValue.isEmpty()) break;
                            else {
                                // Check can we parse the item to Long. If can, add to the value Map.
                                Long.parseLong(cellValue);
                                if (forUnionMembers) {
                                    if (cellCounter == 0) {
                                        unionMembershipNumber.setRegistrationNumber(cellValue);
                                        dataRegNums.get(sheetName).add(cellValue);
                                        continue;
                                    }
                                    else if (cellCounter == 1) {
                                        unionMembershipNumber.setUnionMembershipNum(cellValue);
                                        dataUnionMembNums.get(i).add(unionMembershipNumber);
                                        break;
                                    }
                                }
                                else {
                                    dataRegNums.get(sheetName).add(cellValue);
                                    break;
                                }
                            }
                        }
                        catch (Exception e) {
                            if (!e.getClass().equals(IllegalStateException.class)
                                    && !e.getClass().equals(NumberFormatException.class)) {
                                throw e;
                            }
                        }

                        // Numeric Cell values
                        try {
                            Long numericValue = (long) cell.getNumericCellValue();
                            if (forUnionMembers) {
                                if (cellCounter == 0) {
                                    unionMembershipNumber.setRegistrationNumber(numericValue.toString());
                                    dataRegNums.get(sheetName).add(numericValue.toString());
                                    continue;
                                }
                                else if (cellCounter == 1) {
                                    unionMembershipNumber.setUnionMembershipNum(numericValue.toString());
                                    dataUnionMembNums.get(i).add(unionMembershipNumber);
                                    break;
                                }
                            }
                            else {
                                dataRegNums.get(sheetName).add(numericValue.toString());
                                break;
                            }
                        }
                        catch (IllegalStateException e) { }

                        // Cell formula values
                        try {
                            if (forUnionMembers) {
                                if (cellCounter == 0) {
                                    // try to parse it to number
                                    Long.parseLong(cell.getCellFormula() + "");

                                    unionMembershipNumber.setRegistrationNumber(cell.getCellFormula() + "");
                                    dataRegNums.get(sheetName).add(cell.getCellFormula() + "");
                                    continue;
                                }
                                else if (cellCounter == 1) {
                                    // try to parse it to number
                                    Long.parseLong(cell.getCellFormula() + "");

                                    unionMembershipNumber.setUnionMembershipNum(cell.getCellFormula() + "");
                                    dataUnionMembNums.get(i).add(unionMembershipNumber);
                                    break;
                                }
                            }
                            else {
                                // try to parse it to number
                                Long.parseLong(cell.getCellFormula() + "");

                                dataRegNums.get(sheetName).add(cell.getCellFormula() + "");
                                break;
                            }
                        }
                        catch (Exception e) {
                            if (!e.getClass().equals(IllegalStateException.class)
                                    && !e.getClass().equals(NumberFormatException.class)) {
                                throw e;
                            }
                        }
                        break;
                    }
                }
            }
        }
        catch (Exception e) {
            exception = e;
        }
        ExcelUploadStatics statics = null;

        if (exception == null) {
            try {
                if (forUnionMembers) {
                    statics = unionMemberNumberService.saveNumberWithCheck(dataUnionMembNums, originalFileName);
                    if (!dataRegNums.isEmpty()) registrationNumberService.saveNumberWithCheck(dataRegNums, originalFileName, false);
                }
                else {
                    statics = registrationNumberService.saveNumberWithCheck(dataRegNums, originalFileName, true);
                }

            } catch (Exception e) {
                exception = e;
            }
        }
        else {
            response.setResponseText(getActualResponseText(exception, originalFileName, false));
            response.setSuccessful(false);
            return response;
        }
        // check again if exception is null
        if (exception == null) {
            response.setResponseText(getActualResponseText(exception, originalFileName, true));
            response.setStatics(statics);
            response.setSuccessful(true);
        }
        else {
            response.setResponseText(getActualResponseText(exception, originalFileName, false));
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

    public ExcelDTO getExcelStatics(ExcelDTO excelDTO) {
        ExcelDTO response = new ExcelDTO();

        // 1. Check token is valid
        UserResponseDTO userValidateResponse = tokenService.checkToken(excelDTO.getTokenUUID());
        response.setAuthSuccess(userValidateResponse.isAuthSuccess());
        response.setSuccessful(userValidateResponse.isSuccessful());
        response.setResponseText(userValidateResponse.getResponseText());

        // If not valid, return the response!
        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        User currentUser = userValidateResponse.getUser();

        // 2. Check the current user of the page is equals to user of token
        if (!currentUser.getId().equals(excelDTO.getUser().getId())) {
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

        // ---------------------------------------------------------------------------------------------------------
        ExcelUploadStatics statics = null;
        Integer numberOfActiveUsers = 0;
        Integer numberOfRecords = 0;
        Integer numberOfInactiveUsers;

        try {
            if (currentUser.getRole().equals(Enums.Role.ADMIN)) {
                statics = excelUploadStaticsRepository.findFirstByTypeOfUpload(Enums.ExcelUploadType.REGISTRATION_NUMBER);
                numberOfActiveUsers = regNumberRepository.getNumberOfUsers();
                numberOfRecords = regNumberRepository.getNumberOfRecords();
            }
            else if (currentUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN)) {
                statics = excelUploadStaticsRepository.findFirstByTypeOfUpload(Enums.ExcelUploadType.UNION_MEMBERSHIP_NUMBER);
                numberOfActiveUsers = unionMembershipNumRepo.getNumberOfUnionMemberUsers();
                numberOfRecords = unionMembershipNumRepo.getNumberOfRecords();
            }
            numberOfInactiveUsers = numberOfRecords - numberOfActiveUsers;

            if (statics == null) statics = new ExcelUploadStatics();
            statics.setNumberOfActiveElements(numberOfActiveUsers);
            statics.setNumberOfInactiveElements(numberOfInactiveUsers);

            response.setStatics(statics);
        }
        catch (Exception e) {
            if (e.getMessage() != null)
                response.setResponseText("A feltöltött excel adatok lekérdezése sikertelen a következő hiba miatt: " + e.getMessage());
            else response.setResponseText("A feltöltött excel adatok lekérdezése sikertelen a következő hiba miatt: " + e);
            response.setSuccessful(false);
        }

        return response;
    }

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
}
