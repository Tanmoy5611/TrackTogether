package TrackTogether.service;

import TrackTogether.domain.*;
import TrackTogether.controller.ModelView.TravelGroupPageView;
import TrackTogether.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TravelGroupService {

    private static final String OWNER_CANNOT_LEAVE_MESSAGE =
            "Transfer ownership before leaving this travel group.";

    private final TravelGroupRepository travelGroupRepository;
    private final ActivityRepository activityRepository;
    private final ConversationRepository conversationRepository;
    private final TravelGroupMemberRepository travelGroupMemberRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final CurrentUserService currentUserService;
    private final SystemSettingsService systemSettingsService;

    public TravelGroupService(TravelGroupRepository travelGroupRepository,
                              ActivityRepository activityRepository,
                              ConversationRepository conversationRepository,
                              TravelGroupMemberRepository travelGroupMemberRepository,
                              JoinRequestRepository joinRequestRepository,
                              CurrentUserService currentUserService,
                              SystemSettingsService systemSettingsService) {
        // All database access stays behind repositories and this service
        this.travelGroupRepository = travelGroupRepository;
        this.activityRepository = activityRepository;
        this.conversationRepository = conversationRepository;
        this.travelGroupMemberRepository = travelGroupMemberRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.currentUserService = currentUserService;
        this.systemSettingsService = systemSettingsService;
    }

    // Returns every travel group from the database
    public List<TravelGroup> getAllTravelGroups() {
        return travelGroupRepository.findAll();
    }

    // Finds one travel group or returns a 404 when the id is unknown
    public TravelGroup getTravelGroupById(UUID groupId) {
        return travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel group not found"
                ));
    }

    // Returns all travel groups that belong to one activity
    public List<TravelGroup> getTravelGroupsForActivity(UUID activityId) {
        return travelGroupRepository.findAllByActivity_Id(activityId);
    }

    // Checks if the logged-in member is already in the given group
    public boolean isCurrentUserMember(TravelGroup group) {
        Member member = currentUserService.getCurrentUser();
        return travelGroupMemberRepository.existsByGroupAndMember(group, member);
    }

    // Counts how many members are currently in one group
    public long getMemberCount(TravelGroup group) {
        return travelGroupMemberRepository.countByGroup(group);
    }

    // Builds a lookup map so templates can show capacity without recalculating it
    public Map<UUID, Long> getMemberCounts(List<TravelGroup> groups) {
        return groups.stream()
                .collect(Collectors.toMap(
                        TravelGroup::getGroupId,
                        this::getMemberCount
                ));
    }

    // Builds all data needed by the travel groups list page
    public TravelGroupPageView buildTravelGroupsPage() {
        List<TravelGroup> groups = getAllTravelGroups();
        Set<UUID> joinedGroupIds = getJoinedGroupIds(groups);
        Set<UUID> ownedGroupIds = getOwnedGroupIds(groups);

        // The page view keeps list-page display data together instead of spreading it in the controller
        return new TravelGroupPageView(
                groups,
                getCurrentUserTravelGroups(groups, joinedGroupIds, ownedGroupIds),
                getExploreTravelGroups(groups, joinedGroupIds, ownedGroupIds),
                joinedGroupIds,
                ownedGroupIds,
                getDeletableOwnedGroupIds(groups),
                getMemberCounts(groups),
                isJoinApprovalRequired(),
                getOwnerCannotLeaveMessage(),
                getPendingJoinRequestGroupIds(groups),
                getRejectedJoinRequestGroupIds(groups),
                getPendingJoinRequestCounts(groups)
        );
    }

    // Returns ids of groups where the current user has a membership row
    public Set<UUID> getJoinedGroupIds(List<TravelGroup> groups) {
        Member member = currentUserService.getCurrentUser();

        return groups.stream()
                .filter(group -> travelGroupMemberRepository.existsByGroupAndMember(group, member))
                .map(TravelGroup::getGroupId)
                .collect(Collectors.toSet());
    }

    // Checks if the logged-in member is the current owner of the group
    public boolean isCurrentUserOwner(TravelGroup group) {
        Member member = currentUserService.getCurrentUser();
        return group.getOwner() != null
                && group.getOwner().getUserId().equals(member.getUserId());
    }

    // Returns the shared message used when an owner must transfer first
    public String getOwnerCannotLeaveMessage() {
        return OWNER_CANNOT_LEAVE_MESSAGE;
    }

    // Returns ids of groups owned by the current user
    public Set<UUID> getOwnedGroupIds(List<TravelGroup> groups) {
        Member member = currentUserService.getCurrentUser();

        return groups.stream()
                .filter(group -> group.getOwner() != null)
                .filter(group -> group.getOwner().getUserId().equals(member.getUserId()))
                .map(TravelGroup::getGroupId)
                .collect(Collectors.toSet());
    }

    // Returns ids of owned groups that can be deleted because the owner is alone
    public Set<UUID> getDeletableOwnedGroupIds(List<TravelGroup> groups) {
        return groups.stream()
                .filter(this::canCurrentUserDeleteTravelGroup)
                .map(TravelGroup::getGroupId)
                .collect(Collectors.toSet());
    }

    // Public helper for controllers that still need the current user's groups
    public List<TravelGroup> getCurrentUserTravelGroups(List<TravelGroup> groups) {
        Set<UUID> joinedGroupIds = getJoinedGroupIds(groups);
        Set<UUID> ownedGroupIds = getOwnedGroupIds(groups);

        return getCurrentUserTravelGroups(groups, joinedGroupIds, ownedGroupIds);
    }

    // Uses precomputed ids so the list page builder does not repeat the same checks
    private List<TravelGroup> getCurrentUserTravelGroups(List<TravelGroup> groups,
                                                         Set<UUID> joinedGroupIds,
                                                         Set<UUID> ownedGroupIds) {
        // These are the groups that should appear in "My joined travel groups"
        return groups.stream()
                .filter(group -> joinedGroupIds.contains(group.getGroupId()) || ownedGroupIds.contains(group.getGroupId()))
                .toList();
    }

    // Public helper for controllers that still need discoverable groups
    public List<TravelGroup> getExploreTravelGroups(List<TravelGroup> groups) {
        Set<UUID> joinedGroupIds = getJoinedGroupIds(groups);
        Set<UUID> ownedGroupIds = getOwnedGroupIds(groups);

        return getExploreTravelGroups(groups, joinedGroupIds, ownedGroupIds);
    }

    // Uses precomputed ids to decide which groups are still joinable/discoverable
    private List<TravelGroup> getExploreTravelGroups(List<TravelGroup> groups,
                                                     Set<UUID> joinedGroupIds,
                                                     Set<UUID> ownedGroupIds) {
        // These are the groups the current user can still discover or join
        return groups.stream()
                .filter(group -> !joinedGroupIds.contains(group.getGroupId()) && !ownedGroupIds.contains(group.getGroupId()))
                .toList();
    }

    // Returns group ids where the current user has a pending join request
    public Set<UUID> getPendingJoinRequestGroupIds(List<TravelGroup> groups) {
        return getJoinRequestGroupIds(groups, JoinRequestStatus.PENDING);
    }

    // Returns group ids where the current user's join request was rejected
    public Set<UUID> getRejectedJoinRequestGroupIds(List<TravelGroup> groups) {
        return getJoinRequestGroupIds(groups, JoinRequestStatus.REJECTED);
    }

    // Counts pending requests per group for the owner request badge
    public Map<UUID, Long> getPendingJoinRequestCounts(List<TravelGroup> groups) {
        return groups.stream()
                .collect(Collectors.toMap(
                        TravelGroup::getGroupId,
                        group -> joinRequestRepository.countByGroupAndStatus(group, JoinRequestStatus.PENDING)
                ));
    }

    // Returns the current user's request status for one group, if a request exists
    public JoinRequestStatus getCurrentUserJoinRequestStatus(TravelGroup group) {
        Member member = currentUserService.getCurrentUser();
        return joinRequestRepository.findByGroupAndMember(group, member)
                .map(JoinRequest::getStatus)
                .orElse(null);
    }

    // Shows pending requests only when the current user may manage them
    public List<JoinRequest> getVisiblePendingRequests(TravelGroup group) {
        if (!isCurrentUserOwner(group) || !isJoinApprovalRequired()) {
            return List.of();
        }

        return joinRequestRepository.findAllByGroupAndStatusOrderByRequestedAtAsc(group, JoinRequestStatus.PENDING);
    }

    // Reads the system setting that decides whether joining needs owner approval
    public boolean isJoinApprovalRequired() {
        // Central helper so controllers and templates all use the same setting
        return systemSettingsService.isTravelGroupJoinApprovalEnabled();
    }

    // Returns all membership rows for a group
    public List<TravelGroupMember> getMembersForGroup(TravelGroup group) {
        return travelGroupMemberRepository.findAllByGroup(group);
    }

    // Only the current owner can edit a travel group
    public boolean canCurrentUserEditTravelGroup(TravelGroup group) {
        return isCurrentUserOwner(group);
    }

    // Checks if the owner is allowed to delete the group through Delete & Leave
    public boolean canCurrentUserDeleteTravelGroup(TravelGroup group) {
        // An owner may delete only when they are the last member
        return isCurrentUserOwner(group)
                && isCurrentUserMember(group)
                && travelGroupMemberRepository.countByGroup(group) == 1;
    }

    // Returns joined members who can receive ownership from the current owner
    public List<TravelGroupMember> getOwnershipTransferCandidates(TravelGroup group) {
        if (group.getOwner() == null) {
            return List.of();
        }

        // The current owner is removed, because transferring to yourself changes nothing
        return travelGroupMemberRepository.findAllByGroup(group)
                .stream()
                .filter(membership -> membership.getMember() != null)
                .filter(membership -> !membership.getMember().getUserId().equals(group.getOwner().getUserId()))
                .toList();
    }

    // Deletes every travel group for an activity, including related memberships and requests
    @Transactional
    public void deleteTravelGroupsForActivity(UUID activityId) {
        List<TravelGroup> groups = travelGroupRepository.findAllByActivity_Id(activityId);

        for (TravelGroup group : groups) {
            deleteTravelGroupWithRelations(group);
        }
    }

    // Creates a new TravelGroup for a given activity
    @Transactional
    public TravelGroup createTravelGroup(UUID activityId,
                                         Integer maxMembers,
                                         String location,
                                         TransportMode mode,
                                         LocalDateTime departureTime) {
        return createTravelGroup(
                activityId,
                maxMembers,
                location,
                location,
                null,
                null,
                mode,
                departureTime,
                null
        );
    }

    // Creates a new TravelGroup with route data for matching
    @Transactional
    public TravelGroup createTravelGroup(UUID activityId,
                                         Integer maxMembers,
                                         String location,
                                         String departureLocation,
                                         Double departureLatitude,
                                         Double departureLongitude,
                                         TransportMode mode,
                                         LocalDateTime departureTime,
                                         LocalDateTime estimatedArrivalTime) {

        // Retrieve the activity from the database
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Activity not found"
                ));

        // Validate all form/API values before anything is saved
        validateTravelGroupDetails(
                activity,
                maxMembers,
                location,
                departureLocation,
                departureLatitude,
                departureLongitude,
                mode,
                departureTime,
                estimatedArrivalTime
        );

        // Create a new TravelGroup with the provided information
        String normalizedDepartureLocation = normalizeRequiredRouteLocation(location, departureLocation);
        String displayLocation = normalizeOptionalText(location);
        if (displayLocation == null) {
            displayLocation = normalizedDepartureLocation;
        }

        TravelGroup group = new TravelGroup(maxMembers, displayLocation, mode);
        group.setDepartureLocation(normalizedDepartureLocation);
        group.setDepartureLatitude(departureLatitude);
        group.setDepartureLongitude(departureLongitude);
        group.setDepartureTime(departureTime);
        group.setEstimatedArrivalTime(estimatedArrivalTime);
        group.setArrivalLatitude(activity.getLatitude());
        group.setArrivalLongitude(activity.getLongitude());

        Member owner = currentUserService.getCurrentUser();

        // Link the group to the activity and set the current user as owner
        group.setActivity(activity);
        group.setOwner(owner);

        // Save the group first so the conversation and membership can reference it
        TravelGroup savedGroup = travelGroupRepository.save(group);

        // Every travel group gets its own conversation
        Conversation conversation = new Conversation();
        conversation.setTravelGroup(savedGroup);
        conversation.setCreatedAt(LocalDateTime.now());

        savedGroup.setConversation(conversation);

        conversationRepository.save(conversation);

        // The owner is also stored as a normal member of the group
        TravelGroupMember ownerMembership = new TravelGroupMember();
        ownerMembership.setGroup(savedGroup);
        ownerMembership.setMember(owner);
        travelGroupMemberRepository.save(ownerMembership);

        return savedGroup;
    }

    // Updates editable group details and keeps the same validation rules as create
    @Transactional
    public TravelGroup updateTravelGroup(UUID groupId,
                                         Integer maxMembers,
                                         String location,
                                         TransportMode mode,
                                         LocalDateTime departureTime) {
        TravelGroup existingGroup = getTravelGroupById(groupId);
        return updateTravelGroup(
                groupId,
                maxMembers,
                location,
                location != null ? location : existingGroup.getDepartureLocation(),
                existingGroup.getDepartureLatitude(),
                existingGroup.getDepartureLongitude(),
                mode,
                departureTime,
                existingGroup.getEstimatedArrivalTime()
        );
    }

    // Updates editable route and group details
    @Transactional
    public TravelGroup updateTravelGroup(UUID groupId,
                                         Integer maxMembers,
                                         String location,
                                         String departureLocation,
                                         Double departureLatitude,
                                         Double departureLongitude,
                                         TransportMode mode,
                                         LocalDateTime departureTime,
                                         LocalDateTime estimatedArrivalTime) {
        TravelGroup group = getTravelGroupByIdForUpdate(groupId);
        ensureCurrentUserOwns(group);

        // Keep the edit rules the same as create, so bad values cannot be saved later
        validateTravelGroupDetails(
                group.getActivity(),
                maxMembers,
                location,
                departureLocation,
                departureLatitude,
                departureLongitude,
                mode,
                departureTime,
                estimatedArrivalTime
        );

        long currentMembers = travelGroupMemberRepository.countByGroup(group);
        if (maxMembers < currentMembers) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Maximum members cannot be lower than the current member count"
            );
        }

        // After all checks pass, update only the fields the owner is allowed to edit
        String normalizedDepartureLocation = normalizeRequiredRouteLocation(location, departureLocation);
        String displayLocation = normalizeOptionalText(location);
        if (displayLocation == null) {
            displayLocation = normalizedDepartureLocation;
        }

        group.setMaxMembers(maxMembers);
        group.setLocation(displayLocation);
        group.setDepartureLocation(normalizedDepartureLocation);
        group.setDepartureLatitude(departureLatitude);
        group.setDepartureLongitude(departureLongitude);
        group.setTransportMode(mode);
        group.setDepartureTime(departureTime);
        group.setEstimatedArrivalTime(estimatedArrivalTime);
        group.setArrivalLatitude(group.getActivity().getLatitude());
        group.setArrivalLongitude(group.getActivity().getLongitude());

        return travelGroupRepository.save(group);
    }

    // Creates or reopens a join request when approval is enabled
    @Transactional
    public void requestToJoinTravelGroup(UUID groupId) {
        TravelGroup group = getTravelGroupByIdForUpdate(groupId);

        Member member = currentUserService.getCurrentUser();

        // Owners already belong to their group, so they cannot request to join
        if (isCurrentUserOwner(group)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Group owners cannot request to join their own travel group"
            );
        }

        boolean alreadyJoined =
                travelGroupMemberRepository.existsByGroupAndMember(group, member);

        if (alreadyJoined) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Member already joined this travel group"
            );
        }

        // This count runs after the row lock, so two students joining at the same time
        // cannot both see the same last free seat.
        long memberCount = travelGroupMemberRepository.countByGroup(group);

        if (!group.hasAvailableSpots(memberCount)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Travel group is full"
            );
        }

        // Reuse an old request row when it exists, for example after rejection
        JoinRequest joinRequest = joinRequestRepository.findByGroupAndMember(group, member)
                .orElseGet(JoinRequest::new);

        if (joinRequest.getStatus() == JoinRequestStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Join request is already pending"
            );
        }

        joinRequest.setGroup(group);
        joinRequest.setMember(member);
        joinRequest.setStatus(JoinRequestStatus.PENDING);
        joinRequest.setRequestedAt(LocalDateTime.now());
        joinRequest.setRespondedAt(null);

        joinRequestRepository.save(joinRequest);
    }

    // Main join entry point used by both MVC and API
    @Transactional
    public void joinTravelGroup(UUID groupId) {
        // The same join button can either create a request or join directly.
        if (isJoinApprovalRequired()) {
            requestToJoinTravelGroup(groupId);
            return;
        }

        joinTravelGroupDirectly(groupId);
    }

    // Accepts a pending join request and adds that member to the group
    @Transactional
    public void acceptJoinRequest(Integer requestId) {
        JoinRequest joinRequest = getJoinRequestById(requestId);
        TravelGroup group = getTravelGroupByIdForUpdate(joinRequest.getGroup().getGroupId());

        ensureCurrentUserOwns(group);

        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending join requests can be accepted"
            );
        }

        boolean alreadyJoined =
                travelGroupMemberRepository.existsByGroupAndMember(group, joinRequest.getMember());

        // If the member joined another way, just mark the request as accepted
        if (alreadyJoined) {
            joinRequest.setStatus(JoinRequestStatus.ACCEPTED);
            joinRequest.setRespondedAt(LocalDateTime.now());
            joinRequestRepository.save(joinRequest);
            return;
        }

        // Capacity is checked while the group row is locked. If another owner/member
        // is accepting or joining at the same time, their transaction must wait.
        long memberCount = travelGroupMemberRepository.countByGroup(group);
        if (!group.hasAvailableSpots(memberCount)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Travel group is full"
            );
        }

        TravelGroupMember groupMember = new TravelGroupMember();
        groupMember.setGroup(group);
        groupMember.setMember(joinRequest.getMember());

        travelGroupMemberRepository.save(groupMember);

        joinRequest.setStatus(JoinRequestStatus.ACCEPTED);
        joinRequest.setRespondedAt(LocalDateTime.now());
        joinRequestRepository.save(joinRequest);
    }

    // Rejects a pending join request without adding the member
    @Transactional
    public void rejectJoinRequest(Integer requestId) {
        JoinRequest joinRequest = getJoinRequestById(requestId);

        ensureCurrentUserOwns(joinRequest.getGroup());

        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending join requests can be rejected"
            );
        }

        joinRequest.setStatus(JoinRequestStatus.REJECTED);
        joinRequest.setRespondedAt(LocalDateTime.now());
        joinRequestRepository.save(joinRequest);
    }

    // Lets a normal member leave a group. Owners must transfer first
    @Transactional
    public void leaveTravelGroup(UUID groupId) {

        TravelGroup group = getTravelGroupByIdForUpdate(groupId);

        Member member = currentUserService.getCurrentUser();

        if (isCurrentUserOwner(group)) {
            // Owners must transfer the group first, otherwise the group would have no responsible person
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    OWNER_CANNOT_LEAVE_MESSAGE
            );
        }

        TravelGroupMember membership = travelGroupMemberRepository
                .findByGroupAndMember(group, member)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Member is not part of this travel group"
                ));

        // Remove the user's membership row
        travelGroupMemberRepository.delete(membership);

        // If the last member leaves, remove the travel group to prevent empty inactive groups
        long remainingMembers = travelGroupMemberRepository.countByGroup(group);

        if (remainingMembers == 0) {
            deleteTravelGroupWithRelations(group);
        }
    }

    // Deletes an owner-only group after the Bootstrap confirmation popup
    @Transactional
    public void deleteOwnedTravelGroup(UUID groupId) {
        TravelGroup group = getTravelGroupByIdForUpdate(groupId);
        ensureCurrentUserOwns(group);

        // Owners with other members must transfer first, otherwise the group becomes unmanaged
        long memberCount = travelGroupMemberRepository.countByGroup(group);
        if (memberCount > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, OWNER_CANNOT_LEAVE_MESSAGE);
        }

        Member member = currentUserService.getCurrentUser();
        if (!travelGroupMemberRepository.existsByGroupAndMember(group, member)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Owner membership is missing for this travel group"
            );
        }

        deleteTravelGroupWithRelations(group);
    }

    // Moves ownership to another member who already joined the group
    @Transactional
    public TravelGroup transferOwnership(UUID groupId, UUID newOwnerId) {
        TravelGroup group = getTravelGroupByIdForUpdate(groupId);
        ensureCurrentUserOwns(group);

        if (newOwnerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a member to become the new owner");
        }

        if (group.getOwner() != null && group.getOwner().getUserId().equals(newOwnerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This member is already the owner");
        }

        Member newOwner = travelGroupMemberRepository.findAllByGroup(group)
                .stream()
                .map(TravelGroupMember::getMember)
                .filter(member -> member != null && member.getUserId().equals(newOwnerId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "New owner must already be a member of this travel group"
                ));

        // Only the owner field changes; the existing membership stays in place
        group.setOwner(newOwner);
        return travelGroupRepository.save(group);
    }

    // Finds current-user request group ids by status for list-page badges
    private Set<UUID> getJoinRequestGroupIds(List<TravelGroup> groups, JoinRequestStatus status) {
        if (groups.isEmpty()) {
            return Set.of();
        }

        Member member = currentUserService.getCurrentUser();

        return joinRequestRepository.findAllByMemberAndGroupIn(member, groups)
                .stream()
                .filter(joinRequest -> joinRequest.getStatus() == status)
                .map(joinRequest -> joinRequest.getGroup().getGroupId())
                .collect(Collectors.toSet());
    }

    // Loads a join request or returns 404 if it does not exist
    private JoinRequest getJoinRequestById(Integer requestId) {
        return joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Join request not found"
                ));
    }

    // Real-life seat protection:
    // lock the group row before checking capacity and inserting a member.
    // In PostgreSQL this becomes SELECT ... FOR UPDATE, so simultaneous joins wait their turn.
    private TravelGroup getTravelGroupByIdForUpdate(UUID groupId) {
        return travelGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel group not found"
                ));
    }

    // Adds the current member directly when approval mode is disabled
    private void joinTravelGroupDirectly(UUID groupId) {
        TravelGroup group = getTravelGroupByIdForUpdate(groupId);

        Member member = currentUserService.getCurrentUser();

        if (isCurrentUserOwner(group)) {
            // Owners already have a membership row from group creation
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Group owners are already part of their own travel group"
            );
        }

        boolean alreadyJoined = travelGroupMemberRepository.existsByGroupAndMember(group, member);
        if (alreadyJoined) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Member already joined this travel group"
            );
        }

        // The group was loaded with a database lock, so this capacity check is based
        // on the latest committed member count before we insert the new membership.
        long memberCount = travelGroupMemberRepository.countByGroup(group);
        if (!group.hasAvailableSpots(memberCount)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Travel group is full"
            );
        }

        TravelGroupMember groupMember = new TravelGroupMember();
        groupMember.setGroup(group);
        groupMember.setMember(member);

        travelGroupMemberRepository.save(groupMember);

        // If approval was turned off later, old requests should not stay pending
        joinRequestRepository.findByGroupAndMember(group, member)
                .ifPresent(joinRequest -> {
                    // Keep request history consistent with the direct membership
                    joinRequest.setStatus(JoinRequestStatus.ACCEPTED);
                    joinRequest.setRespondedAt(LocalDateTime.now());
                    joinRequestRepository.save(joinRequest);
                });
    }

    // Deletes a travel group and all records that depend on it
    private void deleteTravelGroupWithRelations(TravelGroup group) {
        // Remove join requests first so no request points to a deleted group.
        List<JoinRequest> joinRequests = joinRequestRepository.findAllByGroup(group);
        if (!joinRequests.isEmpty()) {
            joinRequestRepository.deleteAll(joinRequests);
        }

        // Remove memberships before deleting the group itself
        List<TravelGroupMember> memberships = travelGroupMemberRepository.findAllByGroup(group);
        if (!memberships.isEmpty()) {
            travelGroupMemberRepository.deleteAll(memberships);
        }

        // Detach and delete the group conversation to avoid an orphan chat
        Conversation conversation = group.getConversation();
        if (conversation != null) {
            group.setConversation(null);
            conversation.setTravelGroup(null);
            conversationRepository.delete(conversation);
        }

        travelGroupRepository.delete(group);
    }

    // Shared owner guard for all owner-only actions
    private void ensureCurrentUserOwns(TravelGroup group) {
        if (!isCurrentUserOwner(group)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the group owner can manage this travel group"
            );
        }
    }

    // Validates travel group fields for both create and edit
    private void validateTravelGroupDetails(Activity activity,
                                            Integer maxMembers,
                                            String location,
                                            String departureLocation,
                                            Double departureLatitude,
                                            Double departureLongitude,
                                            TransportMode mode,
                                            LocalDateTime departureTime,
                                            LocalDateTime estimatedArrivalTime) {
        // A group must always have at least one seat
        if (maxMembers == null || maxMembers <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max members must be positive");
        }

        // The route starting point is required because matching needs to know where members leave from
        if (normalizeRequiredRouteLocation(location, departureLocation) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departure location is required");
        }

        validateCoordinates(departureLatitude, departureLongitude, "Departure");

        // Transport mode is required for matching and for the UI cards
        if (mode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transport mode is required");
        }

        // Departure time is required so old or late travel groups cannot be created
        if (departureTime == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departure time is required");
        }

        // Compare at minute precision because the HTML input does not send seconds
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        if (departureTime.isBefore(now)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Departure time cannot be in the past"
            );
        }

        if (activity.getDate() != null && activity.getTime() != null) {
            LocalDateTime activityDateTime = LocalDateTime.of(activity.getDate(), activity.getTime());

            // Travel should start before or at the activity time, never after the event has started
            if (departureTime.isAfter(activityDateTime)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Departure time must be before the activity starts"
                    );
            }
        }

        if (estimatedArrivalTime != null) {
            if (estimatedArrivalTime.isBefore(departureTime)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Estimated arrival time cannot be before departure time"
                );
            }

            if (activity.getDate() != null && activity.getTime() != null) {
                LocalDateTime activityDateTime = LocalDateTime.of(activity.getDate(), activity.getTime());
                if (estimatedArrivalTime.isAfter(activityDateTime)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Estimated arrival time must be before the activity starts"
                    );
                }
            }
        }
    }

    private static String normalizeRequiredRouteLocation(String location, String departureLocation) {
        String normalizedDepartureLocation = normalizeOptionalText(departureLocation);
        if (normalizedDepartureLocation != null) {
            return normalizedDepartureLocation;
        }

        return normalizeOptionalText(location);
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static void validateCoordinates(Double latitude, Double longitude, String label) {
        if ((latitude == null) != (longitude == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    label + " latitude and longitude must be provided together"
            );
        }

        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " latitude must be between -90 and 90");
        }

        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " longitude must be between -180 and 180");
        }
    }
}
