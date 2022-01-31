package Controllers;

import DTOs.UserResponseDTO;
import Models.Token;
import Models.User;
import Services.ITokenService;
import Services.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Controller
@CrossOrigin
@RequestMapping("/token")
public class TokenController {

    @Autowired
    public ITokenService tokenService;
    @Autowired
    public IUserService userService;

    @PostMapping("/checkToken")
    @ResponseBody
    public UserResponseDTO checkToken(@RequestBody User user) {
        UserResponseDTO response = tokenService.checkToken(user.getTokenUUID());
        return userService.removeUserPassword(response);
    }
}
