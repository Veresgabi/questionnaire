package Controllers;

import DTOs.UserRequestDTO;
import DTOs.UserResponseDTO;
import Models.User;
import Services.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@CrossOrigin
@RequestMapping("/user")
public class UserController {

    @Autowired
    public IUserService userService;


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
}
