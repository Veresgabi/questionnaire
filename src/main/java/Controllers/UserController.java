package Controllers;

import DTOs.UserRequestDTO;
import DTOs.UserResponseDTO;
import Models.User;
import Repositories.UserRepository;
import Services.IUserService;
import Utils.Enums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@CrossOrigin
@RequestMapping("/user")
public class UserController {

    @Autowired
    public IUserService userService;

    @Autowired
    public UserRepository userRepository;

    @PostMapping("/saveUser")
    @ResponseBody
    public UserResponseDTO saveUser(@RequestBody User user) throws Exception {
        UserResponseDTO response = userService.saveUser(user);
        return userService.removeUserPassword(response);
    }

    @PostMapping("/login")
    @ResponseBody
    public UserResponseDTO login(@RequestBody User user) throws Exception {
        UserResponseDTO response = userService.login(user);
        return response;
    }

    @PostMapping("/logout")
    @ResponseBody
    public UserResponseDTO logout(@RequestBody User user) {
        UserResponseDTO response = userService.logout(user);
        return response;
    }

    @PostMapping("/changePassword")
    @ResponseBody
    public UserResponseDTO changePassword(@RequestBody User user) throws Exception {
        UserResponseDTO response = userService.changePassword(user);
        return response;
    }

    @PostMapping("/findUserByRegNumber")
    @ResponseBody
    public UserResponseDTO findUserByRegistrationNumber(@RequestBody UserRequestDTO userRequestDTO) {
        UserResponseDTO response = userService.findUserByRegistrationNumber(userRequestDTO);
        return userService.removeUserPassword(response);
    }

    @PostMapping("/deleteUserById")
    @ResponseBody
    public UserResponseDTO deleteUserById(@RequestBody UserRequestDTO userRequestDTO) {
        UserResponseDTO response = userService.deleteUserById(userRequestDTO);
        return userService.removeUserPassword(response);
    }

    @PostMapping("/sendUnionNumber")
    @ResponseBody
    public UserResponseDTO sendUnionNumber(@RequestBody User user) {
        UserResponseDTO response = userService.sendUnionNumber(user);
        return userService.removeUserPassword(response);
    }

    @PostMapping("/isExpiredPage")
    @ResponseBody
    public UserResponseDTO isExpiredPage(@RequestBody User user) {
        UserResponseDTO response = userService.isExpiredPage(user);
        return userService.removeUserPassword(response);
    }

    @PostMapping("/refreshToken")
    @ResponseBody
    public UserResponseDTO checkAndRefreshToken(@RequestBody User user) throws Exception {
        UserResponseDTO response = userService.checkAndRefreshToken(user);
        return userService.removeUserPassword(response);
    }

    @PostMapping("/addAdmin")
    @ResponseBody
    public User addAdmin(@RequestBody User user) throws Exception {
        User user1 = new User();
        user1.setUserName("Admin");
        user1.setPassword("admin123");
        user1.setPrivacyStatement(true);
        user1.setRole(Enums.Role.ADMIN);
        return userRepository.save(user1);
    }

    @PostMapping("/addUnionMemberAdmin")
    @ResponseBody
    public UserResponseDTO addUnionMemberAdmin(@RequestBody User user) throws Exception {
        User user1 = new User();
        user1.setUserName("AdminSzakszerv");
        user1.setPassword("admin456");
        user1.setPrivacyStatement(true);
        user1.setRole(Enums.Role.UNION_MEMBER_ADMIN);

        UserResponseDTO response = new UserResponseDTO();

        try {
            userRepository.save(user1);
            response.setSuccessful(true);
        }
        catch (Exception e) {
            response.setResponseText("A hiba a következő: " + e);
        }
        return response;
    }
}
