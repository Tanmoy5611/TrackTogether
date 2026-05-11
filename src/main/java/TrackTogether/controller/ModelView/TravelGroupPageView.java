package TrackTogether.controller.ModelView;

import TrackTogether.domain.TravelGroup;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TravelGroupPageView {
    private final List<TravelGroup> groups;
    private final List<TravelGroup> myTravelGroups;
    private final List<TravelGroup> exploreTravelGroups;
    private final Set<UUID> joinedGroupIds;
    private final Set<UUID> ownedGroupIds;
    private final Set<UUID> deletableOwnedGroupIds;
    private final Map<UUID, Long> memberCounts;
    private final boolean joinApprovalRequired;
    private final String ownerCannotLeaveMessage;
    private final Set<UUID> pendingJoinRequestGroupIds;
    private final Set<UUID> rejectedJoinRequestGroupIds;
    private final Map<UUID, Long> pendingJoinRequestCounts;

    public TravelGroupPageView(List<TravelGroup> groups,
                               List<TravelGroup> myTravelGroups,
                               List<TravelGroup> exploreTravelGroups,
                               Set<UUID> joinedGroupIds,
                               Set<UUID> ownedGroupIds,
                               Set<UUID> deletableOwnedGroupIds,
                               Map<UUID, Long> memberCounts,
                               boolean joinApprovalRequired,
                               String ownerCannotLeaveMessage,
                               Set<UUID> pendingJoinRequestGroupIds,
                               Set<UUID> rejectedJoinRequestGroupIds,
                               Map<UUID, Long> pendingJoinRequestCounts) {
        this.groups = groups;
        this.myTravelGroups = myTravelGroups;
        this.exploreTravelGroups = exploreTravelGroups;
        this.joinedGroupIds = joinedGroupIds;
        this.ownedGroupIds = ownedGroupIds;
        this.deletableOwnedGroupIds = deletableOwnedGroupIds;
        this.memberCounts = memberCounts;
        this.joinApprovalRequired = joinApprovalRequired;
        this.ownerCannotLeaveMessage = ownerCannotLeaveMessage;
        this.pendingJoinRequestGroupIds = pendingJoinRequestGroupIds;
        this.rejectedJoinRequestGroupIds = rejectedJoinRequestGroupIds;
        this.pendingJoinRequestCounts = pendingJoinRequestCounts;
    }

    public List<TravelGroup> getGroups() {
        return groups;
    }

    public List<TravelGroup> getMyTravelGroups() {
        return myTravelGroups;
    }

    public List<TravelGroup> getExploreTravelGroups() {
        return exploreTravelGroups;
    }

    public Set<UUID> getJoinedGroupIds() {
        return joinedGroupIds;
    }

    public Set<UUID> getOwnedGroupIds() {
        return ownedGroupIds;
    }

    public Set<UUID> getDeletableOwnedGroupIds() {
        return deletableOwnedGroupIds;
    }

    public Map<UUID, Long> getMemberCounts() {
        return memberCounts;
    }

    public boolean isJoinApprovalRequired() {
        return joinApprovalRequired;
    }

    public String getOwnerCannotLeaveMessage() {
        return ownerCannotLeaveMessage;
    }

    public Set<UUID> getPendingJoinRequestGroupIds() {
        return pendingJoinRequestGroupIds;
    }

    public Set<UUID> getRejectedJoinRequestGroupIds() {
        return rejectedJoinRequestGroupIds;
    }

    public Map<UUID, Long> getPendingJoinRequestCounts() {
        return pendingJoinRequestCounts;
    }
}