package DTOs;

import Models.User;

import java.util.UUID;

public class UserResponseDTO extends AbstractDTO {

    private boolean isValidUser;

    public UserResponseDTO(boolean isValidUserr) {
        this.isValidUser = isValidUser;
    }

    public UserResponseDTO() {}

    public boolean isValidUser() {
        return isValidUser;
    }

    public void setValidUser(boolean validUser) {
        isValidUser = validUser;
    }
}
