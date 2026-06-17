package TrackTogether.dto;

import TrackTogether.domain.TravelGroup;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// View data used by the travel group overview page
@Getter
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

    // Stores all values the Thymeleaf overview needs in one object
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

}