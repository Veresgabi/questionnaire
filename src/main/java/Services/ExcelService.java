package Services;

import DTOs.ExcelDTO;
import DTOs.QuestionnaireDTO;
import DTOs.UserResponseDTO;
import Models.*;
import Repositories.AnswerRepository;
import Repositories.ExcelUploadStaticsRepository;
import Repositories.RegNumberRepository;
import Repositories.UnionMembershipNumRepository;
import Utils.Enums;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

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
    public RegNumberRepository regNumRepo;
    @Autowired
    public ExcelUploadStaticsRepository excelUploadStaticsRepository;
    @Autowired
    public AnswerRepository answerRepository;
    @Autowired
    public IQuestionnaireService questionnaireService ;


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

    private ExcelDTO readFromExcel(ExcelDTO response, String filePath, boolean isForUnionMembers, String originalFileName) {

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
                if (isForUnionMembers) dataUnionMembNums.put(i, new ArrayList<>());
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
                                if (isForUnionMembers) {
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
                            if (isForUnionMembers) {
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
                            if (isForUnionMembers) {
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
        List<ExcelUploadStatics> staticsList = new ArrayList<>();

        if (exception == null) {
            try {
                if (isForUnionMembers) {
                    staticsList.add(unionMemberNumberService.saveNumberWithCheck(dataUnionMembNums, originalFileName));
                    if (!dataRegNums.isEmpty()) {
                        staticsList.add(registrationNumberService.saveNumberWithCheck(response.getUser(), dataRegNums,
                                originalFileName, false));
                    }
                }
                else {
                    staticsList.add(registrationNumberService.saveNumberWithCheck(response.getUser(), dataRegNums,
                            originalFileName, true));
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
            response.setStaticsList(staticsList);

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
        List<ExcelUploadStatics> staticsList = new ArrayList<>();

        try {
            List<ExcelUploadStatics> regNumberStatics = excelUploadStaticsRepository.findAllByTypeOfUpload(
                    Enums.ExcelUploadType.REGISTRATION_NUMBER);

            for (ExcelUploadStatics staticsRegNumbers : regNumberStatics) {
                if (staticsRegNumbers == null) staticsRegNumbers = new ExcelUploadStatics();

                staticsRegNumbers.setNumberOfActiveElements(regNumberRepository.getNumberOfUsers());
                staticsRegNumbers.setNumberOfInactiveElements(regNumberRepository.getNumberOfRecords()
                        - staticsRegNumbers.getNumberOfActiveElements());
                staticsList.add(staticsRegNumbers);
            }

            if (currentUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN)) {
                ExcelUploadStatics staticsUnionMembers = excelUploadStaticsRepository.findFirstByTypeOfUpload(Enums.ExcelUploadType.UNION_MEMBERSHIP_NUMBER);
                if (staticsUnionMembers == null) staticsUnionMembers = new ExcelUploadStatics();

                staticsUnionMembers.setNumberOfActiveElements(unionMembershipNumRepo.getNumberOfUnionMemberUsers());
                staticsUnionMembers.setNumberOfInactiveElements(unionMembershipNumRepo.getNumberOfRecords()
                        - staticsUnionMembers.getNumberOfActiveElements());
                staticsList.add(staticsUnionMembers);
            }
            response.setStaticsList(staticsList);
        }
        catch (Exception e) {
            if (e.getMessage() != null)
                response.setResponseText("A feltöltött excel adatok lekérdezése sikertelen a következő hiba miatt: " + e.getMessage());
            else response.setResponseText("A feltöltött excel adatok lekérdezése sikertelen a következő hiba miatt: " + e);
            response.setSuccessful(false);
        }

        return response;
    }

    public void exportResultToExcel(HttpServletResponse response, QuestionnaireDTO questionnaireDTO) throws Exception {

        // 1. Check token is valid
        UserResponseDTO userValidateResponse = tokenService.checkToken(questionnaireDTO.getTokenUUID());

        // If not valid, return the response!
        if (!userValidateResponse.isSuccessful()) response.sendError(500);
        if (!userValidateResponse.isAuthSuccess()) response.sendError(401);

        User currentUser = userValidateResponse.getUser();

        // 2. Check the current user of the page is equals to user of token
        if (!currentUser.getId().equals(questionnaireDTO.getUser().getId())) {
            response.sendError(403);
        }

        // 3. Check the role is compatible for the requested action
        if (!currentUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN)) {
            response.sendError(403);
        }

        if (questionnaireService.getGlobalQuestionnaireDTO() != null
                && questionnaireService.getGlobalQuestionnaireDTO().getUser().getId().equals(currentUser.getId())) {
            questionnaireDTO = questionnaireService.getGlobalQuestionnaireDTO();

            Questionnaire questionnaire = questionnaireDTO.getQuestionnaire();

            String numberOfUsers = "";
            String numberOfUnionMembers = "";

            if (questionnaire.getQuestionnaireType().equals(Enums.QuestionnaireType.SIMPLE)) {
                numberOfUsers = regNumRepo.getNumberOfUsers().toString();
            }
            else if (questionnaire.getQuestionnaireType().equals(Enums.QuestionnaireType.MIXED)) {
                numberOfUsers = regNumRepo.getNumberOfUsers().toString();
                numberOfUnionMembers = unionMembershipNumRepo.getNumberOfUnionMemberUsers().toString();
            }
            else if (questionnaire.getQuestionnaireType().equals(Enums.QuestionnaireType.UNION_MEMBERS_ONLY)) {
                numberOfUnionMembers = unionMembershipNumRepo.getNumberOfUnionMemberUsers().toString();
            }

            XSSFWorkbook workbook = new XSSFWorkbook();

            Sheet sheet = createExportQuestionnaireTitleRow(workbook, questionnaire);

            for (int i = 0; i < questionnaire.getTextualQuestions().size() + questionnaire.getScaleQuestions().size()
                    + questionnaire.getChoiceQuestions().size(); i++) {

                final Integer index = i;
                TextualQuestion textualQuestion = questionnaire.getTextualQuestions()
                        .stream()
                        .filter(question -> question.getNumber().equals(index + 1))
                        .findFirst()
                        .orElse(null);

                XSSFCellStyle questionBodyStyle = workbook.createCellStyle();
                questionBodyStyle.setAlignment(HorizontalAlignment.LEFT);
                questionBodyStyle.setWrapText(true);
                questionBodyStyle.setVerticalAlignment(VerticalAlignment.TOP);

                if (textualQuestion != null) {
                    createExportTextualQuestionResult(sheet, workbook, questionBodyStyle, questionnaireDTO, textualQuestion);
                }
                else {
                    ScaleQuestion scaleQuestion = questionnaire.getScaleQuestions()
                            .stream()
                            .filter(question -> question.getNumber().equals(index + 1))
                            .findFirst()
                            .orElse(null);

                    if (scaleQuestion != null) {
                        createExportScaleQuestionResult(sheet, workbook, questionBodyStyle, questionnaireDTO, scaleQuestion,
                                numberOfUsers, numberOfUnionMembers);
                    }
                    else {
                        ChoiceQuestion choiceQuestion = questionnaire.getChoiceQuestions()
                                .stream()
                                .filter(question -> question.getNumber().equals(index + 1))
                                .findFirst()
                                .orElse(null);

                        createExportChoiceQuestionResult(sheet, workbook, questionBodyStyle, questionnaireDTO, choiceQuestion,
                                numberOfUsers, numberOfUnionMembers);

                    }
                }
            }
            ServletOutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
            workbook.close();
            response.setStatus(200);
            // questionnaireService.setGlobalQuestionnaireDTO(null);
        }
        else response.sendError(404);

    }

    public void exportAnswersToExcel(HttpServletResponse response, QuestionnaireDTO questionnaireDTO) throws Exception {

        // 1. Check token is valid
        UserResponseDTO userValidateResponse = tokenService.checkToken(questionnaireDTO.getTokenUUID());

        // If not valid, return the response!
        if (!userValidateResponse.isSuccessful()) response.sendError(500);
        if (!userValidateResponse.isAuthSuccess()) response.sendError(401);

        User currentUser = userValidateResponse.getUser();

        // 2. Check the current user of the page is equals to user of token
        if (!currentUser.getId().equals(questionnaireDTO.getUser().getId())) {
            response.sendError(403);
        }

        // 3. Check the role is compatible for the requested action
        if (!currentUser.getRole().equals(Enums.Role.USER) && !currentUser.getRole().equals(Enums.Role.UNION_MEMBER_USER)) {
            response.sendError(403);
        }

        if (questionnaireService.getGlobalQuestionnaireDTO() != null
                && questionnaireService.getGlobalQuestionnaireDTO().getUser().getId().equals(currentUser.getId())) {
            questionnaireDTO = questionnaireService.getGlobalQuestionnaireDTO();

            Questionnaire questionnaire = questionnaireDTO.getQuestionnaire();
            XSSFWorkbook workbook = new XSSFWorkbook();

            Sheet sheet = createExportQuestionnaireTitleRow(workbook, questionnaire);

            for (int i = 0; i < questionnaire.getTextualQuestions().size() + questionnaire.getScaleQuestions().size()
                    + questionnaire.getChoiceQuestions().size(); i++) {

                final Integer index = i;
                TextualQuestion textualQuestion = questionnaire.getTextualQuestions()
                        .stream()
                        .filter(question -> question.getNumber().equals(index + 1))
                        .findFirst()
                        .orElse(null);

                XSSFFont questionBodyFont = createExportFont(workbook, true,
                        new XSSFColor(new java.awt.Color(47, 117, 181)));

                XSSFCellStyle questionBodyStyle = workbook.createCellStyle();
                questionBodyStyle.setFont(questionBodyFont);
                questionBodyStyle.setAlignment(HorizontalAlignment.LEFT);
                questionBodyStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(242, 242, 242)));
                questionBodyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                questionBodyStyle.setWrapText(true);
                questionBodyStyle.setVerticalAlignment(VerticalAlignment.TOP);

                if (textualQuestion != null) {
                    createExportTextualQuestion(sheet, workbook, questionBodyStyle, questionnaireDTO, textualQuestion);
                }
                else {
                    XSSFFont selectedFont = createExportFont(workbook, true,
                            new XSSFColor(new java.awt.Color(255,255,255)));

                    XSSFCellStyle markStyle = workbook.createCellStyle();
                    markStyle.cloneStyleFrom(questionBodyStyle);
                    markStyle.setAlignment(HorizontalAlignment.CENTER);

                    XSSFCellStyle selectedMarkStyle = workbook.createCellStyle();
                    selectedMarkStyle.setFont(selectedFont);
                    selectedMarkStyle.setAlignment(HorizontalAlignment.CENTER);
                    selectedMarkStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(146, 208, 80)));
                    selectedMarkStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                    XSSFFont inactiveFont = createExportFont(workbook, true,
                            new XSSFColor(new java.awt.Color(128,128,128)));

                    XSSFCellStyle inactiveMarkStyle = workbook.createCellStyle();
                    inactiveMarkStyle.cloneStyleFrom(questionBodyStyle);
                    inactiveMarkStyle.setAlignment(HorizontalAlignment.CENTER);
                    inactiveMarkStyle.setFont(inactiveFont);

                    ScaleQuestion scaleQuestion = questionnaire.getScaleQuestions()
                            .stream()
                            .filter(question -> question.getNumber().equals(index + 1))
                            .findFirst()
                            .orElse(null);

                    if (scaleQuestion != null) {
                        createExportScaleQuestion(sheet, workbook, questionBodyStyle, markStyle, selectedMarkStyle,
                                inactiveMarkStyle, questionnaireDTO, scaleQuestion);
                    }
                    else {
                        ChoiceQuestion choiceQuestion = questionnaire.getChoiceQuestions()
                                .stream()
                                .filter(question -> question.getNumber().equals(index + 1))
                                .findFirst()
                                .orElse(null);

                        createExportChoiceQuestion(sheet, workbook, questionBodyStyle, markStyle, selectedMarkStyle,
                                inactiveMarkStyle, questionnaireDTO, choiceQuestion);
                    }
                }
            }
            ServletOutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
            workbook.close();
            response.setStatus(200);
            questionnaireService.setGlobalQuestionnaireDTO(null);
        }
        else response.sendError(404);
    }

    private Sheet createExportQuestionnaireTitleRow(XSSFWorkbook workbook, Questionnaire questionnaire) {
        Sheet sheet = workbook.createSheet("Kérdőív");
        sheet.setColumnWidth(0, 6000);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 15));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 16, 21));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 21));

        Row titleRow1 = sheet.createRow(0);

        XSSFFont titleFont = createExportFont(workbook, true, new XSSFColor(new java.awt.Color(47, 117, 181)));
        titleFont.setFontHeightInPoints((short) 16);

        XSSFCellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(242, 242, 242)));
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Cell titleCell1 = titleRow1.createCell(0, CellType.STRING);
        titleCell1.setCellStyle(titleStyle);
        titleCell1 = titleRow1.createCell(1, CellType.STRING);
        titleCell1.setCellValue(questionnaire.getTitle());
        titleCell1.setCellStyle(titleStyle);
        titleCell1 = titleRow1.createCell(16, CellType.STRING);
        titleCell1.setCellStyle(titleStyle);

        Row titleRow2 = sheet.createRow(1);
        Cell titleCell2 = titleRow2.createCell(0, CellType.STRING);
        titleCell2.setCellStyle(titleStyle);

        return sheet;
    }

    private void createExportTextualQuestion(Sheet sheet, XSSFWorkbook workbook, XSSFCellStyle questionBodyStyle,
                                             QuestionnaireDTO questionnaireDTO, TextualQuestion textualQuestion) {
        createExportQuestionHeader(sheet, workbook, textualQuestion);

        String answerText = questionnaireDTO.getAnswers()
                .stream()
                .filter(a -> a.getTextualQuestionId() != null
                        && a.getTextualQuestionId().equals(textualQuestion.getId()))
                .map(a -> a.getContent())
                .findFirst()
                .orElse(null);

        if (!textualQuestion.isCompletedByCurrentUser() && textualQuestion.getType() != null && answerText != null) {
            sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                    sheet.getLastRowNum() + 1, 1, 15));
            sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 2,
                    sheet.getLastRowNum() + 2, 1, 15));
            sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                    sheet.getLastRowNum() + 5, 16, 21));
            sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 3,
                    sheet.getLastRowNum() + 5, 1, 15));
            sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 6,
                    sheet.getLastRowNum() + 6, 0, 21));

            Row answerFirstRow = sheet.createRow(sheet.getLastRowNum() + 1);
            Row answerSecondRow = sheet.createRow(sheet.getLastRowNum() + 1);

            Cell answerHeaderCell = answerSecondRow.createCell(1, CellType.STRING);

            Cell answerEmptyCell1 = answerFirstRow.createCell(0, CellType.STRING);
            answerEmptyCell1.setCellStyle(questionBodyStyle);
            answerEmptyCell1 = answerFirstRow.createCell(1, CellType.STRING);
            answerEmptyCell1.setCellStyle(questionBodyStyle);
            answerEmptyCell1 = answerFirstRow.createCell(16, CellType.STRING);
            answerEmptyCell1.setCellStyle(questionBodyStyle);

            answerHeaderCell.setCellStyle(questionBodyStyle);
            // TODO: itt is kell majd kezelni a "már megválaszolva"-t és a "Csak szakszervezeti tagoknak" és az optional
            //  miatti ürességeket!
            answerHeaderCell.setCellValue("Válasz:");

            Cell answerEmptyCell2 = answerSecondRow.createCell(0, CellType.STRING);
            answerEmptyCell2.setCellStyle(questionBodyStyle);

            Row answerThirdRow = sheet.createRow(sheet.getLastRowNum() + 1);

            XSSFFont answerTextFont = createExportFont(workbook, false,
                    new XSSFColor(new java.awt.Color(47, 117, 181)));

            XSSFCellStyle answerTextStyle = workbook.createCellStyle();
            answerTextStyle.cloneStyleFrom(questionBodyStyle);
            answerTextStyle.setFont(answerTextFont);

            Cell answerCell = answerThirdRow.createCell(1, CellType.STRING);
            answerCell.setCellStyle(answerTextStyle);

            answerCell.setCellValue(answerText);

            Cell answerEmptyCell3 = answerThirdRow.createCell(0, CellType.STRING);
            answerEmptyCell3.setCellStyle(questionBodyStyle);

            Row answerFourthRow = sheet.createRow(sheet.getLastRowNum() + 1);
            Cell answerEmptyCell4 = answerFourthRow.createCell(0, CellType.STRING);
            answerEmptyCell4.setCellStyle(answerTextStyle);

            Row answerFifthRow = sheet.createRow(sheet.getLastRowNum() + 1);
            Cell answerEmptyCell5 = answerFifthRow.createCell(0, CellType.STRING);
            answerEmptyCell5.setCellStyle(answerTextStyle);

            Row answerSixthRow = sheet.createRow(sheet.getLastRowNum() + 1);
            Cell answerEmptyCell6 = answerSixthRow.createCell(0, CellType.STRING);
            answerEmptyCell6.setCellStyle(answerTextStyle);
        }
        else {
            createExportEmptyQuestionField(sheet, questionBodyStyle);
        }
    }

    private void createExportTextualQuestionResult(Sheet sheet, XSSFWorkbook workbook, XSSFCellStyle questionBodyStyle,
                                             QuestionnaireDTO questionnaireDTO, TextualQuestion textualQuestion) {
        createExportQuestionHeader(sheet, workbook, textualQuestion);

        if (questionnaireService.getLimit() < textualQuestion.getNumberOfAnswers()) {
            List<Answer> answers = answerRepository.findByTextualQuestionId(textualQuestion.getId());
            textualQuestion.setAnswers(answers);
        }

        List<String> answerTexts = textualQuestion.getAnswers()
                .stream()
                .map(a -> a.getContent())
                .collect(Collectors.toList());

        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                sheet.getLastRowNum() + 1, 1, 15));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                sheet.getLastRowNum() + 1, 16, 21));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 2,
                sheet.getLastRowNum() + 2, 1, 15));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 2,
                sheet.getLastRowNum() + 2, 16, 21));

        Row answerFirstRow = sheet.createRow(sheet.getLastRowNum() + 1);
        Row answerSecondRow = sheet.createRow(sheet.getLastRowNum() + 1);

        XSSFFont answerNumberFont = createExportFont(workbook, false,
                new XSSFColor(new java.awt.Color(0, 0, 0)));
        questionBodyStyle.setFont(answerNumberFont);

        Cell answerHeaderCell = answerSecondRow.createCell(1, CellType.STRING);
        answerHeaderCell.setCellStyle(questionBodyStyle);
        answerHeaderCell.setCellValue("Válaszok:");

        int i = 1;
        for (String answerText : answerTexts) {

            sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                    sheet.getLastRowNum() + 3, 1, 1));
            sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                    sheet.getLastRowNum() + 3, 2, 15));
            sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                    sheet.getLastRowNum() + 3, 16, 21));

            Row answerRow1 = sheet.createRow(sheet.getLastRowNum() + 1);

            XSSFCellStyle answerNumberStyle = workbook.createCellStyle();
            answerNumberStyle.cloneStyleFrom(questionBodyStyle);
            answerNumberStyle.setAlignment(HorizontalAlignment.CENTER);
            answerNumberStyle.setVerticalAlignment(VerticalAlignment.TOP);

            Cell numberCell = answerRow1.createCell(1, CellType.STRING);
            numberCell.setCellStyle(answerNumberStyle);
            numberCell.setCellValue(i + ".");

            XSSFFont answerTextFont = createExportFont(workbook, false,
                    new XSSFColor(new java.awt.Color(47, 117, 181)));

            XSSFCellStyle answerTextStyle = workbook.createCellStyle();
            answerTextStyle.cloneStyleFrom(questionBodyStyle);
            answerTextStyle.setFont(answerTextFont);
            answerTextStyle.setAlignment(HorizontalAlignment.LEFT);
            answerTextStyle.setVerticalAlignment(VerticalAlignment.TOP);

            Cell answerCell = answerRow1.createCell(2, CellType.STRING);
            answerCell.setCellStyle(answerTextStyle);
            answerCell.setCellValue(answerText);

            Row answerRow2 = sheet.createRow(sheet.getLastRowNum() + 1);
            Row answerRow3 = sheet.createRow(sheet.getLastRowNum() + 1);

            i++;
        }
        Row answerThirdRow = sheet.createRow(sheet.getLastRowNum() + 1);
    }

    private void createExportScaleQuestion(Sheet sheet, XSSFWorkbook workbook, XSSFCellStyle questionBodyStyle,
                                           XSSFCellStyle markStyle, XSSFCellStyle selectedMarkStyle,
                                           XSSFCellStyle inactiveMarkStyle, QuestionnaireDTO questionnaireDTO,
                                           ScaleQuestion scaleQuestion) {
        createExportQuestionHeader(sheet, workbook, scaleQuestion);

        if (scaleQuestion.getType() != null) {
            sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                    sheet.getLastRowNum() + 1, 1, 11));
            if (scaleQuestion.getScaleMaxNumber() > 10) {
                if (!scaleQuestion.isCompletedByCurrentUser()) {
                    sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                            sheet.getLastRowNum() + 7, 12, 21));
                }
                else {
                    sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                            sheet.getLastRowNum() + 5, 12, 21));
                }
            }
            else {
                sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                        sheet.getLastRowNum() + 3, 12, 21));
            }
            Row firstRow = sheet.createRow(sheet.getLastRowNum() + 1);
            Cell emptyCell1 = firstRow.createCell(0, CellType.STRING);
            emptyCell1.setCellStyle(questionBodyStyle);
            emptyCell1 = firstRow.createCell(1, CellType.STRING);
            emptyCell1.setCellStyle(questionBodyStyle);
            emptyCell1 = firstRow.createCell(12, CellType.STRING);
            emptyCell1.setCellStyle(questionBodyStyle);

            Row secondRow = sheet.createRow(sheet.getLastRowNum() + 1);

            Cell emptyCell2 = secondRow.createCell(0, CellType.STRING);
            emptyCell2.setCellStyle(questionBodyStyle);

            String choosenValue = questionnaireDTO.getAnswers()
                    .stream()
                    .filter(a -> a.getScaleQuestionId() != null
                            && a.getScaleQuestionId().equals(scaleQuestion.getId()))
                    .map(a -> a.getContent())
                    .findFirst()
                    .orElse(null);

            int actualScaleNumber = scaleQuestion.getScaleMinNumber();

            XSSFCellStyle scaleNumberStyle;
            if (scaleQuestion.isCompletedByCurrentUser()) scaleNumberStyle = inactiveMarkStyle;
            else scaleNumberStyle = markStyle;

            if (scaleQuestion.getScaleMaxNumber() > 10) {
                sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum(),
                        sheet.getLastRowNum(), 1, 4));
                sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 2,
                        sheet.getLastRowNum() + 2, 1, 4));

                Cell minNumberTitleCell = secondRow.createCell(1, CellType.STRING);
                minNumberTitleCell.setCellStyle(scaleNumberStyle);
                minNumberTitleCell.setCellValue("Minimálisan kiválasztható érték:");
                Cell minNumberCell = secondRow.createCell(5, CellType.STRING);
                minNumberCell.setCellStyle(scaleNumberStyle);
                minNumberCell.setCellValue(scaleQuestion.getScaleMinNumber());

                sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum(),
                        sheet.getLastRowNum(), 6, 11));
                emptyCell2 = secondRow.createCell(6, CellType.STRING);
                emptyCell2.setCellStyle(questionBodyStyle);

                sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                        sheet.getLastRowNum() + 1, 0, 11));
                Row thirdRow = sheet.createRow(sheet.getLastRowNum() + 1);
                Cell emptyCell3 = thirdRow.createCell(0, CellType.STRING);
                emptyCell3.setCellStyle(questionBodyStyle);

                sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                        sheet.getLastRowNum() + 1, 6, 11));
                Row fourthRow = sheet.createRow(sheet.getLastRowNum() + 1);
                Cell emptyCell4 = fourthRow.createCell(0, CellType.STRING);
                emptyCell4.setCellStyle(questionBodyStyle);
                emptyCell4 = fourthRow.createCell(6, CellType.STRING);
                emptyCell4.setCellStyle(questionBodyStyle);

                Cell maxNumberTitleCell = fourthRow.createCell(1, CellType.STRING);
                maxNumberTitleCell.setCellValue("Maximálisan kiválasztható érték:");
                maxNumberTitleCell.setCellStyle(scaleNumberStyle);
                Cell maxNumberCell = fourthRow.createCell(5, CellType.STRING);
                maxNumberCell.setCellStyle(scaleNumberStyle);
                maxNumberCell.setCellValue(scaleQuestion.getScaleMaxNumber());

                if (!scaleQuestion.isCompletedByCurrentUser()) {
                    sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                            sheet.getLastRowNum() + 1, 0, 11));
                    Row fifthRow = sheet.createRow(sheet.getLastRowNum() + 1);
                    Cell emptyCell5 = fifthRow.createCell(0, CellType.STRING);
                    emptyCell5.setCellStyle(questionBodyStyle);

                    Row sixthRow = sheet.createRow(sheet.getLastRowNum() + 1);
                    sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum(),
                            sheet.getLastRowNum(), 1, 4));
                    sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum(),
                            sheet.getLastRowNum(), 6, 11));
                    Cell emptyCell6 = sixthRow.createCell(0, CellType.STRING);
                    emptyCell6.setCellStyle(questionBodyStyle);
                    emptyCell6 = sixthRow.createCell(6, CellType.STRING);
                    emptyCell6.setCellStyle(questionBodyStyle);

                    XSSFCellStyle selectedStyle = workbook.createCellStyle();
                    selectedStyle.cloneStyleFrom(selectedMarkStyle);
                    selectedStyle.setAlignment(HorizontalAlignment.LEFT);
                    Cell choosenValueTitleCell = sixthRow.createCell(1, CellType.STRING);
                    choosenValueTitleCell.setCellStyle(selectedStyle);
                    choosenValueTitleCell.setCellValue("Kiválasztott érték:");

                    Cell choosenValueCell = sixthRow.createCell(5, CellType.STRING);
                    choosenValueCell.setCellStyle(selectedMarkStyle);
                    choosenValueCell.setCellValue(choosenValue);
                }
            }
            else {
                for (int j = 1; j <= 11; j++) {

                    Cell numberCell = secondRow.createCell(j, CellType.STRING);
                    numberCell.setCellStyle(scaleNumberStyle);

                    if (actualScaleNumber <= scaleQuestion.getScaleMaxNumber()) {
                        numberCell.setCellValue(actualScaleNumber);

                        if (choosenValue != null && choosenValue.equals(actualScaleNumber + "")) {
                            numberCell.setCellStyle(selectedMarkStyle);
                        }
                    }
                    actualScaleNumber++;
                }
            }
            Row lastRow = sheet.createRow(sheet.getLastRowNum() + 1);
            sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum(),
                    sheet.getLastRowNum(), 0, 11));
            Cell emptyCellLast = lastRow.createCell(0, CellType.STRING);
            emptyCellLast.setCellStyle(questionBodyStyle);
        }
        else {
            createExportEmptyQuestionField(sheet, questionBodyStyle);
        }
    }

    private void createExportScaleQuestionResult(Sheet sheet, XSSFWorkbook workbook, XSSFCellStyle questionBodyStyle,
                                                 QuestionnaireDTO questionnaireDTO, ScaleQuestion scaleQuestion,
                                                 String numberOfUsers, String numberOfUnionMembers) {
        createExportQuestionHeader(sheet, workbook, scaleQuestion);

        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 2,
                sheet.getLastRowNum() + 3, 1, 5));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 4,
                sheet.getLastRowNum() + 5, 1, 5));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 6,
                sheet.getLastRowNum() + 7, 1, 5));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 8,
                sheet.getLastRowNum() + 9, 1, 5));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 10,
                sheet.getLastRowNum() + 11, 1, 5));

        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 2,
                sheet.getLastRowNum() + 3, 6, 8));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 4,
                sheet.getLastRowNum() + 5, 6, 8));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 6,
                sheet.getLastRowNum() + 7, 6, 8));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 8,
                sheet.getLastRowNum() + 9, 6, 8));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 10,
                sheet.getLastRowNum() + 11, 6, 8));

        Row firstRow = sheet.createRow(sheet.getLastRowNum() + 1);
        Row secondRow = sheet.createRow(sheet.getLastRowNum() + 1);
        Row thirdRow = sheet.createRow(sheet.getLastRowNum() + 1);
        Row fourthRow = sheet.createRow(sheet.getLastRowNum() + 1);
        Row fifthRow = sheet.createRow(sheet.getLastRowNum() + 1);
        Row sixthRow = sheet.createRow(sheet.getLastRowNum() + 1);
        Row seventhRow = sheet.createRow(sheet.getLastRowNum() + 1);
        Row eighthRow = sheet.createRow(sheet.getLastRowNum() + 1);
        Row ninthRow = sheet.createRow(sheet.getLastRowNum() + 1);
        Row tenthRow = sheet.createRow(sheet.getLastRowNum() + 1);
        Row eleventhRow = sheet.createRow(sheet.getLastRowNum() + 1);
        Row twelfthRow = sheet.createRow(sheet.getLastRowNum() + 1);

        XSSFFont answerTextBlackFont = createExportFont(workbook, false,
                new XSSFColor(new java.awt.Color(0, 0, 0)));
        questionBodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        questionBodyStyle.setFont(answerTextBlackFont);

        XSSFFont answerTextBlueFont = createExportFont(workbook, false,
                new XSSFColor(new java.awt.Color(47, 117, 181)));

        XSSFCellStyle answerTextStyleBlueFont = workbook.createCellStyle();
        answerTextStyleBlueFont.cloneStyleFrom(questionBodyStyle);
        answerTextStyleBlueFont.setFont(answerTextBlueFont);
        answerTextStyleBlueFont.setAlignment(HorizontalAlignment.CENTER);

        XSSFCellStyle answerTextStyleBlackFont = workbook.createCellStyle();
        answerTextStyleBlackFont.cloneStyleFrom(answerTextStyleBlueFont);
        answerTextStyleBlackFont.setFont(answerTextBlackFont);

        Cell minValueTitleCell = secondRow.createCell(1, CellType.STRING);
        minValueTitleCell.setCellStyle(questionBodyStyle);
        minValueTitleCell.setCellValue("Minimális választható érték:");

        Cell minValueCell = secondRow.createCell(6, CellType.STRING);
        minValueCell.setCellStyle(answerTextStyleBlackFont);
        minValueCell.setCellValue(scaleQuestion.getScaleMinNumber());

        Cell maxValueTitleCell = fourthRow.createCell(1, CellType.STRING);
        maxValueTitleCell.setCellStyle(questionBodyStyle);
        maxValueTitleCell.setCellValue("Maximális választható érték:");

        Cell maxValueCell = fourthRow.createCell(6, CellType.STRING);
        maxValueCell.setCellStyle(answerTextStyleBlackFont);
        maxValueCell.setCellValue(scaleQuestion.getScaleMaxNumber());

        Cell averageTitleCell = sixthRow.createCell(1, CellType.STRING);
        averageTitleCell.setCellStyle(questionBodyStyle);
        averageTitleCell.setCellValue("Átlag:");

        Cell averageCell = sixthRow.createCell(6, CellType.STRING);
        averageCell.setCellStyle(answerTextStyleBlueFont);
        averageCell.setCellValue(scaleQuestion.getAverageRate());

        Cell completionTitleCell = eighthRow.createCell(1, CellType.STRING);
        completionTitleCell.setCellStyle(questionBodyStyle);
        completionTitleCell.setCellValue("Megválaszolva (alkalom):");

        Cell completionCell = eighthRow.createCell(6, CellType.STRING);
        completionCell.setCellStyle(answerTextStyleBlueFont);
        completionCell.setCellValue(scaleQuestion.getCompletion());

        Cell numberOfPersonsToAnswerTitleCell = tenthRow.createCell(1, CellType.STRING);
        numberOfPersonsToAnswerTitleCell.setCellStyle(questionBodyStyle);
        numberOfPersonsToAnswerTitleCell.setCellValue("Megválaszolásra jogosult személyek száma:");

        Cell numberOfPersonsToAnswerCell = tenthRow.createCell(6, CellType.STRING);
        numberOfPersonsToAnswerCell.setCellStyle(answerTextStyleBlueFont);
        String numberOfPersonsToAnswer;
        if (scaleQuestion.isUnionMembersOnly()) numberOfPersonsToAnswer = numberOfUnionMembers;
        else numberOfPersonsToAnswer = numberOfUsers;

        numberOfPersonsToAnswerCell.setCellValue(numberOfPersonsToAnswer);
    }

    private void createExportChoiceQuestion(Sheet sheet, XSSFWorkbook workbook, XSSFCellStyle questionBodyStyle,
                                            XSSFCellStyle markStyle, XSSFCellStyle selectedMarkStyle,
                                            XSSFCellStyle inactiveMarkStyle, QuestionnaireDTO questionnaireDTO,
                                            ChoiceQuestion choiceQuestion) {

        createExportQuestionHeader(sheet, workbook, choiceQuestion);

        Answer answer = questionnaireDTO.getAnswers()
                .stream()
                .filter(a -> a.getChoiceQuestionId() != null
                        && a.getChoiceQuestionId().equals(choiceQuestion.getId()))
                .findFirst()
                .orElse(null);

        List<String> choosenChoices = new ArrayList<>();

        if (answer != null) {
            choosenChoices = answer.getChoiceAnswers()
                    .stream()
                    .map(ca -> ca.getContent())
                    .collect(Collectors.toList());
        }

        if (choiceQuestion.getType() != null) {
            sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                    sheet.getLastRowNum() + 1, 0, 21));

            XSSFCellStyle selectedQuestionBodyStyle = workbook.createCellStyle();
            selectedQuestionBodyStyle.cloneStyleFrom(selectedMarkStyle);
            selectedQuestionBodyStyle.setAlignment(HorizontalAlignment.LEFT);

            XSSFCellStyle inactiveQuestionBodyStyle = workbook.createCellStyle();
            inactiveQuestionBodyStyle.cloneStyleFrom(inactiveMarkStyle);
            inactiveQuestionBodyStyle.setAlignment(HorizontalAlignment.LEFT);

            Row firstRow = sheet.createRow(sheet.getLastRowNum() + 1);
            Cell emptyCell1 = firstRow.createCell(0, CellType.STRING);
            emptyCell1.setCellStyle(questionBodyStyle);

            for (Choice choice : choiceQuestion.getChoices()) {
                sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                        sheet.getLastRowNum() + 1, 2, 15));
                sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                        sheet.getLastRowNum() + 1, 16, 21));
                sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 2,
                        sheet.getLastRowNum() + 2, 0, 21));

                Row row1 = sheet.createRow(sheet.getLastRowNum() + 1);

                Cell emptyCell2 = row1.createCell(0, CellType.STRING);
                emptyCell2.setCellStyle(questionBodyStyle );

                Cell markCell = row1.createCell(1, CellType.STRING);
                markCell.setCellValue(choice.getMark());

                Cell textCell = row1.createCell(2, CellType.STRING);
                textCell.setCellValue(choice.getText());

                if (!choiceQuestion.isCompletedByCurrentUser()) {
                    if (choosenChoices.contains(choice.getMark())) {
                        markCell.setCellStyle(selectedMarkStyle);
                        textCell.setCellStyle(selectedQuestionBodyStyle);
                    }
                    else {
                        markCell.setCellStyle(markStyle);
                        textCell.setCellStyle(questionBodyStyle);
                    }
                }
                else {
                    markCell.setCellStyle(inactiveMarkStyle);
                    textCell.setCellStyle(inactiveQuestionBodyStyle);
                }

                emptyCell2 = row1.createCell(16, CellType.STRING);
                emptyCell2.setCellStyle(questionBodyStyle );

                Row row2 = sheet.createRow(sheet.getLastRowNum() + 1);
                Cell emptyCell3 = row2.createCell(0, CellType.STRING);
                emptyCell3.setCellStyle(questionBodyStyle );
            }
        }
        else {
            createExportEmptyQuestionField(sheet, questionBodyStyle);
        }
    }

    private void createExportChoiceQuestionResult(Sheet sheet, XSSFWorkbook workbook, XSSFCellStyle questionBodyStyle,
                                            QuestionnaireDTO questionnaireDTO, ChoiceQuestion choiceQuestion,
                                            String numberOfUsers, String numberOfUnionMembers) {

        createExportQuestionHeader(sheet, workbook, choiceQuestion);

        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 2,
                sheet.getLastRowNum() + 2, 16, 18));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 2,
                sheet.getLastRowNum() + 2, 19, 21));

        Row firstRow = sheet.createRow(sheet.getLastRowNum() + 1);
        Row secondRow = sheet.createRow(sheet.getLastRowNum() + 1);

        XSSFFont answerTextBlackFont = createExportFont(workbook, false,
                new XSSFColor(new java.awt.Color(0, 0, 0)));
        questionBodyStyle.setFont(answerTextBlackFont);

        XSSFCellStyle answerTextStyleCenteredBlackFont = workbook.createCellStyle();
        answerTextStyleCenteredBlackFont.cloneStyleFrom(questionBodyStyle);
        answerTextStyleCenteredBlackFont.setAlignment(HorizontalAlignment.CENTER);

        XSSFFont answerTextBlueFont = createExportFont(workbook, false,
                new XSSFColor(new java.awt.Color(47, 117, 181)));

        XSSFCellStyle answerTextStyleCenteredBlueFont = workbook.createCellStyle();
        answerTextStyleCenteredBlueFont.cloneStyleFrom(questionBodyStyle);
        answerTextStyleCenteredBlueFont.setFont(answerTextBlueFont);
        answerTextStyleCenteredBlueFont.setAlignment(HorizontalAlignment.CENTER);
        answerTextStyleCenteredBlueFont.setVerticalAlignment(VerticalAlignment.CENTER);

        Cell selectionRateTitleCell = secondRow.createCell(16, CellType.STRING);
        selectionRateTitleCell.setCellStyle(answerTextStyleCenteredBlackFont);
        selectionRateTitleCell.setCellValue("Kiválasztási arány:");

        Cell selectionNumberTitleCell = secondRow.createCell(19, CellType.STRING);
        selectionNumberTitleCell.setCellStyle(answerTextStyleCenteredBlackFont);
        selectionNumberTitleCell.setCellValue("Kiválasztások száma:");

        for (Choice choice : choiceQuestion.getChoices()) {
            sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                    sheet.getLastRowNum() + 2, 1, 1));
            sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                    sheet.getLastRowNum() + 2, 2, 15));
            sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                    sheet.getLastRowNum() + 2, 16, 18));
            sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                    sheet.getLastRowNum() + 2, 19, 21));

            Row firstChoiceRow = sheet.createRow(sheet.getLastRowNum() + 1);
            Row secondChoiceRow = sheet.createRow(sheet.getLastRowNum() + 1);

            Cell markCell = firstChoiceRow.createCell(1, CellType.STRING);
            markCell.setCellStyle(answerTextStyleCenteredBlackFont);
            markCell.setCellValue(choice.getMark());

            Cell choiceCell = firstChoiceRow.createCell(2, CellType.STRING);
            choiceCell.setCellStyle(questionBodyStyle);
            choiceCell.setCellValue(choice.getText());

            Cell selectionRateCell = firstChoiceRow.createCell(16, CellType.STRING);
            selectionRateCell.setCellStyle(answerTextStyleCenteredBlueFont);
            selectionRateCell.setCellValue(choice.getPercentOfSelection());

            Cell selectionNumberCell = firstChoiceRow.createCell(19, CellType.STRING);
            selectionNumberCell.setCellStyle(answerTextStyleCenteredBlueFont);
            selectionNumberCell.setCellValue(choice.getNumberOfSelection());
        }

        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                sheet.getLastRowNum() + 2, 11, 15));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                sheet.getLastRowNum() + 2, 16, 18));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 3,
                sheet.getLastRowNum() + 4, 11, 15));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 3,
                sheet.getLastRowNum() + 4, 16, 18));

        Row lastSectionRow1 = sheet.createRow(sheet.getLastRowNum() + 1);
        Row lastSectionRow2 = sheet.createRow(sheet.getLastRowNum() + 1);
        Row lastSectionRow3 = sheet.createRow(sheet.getLastRowNum() + 1);
        Row lastSectionRow4 = sheet.createRow(sheet.getLastRowNum() + 1);
        Row lastSectionRow5 = sheet.createRow(sheet.getLastRowNum() + 1);

        XSSFCellStyle answerTextStyleVerticalCenteredBlackFont = workbook.createCellStyle();
        answerTextStyleVerticalCenteredBlackFont.cloneStyleFrom(questionBodyStyle);
        answerTextStyleVerticalCenteredBlackFont.setVerticalAlignment(VerticalAlignment.CENTER);

        Cell numberOfAnswersTitleCell = lastSectionRow1.createCell(11, CellType.STRING);
        numberOfAnswersTitleCell.setCellStyle(answerTextStyleVerticalCenteredBlackFont);
        numberOfAnswersTitleCell.setCellValue("Megválaszolva (alkalom):");
        Cell numberOfAnswersCell = lastSectionRow1.createCell(16, CellType.STRING);
        numberOfAnswersCell.setCellStyle(answerTextStyleCenteredBlueFont);
        numberOfAnswersCell.setCellValue(choiceQuestion.getCompletion());

        Cell numberOfPersonsToAnswerTitleCell = lastSectionRow3.createCell(11, CellType.STRING);
        numberOfPersonsToAnswerTitleCell.setCellStyle(answerTextStyleVerticalCenteredBlackFont);
        numberOfPersonsToAnswerTitleCell.setCellValue("Megválaszolásra jogosult személyek száma:");
        Cell numberOfPersonsToAnswerCell = lastSectionRow3.createCell(16, CellType.STRING);
        numberOfPersonsToAnswerCell.setCellStyle(answerTextStyleCenteredBlueFont);

        String numberOfPersonsToAnswer;
        if (choiceQuestion.isUnionMembersOnly()) numberOfPersonsToAnswer = numberOfUnionMembers;
        else numberOfPersonsToAnswer = numberOfUsers;

        numberOfPersonsToAnswerCell.setCellValue(numberOfPersonsToAnswer);
    }

    private void createExportQuestionHeader(Sheet sheet, XSSFWorkbook workbook, Object question) {
        Integer number = 0;
        String questionText = "";
        boolean isUnionMembersOnly = false;
        boolean isOptional = false;
        Enums.Type type = null;
        boolean isCompletedByCurrentUser = false;
        boolean isMultipleChoiceEnabled = false;
        if (question instanceof TextualQuestion) {
            number = ((TextualQuestion) question).getNumber();
            questionText = ((TextualQuestion) question).getQuestion();
            isUnionMembersOnly = ((TextualQuestion) question).isUnionMembersOnly();
            isOptional = ((TextualQuestion) question).getOptional();
            type = ((TextualQuestion) question).getType();
            isCompletedByCurrentUser = ((TextualQuestion) question).isCompletedByCurrentUser();
        }
        else if (question instanceof ScaleQuestion) {
            number = ((ScaleQuestion) question).getNumber();
            questionText = ((ScaleQuestion) question).getQuestion();
            isUnionMembersOnly = ((ScaleQuestion) question).isUnionMembersOnly();
            type = ((ScaleQuestion) question).getType();
            isCompletedByCurrentUser = ((ScaleQuestion) question).isCompletedByCurrentUser();
        }
        else if (question instanceof ChoiceQuestion) {
            number = ((ChoiceQuestion) question).getNumber();
            questionText = ((ChoiceQuestion) question).getQuestion();
            isUnionMembersOnly = ((ChoiceQuestion) question).isUnionMembersOnly();
            type = ((ChoiceQuestion) question).getType();
            isCompletedByCurrentUser = ((ChoiceQuestion) question).isCompletedByCurrentUser();
            isMultipleChoiceEnabled = ((ChoiceQuestion) question).isMultipleChoiceEnabled();
        }

        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1, sheet.getLastRowNum() + 3, 1, 15));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1, sheet.getLastRowNum() + 1, 17, 21));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 2, sheet.getLastRowNum() + 3, 17, 21));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1, sheet.getLastRowNum() + 3, 16, 16));

        Row questionHeaderRow1 = sheet.createRow(sheet.getLastRowNum() + 1);

        XSSFFont questionHeaderFont = createExportFont(workbook, true,
                new XSSFColor(new java.awt.Color(255, 255, 255)));
        questionHeaderFont.setBold(true);

        XSSFCellStyle questionHeaderStyle = workbook.createCellStyle();
        questionHeaderStyle.setFont(questionHeaderFont);
        questionHeaderStyle.setAlignment(HorizontalAlignment.LEFT);
        questionHeaderStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(47, 117, 181)));
        questionHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        questionHeaderStyle.setWrapText(true);
        questionHeaderStyle.setVerticalAlignment(VerticalAlignment.TOP);

        XSSFFont questionHeaderNonActiveFont = createExportFont(workbook, true,
                new XSSFColor(new java.awt.Color(191, 191, 191)));

        XSSFCellStyle questionHeaderNonActiveStyle = workbook.createCellStyle();
        questionHeaderNonActiveStyle.cloneStyleFrom(questionHeaderStyle);
        questionHeaderNonActiveStyle.setFont(questionHeaderNonActiveFont);

        Cell questionNumbTextCell = questionHeaderRow1.createCell(0, CellType.STRING);
        if (type == null || isCompletedByCurrentUser) questionNumbTextCell.setCellStyle(questionHeaderNonActiveStyle);
        else questionNumbTextCell.setCellStyle(questionHeaderStyle);
        questionNumbTextCell.setCellValue(number + ". kérdés");

        questionNumbTextCell = questionHeaderRow1.createCell(1, CellType.STRING);
        if (type == null || isCompletedByCurrentUser) questionNumbTextCell.setCellStyle(questionHeaderNonActiveStyle);
        else questionNumbTextCell.setCellStyle(questionHeaderStyle);
        questionNumbTextCell.setCellValue(questionText);

        XSSFFont unionMembersFont = createExportFont(workbook, true,
                new XSSFColor(new java.awt.Color(255, 255, 0)));

        XSSFCellStyle unionMembersStyle = workbook.createCellStyle();
        unionMembersStyle.cloneStyleFrom(questionHeaderStyle);
        unionMembersStyle.setFont(unionMembersFont);

        questionNumbTextCell = questionHeaderRow1.createCell(17, CellType.STRING);

        if (isUnionMembersOnly && type != null && !isCompletedByCurrentUser) {
            questionNumbTextCell.setCellValue("Csak szakszervezeti tagok részére");
            questionNumbTextCell.setCellStyle(unionMembersStyle);
        }
        else if (isCompletedByCurrentUser) {
            questionNumbTextCell.setCellValue("Korábban megválaszolva");
            questionNumbTextCell.setCellStyle(questionHeaderNonActiveStyle);
        }
        else {
            questionNumbTextCell.setCellStyle(unionMembersStyle);
        }

        Row questionHeaderRow2 = sheet.createRow(sheet.getLastRowNum() + 1);

        XSSFFont questionOptionalFont = createExportFont(workbook, false,
                new XSSFColor(new java.awt.Color(255, 255, 0)));

        XSSFCellStyle questionOptionalStyle = workbook.createCellStyle();
        questionOptionalStyle.cloneStyleFrom(unionMembersStyle);
        questionOptionalStyle.setFont(questionOptionalFont);
        questionOptionalStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        Cell optionalCell = questionHeaderRow2.createCell(0, CellType.STRING);
        optionalCell.setCellStyle(questionOptionalStyle);

        Cell multipleChoiceCell = questionHeaderRow2.createCell(17, CellType.STRING);
        multipleChoiceCell.setCellStyle(questionOptionalStyle);
        if (isMultipleChoiceEnabled) {
            multipleChoiceCell.setCellValue("(Több válasz is megjelölhető volt)");
        }

        if (isOptional && type != null && !isCompletedByCurrentUser) optionalCell.setCellValue("(opcionális)");

        Row questionHeaderRow3 = sheet.createRow(sheet.getLastRowNum() + 1);
        Cell emptyCell1 = questionHeaderRow3.createCell(0, CellType.STRING);
        emptyCell1.setCellStyle(questionOptionalStyle);
        Cell emptyCell2 = questionHeaderRow1.createCell(16, CellType.STRING);
        emptyCell2.setCellStyle(questionHeaderStyle);
    }

    private void createExportEmptyQuestionField(Sheet sheet, XSSFCellStyle style) {
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 1,
                sheet.getLastRowNum() + 1, 0, 21));
        sheet.addMergedRegion(new CellRangeAddress(sheet.getLastRowNum() + 2,
                sheet.getLastRowNum() + 2, 0, 21));

        Row row1 = sheet.createRow(sheet.getLastRowNum() + 1);
        Cell emptyCell1 = row1.createCell(0, CellType.STRING);
        emptyCell1.setCellStyle(style);

        Row row2 = sheet.createRow(sheet.getLastRowNum() + 1);
        Cell emptyCell2 = row2.createCell(0, CellType.STRING);
        emptyCell2.setCellStyle(style);
    }

    private XSSFFont createExportFont(XSSFWorkbook workbook, boolean isBold, XSSFColor color) {
        XSSFFont font = workbook.createFont();
        font.setBold(isBold);
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 12);
        font.setColor(color);

        return font;
    }

    // IMPORT:

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

    // EXPORT:

    /* Finally, let's write the content to a “temp.xlsx” file in the current directory and close the workbook:
       File currDir = new File(".");
       String path = currDir.getAbsolutePath();
       String fileLocation = path.substring(0, path.length() - 1) + "temp.xlsx";

       FileOutputStream outputStream = new FileOutputStream(fileLocation);
       workbook.write(outputStream);
       workbook.close(); */
}
