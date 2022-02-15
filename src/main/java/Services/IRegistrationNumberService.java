package Services;

import Models.ExcelUploadStatics;
import Models.RegistrationNumber;

import java.util.List;
import java.util.Map;

public interface IRegistrationNumberService {

    ExcelUploadStatics saveNumberWithCheck(Map<String, List<String>> regNumbers, String originalFileName,
                                           boolean needToInactivate);
}
