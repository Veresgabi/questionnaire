package Services;

import DTOs.AbstractDTO;
import DTOs.UserRequestDTO;
import DTOs.UserResponseDTO;
import Models.RegistrationNumberQuestionnaire;
import Models.User;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface IUserService {

    UserResponseDTO getApiResponse();
    UserResponseDTO saveUser(User user) throws Exception;
    String confirmRegistration(String id);
    UserResponseDTO login(User user) throws Exception;
    UserResponseDTO logout(User user);
    UserResponseDTO changePassword(User user) throws Exception;
    UserResponseDTO findUserByRegistrationNumber(UserRequestDTO userRequestDTO);
    UserResponseDTO deleteUserById(UserRequestDTO user);
    UserResponseDTO sendUnionNumber(User user);
    UserResponseDTO isExpiredPage(User user);
    UserResponseDTO checkAndRefreshToken(User user);
    <T extends AbstractDTO> T removeUserPassword(T response);
    UserResponseDTO testTransactionOperation(User user, User userWithoutRegNum, List<RegistrationNumberQuestionnaire> rnqList);
}
