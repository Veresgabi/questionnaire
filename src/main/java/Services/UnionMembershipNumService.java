package Services;

import Models.UnionMembershipNumber;
import Models.User;
import Repositories.UnionMembershipNumRepository;
import Repositories.UserRepository;
import Utils.Enums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UnionMembershipNumService implements IUnionMembershipNumService {

  @Autowired
  public UserRepository userRepository;
  @Autowired
  public UnionMembershipNumRepository unionMembershipNumRepo;

  public void saveNumberWithCheck(Map<Integer, List<String>> unMembNumbers) {
    List<UnionMembershipNumber> insertedUnMembNumbers = new ArrayList<>();
    List<UnionMembershipNumber> inactivatedUnMembNumbers = new ArrayList<>();

    List<UnionMembershipNumber> allUnMembNumbers = unionMembershipNumRepo.findAll();

    List<User> usersToSetRole = new ArrayList<>();

    List<Integer> sheetNumbers = new ArrayList<>(unMembNumbers.keySet());
    for (Integer sheetNumber : sheetNumbers) {
      for (String unionNum : unMembNumbers.get(sheetNumber)) {
        UnionMembershipNumber sameUnMembNum = null;

        if (allUnMembNumbers != null && !allUnMembNumbers.isEmpty()) {
          sameUnMembNum = allUnMembNumbers.stream().filter(un -> un.getUnionMembershipNum()
              .equals(unionNum)).findFirst().orElse(null);
        }

        if (sameUnMembNum == null) {
          UnionMembershipNumber unionMembershipNumber = new UnionMembershipNumber();
          unionMembershipNumber.setUnionMembershipNum(unionNum);
          unionMembershipNumber.setActive(true);
          insertedUnMembNumbers.add(unionMembershipNumber);
        }
        else {
          sameUnMembNum.setNeedToInactivate(false);

          if (!sameUnMembNum.isActive()) {
            sameUnMembNum.setActive(true);
            User user = userRepository.findByUnionMembershipNum(sameUnMembNum.getUnionMembershipNum());
            if (user != null) {
              user.setRole(Enums.Role.UNION_MEMBER_USER);
              usersToSetRole.add(user);
            }
            insertedUnMembNumbers.add(sameUnMembNum);
          }
        }
      }
    }
    inactivatedUnMembNumbers = allUnMembNumbers.stream().filter(rn -> rn.isNeedToInactivate()).collect(Collectors.toList());

    if (!insertedUnMembNumbers.isEmpty()) unionMembershipNumRepo.saveAll(insertedUnMembNumbers);

    if (!inactivatedUnMembNumbers.isEmpty()) {
      for (UnionMembershipNumber un : inactivatedUnMembNumbers) {
        un.setActive(false);

        User user = userRepository.findByUnionMembershipNum(un.getUnionMembershipNum());
        if (user != null) {
          // user.setUnionMembershipNum(null);
          user.setRole(Enums.Role.USER);
          usersToSetRole.add(user);
        }
      }
      unionMembershipNumRepo.saveAll(inactivatedUnMembNumbers);
      userRepository.saveAll(usersToSetRole);
    }
  }
}
