package Services;

import Models.ExcelUploadStatics;
import Models.UnionMembershipNumber;
import Models.User;
import Repositories.ExcelUploadStaticsRepository;
import Repositories.UnionMembershipNumRepository;
import Repositories.UserRepository;
import Utils.Enums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UnionMembershipNumService implements IUnionMembershipNumService {

  @Autowired
  public UserRepository userRepository;
  @Autowired
  public UnionMembershipNumRepository unionMembershipNumRepo;
  @Autowired
  public ExcelUploadStaticsRepository excelUploadStaticsRepository;

  public ExcelUploadStatics saveNumberWithCheck(Map<Integer, List<UnionMembershipNumber>> unMembNumbers, String originalFileName) {
    Map<String, UnionMembershipNumber> insertedUnMembNumbers = new HashMap<>();
    List<UnionMembershipNumber> inactivatedUnMembNumbers;

    List<UnionMembershipNumber> allUnMembNumbers = unionMembershipNumRepo.findAll();

    List<User> usersToUpdate = new ArrayList<>();

    List<Integer> sheetNumbers = new ArrayList<>(unMembNumbers.keySet());
    for (Integer sheetNumber : sheetNumbers) {
      for (UnionMembershipNumber unionNum : unMembNumbers.get(sheetNumber)) {
        UnionMembershipNumber sameUnMembNum = null;

        if (allUnMembNumbers != null && !allUnMembNumbers.isEmpty()) {
          sameUnMembNum = allUnMembNumbers.stream().filter(un -> un.getUnionMembershipNum()
              .equals(unionNum.getUnionMembershipNum())).findFirst().orElse(null);
        }

        if (sameUnMembNum == null) {
          unionNum.setActive(true);
          insertedUnMembNumbers.put(unionNum.getUnionMembershipNum(), unionNum);
        }
        else {
          sameUnMembNum.setNeedToInactivate(false);

          if (!sameUnMembNum.isActive() || (sameUnMembNum.getRegistrationNumber() != null
                  && !sameUnMembNum.getRegistrationNumber().equals(unionNum.getRegistrationNumber()))) {

            User user = userRepository.findByUnionMembershipNum(sameUnMembNum.getUnionMembershipNum());
            if (user != null) {

              if (!sameUnMembNum.getRegistrationNumber().equals(unionNum.getRegistrationNumber())) {
                user.setRole(Enums.Role.USER);
                user.setUnionMembershipNum(null);
              }
              else user.setRole(Enums.Role.UNION_MEMBER_USER);

              usersToUpdate.add(user);
            }
            sameUnMembNum.setActive(true);
            sameUnMembNum.setRegistrationNumber(unionNum.getRegistrationNumber());

            insertedUnMembNumbers.put(sameUnMembNum.getUnionMembershipNum(), sameUnMembNum);
          }

          if (sameUnMembNum.getRegistrationNumber() == null && unionNum.getRegistrationNumber() != null) {
            sameUnMembNum.setRegistrationNumber(unionNum.getRegistrationNumber());
            insertedUnMembNumbers.put(sameUnMembNum.getUnionMembershipNum(), sameUnMembNum);
          }
        }
      }
    }
    inactivatedUnMembNumbers = allUnMembNumbers.stream().filter(un -> un.isNeedToInactivate()).collect(Collectors.toList());

    if (!insertedUnMembNumbers.isEmpty()) unionMembershipNumRepo.saveAll(insertedUnMembNumbers.values());

    if (!inactivatedUnMembNumbers.isEmpty()) {
      for (UnionMembershipNumber un : inactivatedUnMembNumbers) {
        un.setActive(false);

        User user = userRepository.findByUnionMembershipNum(un.getUnionMembershipNum());
        if (user != null) {
          // user.setUnionMembershipNum(null);
          user.setRole(Enums.Role.USER);
          usersToUpdate.add(user);
        }
      }
      unionMembershipNumRepo.saveAll(inactivatedUnMembNumbers);
    }
    if (!usersToUpdate.isEmpty()) userRepository.saveAll(usersToUpdate);

    ExcelUploadStatics statics = new ExcelUploadStatics();

    ExcelUploadStatics foundStatics = excelUploadStaticsRepository.findFirstByTypeOfUpload(Enums.ExcelUploadType.UNION_MEMBERSHIP_NUMBER);
    if (foundStatics != null) statics.setId(foundStatics.getId());

    statics.setLastUploadedFile(originalFileName);
    statics.setLastUpload(LocalDateTime.now());
    statics.setUpdatedBy(Enums.Role.UNION_MEMBER_ADMIN);
    statics.setTypeOfUpload(Enums.ExcelUploadType.UNION_MEMBERSHIP_NUMBER);

    Integer numberOfActiveUnionMemberships;
    numberOfActiveUnionMemberships = unionMembershipNumRepo.getNumberOfUnionMemberUsers();
    Integer numberOfRecords;
    numberOfRecords = unionMembershipNumRepo.getNumberOfRecords();
    Integer numberOfInactiveUsers = numberOfRecords - numberOfActiveUnionMemberships;
    statics.setNumberOfActiveElements(numberOfActiveUnionMemberships);
    statics.setNumberOfInactiveElements(numberOfInactiveUsers);

    excelUploadStaticsRepository.save(statics);

    return statics;
  }
}
