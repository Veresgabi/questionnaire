package Controllers;

import DTOs.ExcelDTO;
import Models.User;
import Services.IExcelService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@CrossOrigin
@RequestMapping("/excel")
public class ExcelController {

    @Autowired
    IExcelService excelService;

    @Transactional
    @PostMapping("/readFromExcel")
    @ResponseBody
    public ExcelDTO readFromExcel(@RequestParam("file") MultipartFile file,
                                  @RequestParam("tokenUUID") String tokenUUID,
                                  @RequestParam("userId") String userId) throws Exception {
        UUID token = null;
        if (tokenUUID != null && !tokenUUID.equals("null")) {
            token = UUID.fromString(tokenUUID);
        }
        User user = new User();
        user.setId(Long.parseLong(userId));
        ExcelDTO request = new ExcelDTO();
        request.setFile(file);
        request.setTokenUUID(token);
        request.setUser(user);

        return excelService.readFromUploadedExcel(request);
    }

    @Transactional
    @PostMapping("/getExcelStatics")
    @ResponseBody
    public ExcelDTO getExcelStatics(@RequestBody ExcelDTO excelDTO) {
        return excelService.getExcelStatics(excelDTO);
    }

    @GetMapping("/downloadExcelTest")
    public void downloadExcelTest(HttpServletResponse response) throws Exception {

        response.setContentType("application/octet-stream");
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateTime = dateFormatter.format(new Date());

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=users_" + currentDateTime + ".xlsx";
        response.setHeader(headerKey, headerValue);

        excelService.downloadExcelTest(response);
    }
}
