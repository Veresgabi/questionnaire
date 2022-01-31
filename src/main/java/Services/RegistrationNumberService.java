package Services;

import Models.RegistrationNumber;
import Models.UnionMembershipNumber;
import Models.User;
import Repositories.RegNumberRepository;
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
public class RegistrationNumberService implements IRegistrationNumberService {

    @Autowired
    public RegNumberRepository regNumberRepository;
    @Autowired
    public UserRepository userRepository;
    @Autowired
    public UnionMembershipNumRepository unionMembershipNumRepo;

    public void saveNumberWithCheck(Map<String, List<String>> regNumbers) {

        List<RegistrationNumber> insertedRegNumbers = new ArrayList<>();
        List<RegistrationNumber> inactivatedRegNumbers = new ArrayList<>();

        List<RegistrationNumber> allRegNumbers = regNumberRepository.findAll();

        List<String> sheetNames = new ArrayList<>(regNumbers.keySet());
        for (String sheetName : sheetNames) {
            Enums.Location location = null;

            if (sheetName.equals("Makó")) location = Enums.Location.MAKO;
            else if (sheetName.equals("Makó_kölcsönzött")) location = Enums.Location.MAKO_BORROWED;
            else if (sheetName.equals("Vác")) location = Enums.Location.VAC;
            else if (sheetName.equals("Vác_kölcsönzött")) location = Enums.Location.VAC_BORROWED;

            for (String regNum : regNumbers.get(sheetName)) {
                RegistrationNumber sameRegNum = null;

                if (allRegNumbers != null && !allRegNumbers.isEmpty()) {
                    sameRegNum = allRegNumbers.stream().filter(rn -> rn.getRegistrationNum()
                        .equals(regNum)).findFirst().orElse(null);
                }

                if (sameRegNum == null) {
                    RegistrationNumber registrationNumber = new RegistrationNumber();
                    registrationNumber.setRegistrationNum(regNum);
                    registrationNumber.setActive(true);
                    registrationNumber.setFirstName("");
                    registrationNumber.setLastName("");
                    registrationNumber.setLocation(location);
                    insertedRegNumbers.add(registrationNumber);
                }
                else {
                    sameRegNum.setNeedToInactivate(false);
                    sameRegNum.setLocation(location);

                    if (!sameRegNum.isActive()) {
                        sameRegNum.setActive(true);
                        insertedRegNumbers.add(sameRegNum);
                    }
                }
            }
        }
        inactivatedRegNumbers = allRegNumbers.stream().filter(rn -> rn.isNeedToInactivate()).collect(Collectors.toList());

        if (!insertedRegNumbers.isEmpty()) regNumberRepository.saveAll(insertedRegNumbers);

        List<User> usersToDelete = new ArrayList<>();
        List<UnionMembershipNumber> unionMembNumsToInactive = new ArrayList<>();
        if (!inactivatedRegNumbers.isEmpty()) {
            for (RegistrationNumber rn : inactivatedRegNumbers) {
                rn.setActive(false);

                User user = userRepository.findByRegistrationNum(rn.getRegistrationNum());
                if (user != null) {
                    usersToDelete.add(user);

                    // TODO: Test inactivate the related union member number
                    UnionMembershipNumber unionMembershipNumber = unionMembershipNumRepo.findByUnionMembershipNum(user.getUnionMembershipNum());
                    if (unionMembershipNumber != null) {
                        unionMembershipNumber.setActive(false);
                        unionMembNumsToInactive.add(unionMembershipNumber);
                    }
                }
            }
            regNumberRepository.saveAll(inactivatedRegNumbers);
            userRepository.deleteAll(usersToDelete);
            unionMembershipNumRepo.saveAll(unionMembNumsToInactive);
        }
    }
}
