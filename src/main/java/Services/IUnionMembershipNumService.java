package Services;


import java.util.List;
import java.util.Map;

public interface IUnionMembershipNumService {

  void saveNumberWithCheck(Map<Integer, List<String>> regNumbers);
}
