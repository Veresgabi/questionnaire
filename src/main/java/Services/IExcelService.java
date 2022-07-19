package Services;

import DTOs.ExcelDTO;
import DTOs.QuestionnaireDTO;
import Models.User;

import javax.servlet.http.HttpServletResponse;

public interface IExcelService {
    ExcelDTO readFromUploadedExcel(ExcelDTO request) throws Exception;
    void exportResultToExcel(HttpServletResponse response, QuestionnaireDTO questionnaireDTO) throws Exception;
    void exportAnswersToExcel(HttpServletResponse response, QuestionnaireDTO questionnaireDTO) throws Exception;
    ExcelDTO getExcelStatics(ExcelDTO excelDTO);
}
