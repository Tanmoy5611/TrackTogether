package TrackTogether.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class SystemSettings {

    // kept one settings row for the whole platform
    @Id
    private Long id = 1L;

    // When false, students join travel groups directly
    private boolean travelGroupJoinApprovalEnabled;

    public Long getId() {
        return id;
    }

    public boolean isTravelGroupJoinApprovalEnabled() {
        return travelGroupJoinApprovalEnabled;
    }

    public void setTravelGroupJoinApprovalEnabled(boolean travelGroupJoinApprovalEnabled) {
        this.travelGroupJoinApprovalEnabled = travelGroupJoinApprovalEnabled;
    }
}