package TrackTogether.controller.ModelView;

import TrackTogether.domain.ActivityVerificationStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class ActivityView {
    private UUID id;
    private String name;
    private String location;
    private LocalDate date;
    private LocalTime time;
    private String creatorName;
    private boolean kdgActivity;
    private ActivityVerificationStatus verificationStatus;
    private boolean canVerify;
    private boolean canRemove;

    public ActivityView(UUID id,
                        String name,
                        String location,
                        LocalDate date,
                        LocalTime time,
                        String creatorName,
                        boolean kdgActivity,
                        ActivityVerificationStatus verificationStatus,
                        boolean canVerify,
                        boolean canRemove) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.date = date;
        this.time = time;
        this.creatorName = creatorName;
        this.kdgActivity = kdgActivity;
        this.verificationStatus = verificationStatus;
        this.canVerify = canVerify;
        this.canRemove = canRemove;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public boolean isKdgActivity() {
        return kdgActivity;
    }

    public void setKdgActivity(boolean kdgActivity) {
        this.kdgActivity = kdgActivity;
    }

    public ActivityVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(ActivityVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public boolean isCanVerify() {
        return canVerify;
    }

    public void setCanVerify(boolean canVerify) {
        this.canVerify = canVerify;
    }

    public boolean isCanRemove() {
        return canRemove;
    }

    public void setCanRemove(boolean canRemove) {
        this.canRemove = canRemove;
    }
}