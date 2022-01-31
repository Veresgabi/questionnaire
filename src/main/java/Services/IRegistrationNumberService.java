package Services;

import Models.RegistrationNumber;

import java.util.List;
import java.util.Map;

public interface IRegistrationNumberService {

    void saveNumberWithCheck(Map<String, List<String>> regNumbers);
}
