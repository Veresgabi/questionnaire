package Services;

import Models.ExcelUploadStatics;
import Models.RegistrationNumber;
import Models.User;

import java.util.List;
import java.util.Map;

public interface IRegistrationNumberService {

    ExcelUploadStatics saveNumberWithCheck(User currentUser, Map<String, List<String>> regNumbers, String originalFileName,
                                           boolean needToInactivate);
}
