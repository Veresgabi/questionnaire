package DTOs;

import Models.User;

import java.util.UUID;

public class AbstractDTO {

    private UUID tokenUUID;
    private String responseText;
    private boolean isSuccessful;
    private boolean isAuthSuccess;
    private boolean isExpiredPage;
    private User user;

    public AbstractDTO(UUID tokenUUID, String responseText, boolean isSuccessful, boolean isAuthSuccess, boolean isExpiredPage, User user) {
        this.tokenUUID = tokenUUID;
        this.responseText = responseText;
        this.isSuccessful = isSuccessful;
        this.isAuthSuccess = isAuthSuccess;
        this.isExpiredPage = isExpiredPage;
        this.user = user;
    }

    public AbstractDTO() { }

    public UUID getTokenUUID() {
        return tokenUUID;
    }

    public void setTokenUUID(UUID tokenUUID) {
        this.tokenUUID = tokenUUID;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public void setSuccessful(boolean successful) {
        isSuccessful = successful;
    }

    public boolean isAuthSuccess() {
        return isAuthSuccess;
    }

    public void setAuthSuccess(boolean authSuccess) {
        isAuthSuccess = authSuccess;
    }

    public boolean isExpiredPage() {
        return isExpiredPage;
    }

    public void setExpiredPage(boolean expiredPage) {
        isExpiredPage = expiredPage;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
