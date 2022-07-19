package Controllers;

import DTOs.ExcelDTO;
import DTOs.QuestionnaireDTO;
import Models.RegistrationNumber;
import Models.TextualQuestion;
import Models.UnionMembershipNumber;
import Models.User;
import Repositories.RegNumberRepository;
import Repositories.UnionMembershipNumRepository;
import Services.IExcelService;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
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

    @Autowired
    public UnionMembershipNumRepository unionMembershipNumRepository;

    @Autowired
    public RegNumberRepository regNumberRepository;

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

    @PostMapping("/downloadExcelTest")
    public void downloadExcelTest(HttpServletResponse response) throws Exception {

        response.setContentType("application/octet-stream");
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateTime = dateFormatter.format(new Date());

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=users_" + currentDateTime + ".xlsx";
        response.setHeader(headerKey, headerValue);

        excelService.exportAnswersToExcel(response, new QuestionnaireDTO());
    }

    @PostMapping("/downloadAnswerExcel")
    public void downloadAnswerExcel(HttpServletRequest request, HttpServletResponse response) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        try {
            String questionnaireDtoJson = request.getHeader("QuestionnaireDTO");
            questionnaireDtoJson = removeUnicode(questionnaireDtoJson);
            QuestionnaireDTO questionnaireDTO = objectMapper.readValue(questionnaireDtoJson, QuestionnaireDTO.class);
            excelService.exportAnswersToExcel(response, questionnaireDTO);
        }
        catch (Exception e) {
            if (response.getStatus() == 200) response.setStatus(500);
        }
    }

    @PostMapping("/downloadResultExcel")
    public void downloadResultExcel(HttpServletRequest request, HttpServletResponse response) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        try {
            String questionnaireDtoJson = request.getHeader("QuestionnaireDTO");
            questionnaireDtoJson = removeUnicode(questionnaireDtoJson);
            QuestionnaireDTO questionnaireDTO = objectMapper.readValue(questionnaireDtoJson, QuestionnaireDTO.class);
            excelService.exportResultToExcel(response, questionnaireDTO);
        }
        catch (Exception e) {
            if (response.getStatus() == 200) response.setStatus(500);
        }
    }

    private String removeUnicode(String text) {
        return text
                .replace("U+00C1", "Á")
                .replace("U+00E1", "á")
                .replace("U+00C9", "É")
                .replace("U+00E9", "é")
                .replace("U+00CD", "Í")
                .replace("U+00ED", "í")
                .replace("U+00D3", "Ó")
                .replace("U+00F3", "ó")
                .replace("U+00D6", "Ö")
                .replace("U+00F6", "ö")
                .replace("U+0150", "Ő")
                .replace("U+0151", "ő")
                .replace("U+00DA", "Ú")
                .replace("U+00FA", "ú")
                .replace("U+00DC", "Ü")
                .replace("U+00FC", "ü")
                .replace("U+0170", "Ű")
                .replace("U+0171", "ű");
    }

    @GetMapping("/testGetUnionMembers")
    @ResponseBody
    public List<UnionMembershipNumber> testGetUnionMembershipNumbers() {
        return unionMembershipNumRepository.findAllOrderById();
    }

    @GetMapping("/testGetRegistrationNumbers")
    @ResponseBody
    public List<RegistrationNumber> testGetRegistrationNumbers() {
        return regNumberRepository.findAllOrderById();
    }
}
