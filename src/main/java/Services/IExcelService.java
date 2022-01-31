package Services;

import DTOs.ExcelDTO;

import javax.servlet.http.HttpServletResponse;

public interface IExcelService {
    ExcelDTO readFromUploadedExcel(ExcelDTO request) throws Exception;
    void downloadExcelTest(HttpServletResponse response) throws Exception;
}
