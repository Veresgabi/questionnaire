package Services;


import Models.ExcelUploadStatics;
import Models.UnionMembershipNumber;

import java.util.List;
import java.util.Map;

public interface IUnionMembershipNumService {

  ExcelUploadStatics saveNumberWithCheck(Map<Integer, List<UnionMembershipNumber>> regNumbers, String originalFileName);
}
