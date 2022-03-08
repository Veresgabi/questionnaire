package DTOs;

import Models.User;

import java.util.List;
import java.util.UUID;

public class UserResponseDTO extends AbstractDTO {

    private boolean isValidUser;

    private List<User> users;

    public UserResponseDTO(boolean isValidUser, List<User> users) {
        this.isValidUser = isValidUser;
        this.users = users;
    }

    public UserResponseDTO() {}

    public boolean isValidUser() {
        return isValidUser;
    }

    public void setValidUser(boolean validUser) {
        isValidUser = validUser;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
