package TrackTogether.webapi.dto;

public class UpdateUserRequest {
    private Boolean status;
    private String role;

    public Boolean getStatus() {
        return status;
    }
    public void setStatus(Boolean status) {
        this.status = status;
    }

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
}