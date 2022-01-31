package DTOs;

import java.util.UUID;

public class UserRequestDTO {

    private Long currentUserId;
    private Long deletedUserId;
    private UUID tokenUUID;
    private String registrationNum;

    public UserRequestDTO(Long currentUserId, Long deletedUserId, UUID tokenUUID, String registrationNum) {
        this.currentUserId = currentUserId;
        this.deletedUserId = deletedUserId;
        this.tokenUUID = tokenUUID;
        this.registrationNum = registrationNum;
    }

    public UserRequestDTO() {}

    public Long getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(Long currentUserId) {
        this.currentUserId = currentUserId;
    }

    public Long getDeletedUserId() {
        return deletedUserId;
    }

    public void setDeletedUserId(Long deletedUserId) {
        this.deletedUserId = deletedUserId;
    }

    public UUID getTokenUUID() {
        return tokenUUID;
    }

    public void setTokenUUID(UUID tokenUUID) {
        this.tokenUUID = tokenUUID;
    }

    public String getRegistrationNum() {
        return registrationNum;
    }

    public void setRegistrationNum(String registrationNum) {
        this.registrationNum = registrationNum;
    }
}
