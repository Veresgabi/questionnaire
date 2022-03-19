package Services;

import DTOs.UserResponseDTO;
import Models.Token;
import Models.User;
import Utils.Enums;

import java.util.List;
import java.util.UUID;

public interface ITokenService {

    UserResponseDTO checkToken(UUID tokenUUID);
    UserResponseDTO refreshToken(UUID tokenUUID, User user);
    UserResponseDTO deleteToken(UUID tokenUUID);
    Token createToken(Enums.Role role);
    Token createToken(Enums.Role role, Integer expireTimeInHours);
    Token createToken(User user);
}
