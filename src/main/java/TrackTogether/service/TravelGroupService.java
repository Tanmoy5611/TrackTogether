package TrackTogether.service;

import TrackTogether.domain.*;
import TrackTogether.dto.TravelGroupPageView;
import TrackTogether.dto.TravelGroupActivityLogView;
import TrackTogether.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final MemberConversationRepository memberConversationRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final TravelGroupActivityLogRepository travelGroupActivityLogRepository;
    private final MemberRepository memberRepository;
    private final CurrentUserService currentUserService;
    private final SystemSettingsService systemSettingsService;
    private final NotificationService notificationService;
    private final ActivityPolicyService activityPolicyService;

    // Wires repositories and helper services used by travel group workflows
    public TravelGroupService(TravelGroupRepository travelGroupRepository,
                              ActivityRepository activityRepository,
                              ConversationRepository conversationRepository,
                              TravelGroupMemberRepository travelGroupMemberRepository,
                              MemberConversationRepository memberConversationRepository,
                              JoinRequestRepository joinRequestRepository,
                              TravelGroupActivityLogRepository travelGroupActivityLogRepository,
                              MemberRepository memberRepository,
                              CurrentUserService currentUserService,
                              SystemSettingsService systemSettingsService,
                              NotificationService notificationService,
                              ActivityPolicyService activityPolicyService) {
        this.travelGroupRepository = travelGroupRepository;
        this.activityRepository = activityRepository;
        this.conversationRepository = conversationRepository;
        this.travelGroupMemberRepository = travelGroupMemberRepository;
        this.memberConversationRepository = memberConversationRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.travelGroupActivityLogRepository = travelGroupActivityLogRepository;
        this.memberRepository = memberRepository;
        this.currentUserService = currentUserService;
        this.systemSettingsService = systemSettingsService;
        this.notificationService = notificationService;
        this.activityPolicyService = activityPolicyService;
    }

    // Returns all travel groups whose activities are visible to the current user
    public List<TravelGroup> getAllTravelGroups() {
        Member currentUser = currentUserService.getCurrentUser();
        return travelGroupRepository.findAll()
                .stream()
                .filter(group -> activityPolicyService.isVisibleTo(group.getActivity(), currentUser))
                .toList();
    }

    // Loads one travel group and checks that the current user may view its activity
    public TravelGroup getTravelGroupById(UUID groupId) {
        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel group not found"
                ));

        ensureActivityVisible(group);
        return group;
    }

    // Returns visible travel groups linked to one activity
    public List<TravelGroup> getTravelGroupsForActivity(UUID activityId) {
        Member currentUser = currentUserService.getCurrentUser();
        return travelGroupRepository.findAllByActivity_Id(activityId)
                .stream()
                .filter(group -> activityPolicyService.isVisibleTo(group.getActivity(), currentUser))
                .toList();
    }

    // Checks whether the current user has a membership in the group
    public boolean isCurrentUserMember(TravelGroup group) {
        Member member = currentUserService.getCurrentUser();
        return travelGroupMemberRepository.existsByGroupAndMember(group, member);
    }

    // Counts the current number of members in a group
    public long getMemberCount(TravelGroup group) {
        return travelGroupMemberRepository.countByGroup(group);
    }

    // Builds a group id to member count map for list rendering
    public Map<UUID, Long> getMemberCounts(List<TravelGroup> groups) {
        // Start every group at zero so empty groups still display a count
        Map<UUID, Long> memberCounts = groups.stream()
                .collect(Collectors.toMap(
                        TravelGroup::getGroupId,
                        group -> 0L,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        if (groups.isEmpty()) {
            return memberCounts;
        }

        // Replace the zero values with the grouped counts returned by the repository
        travelGroupMemberRepository.countMembersByGroupIn(groups)
                .forEach(row -> memberCounts.put((UUID) row[0], (Long) row[1]));

        return memberCounts;
    }

    // Builds the grouped travel group overview model for the list page
    public TravelGroupPageView buildTravelGroupsPage() {
        List<TravelGroup> groups = getAllTravelGroups();
        // Prepare shared lookup data once so the page does not repeat database work per card
        Set<UUID> joinedGroupIds = getJoinedGroupIds(groups);
        Set<UUID> ownedGroupIds = getOwnedGroupIds(groups);
        Map<UUID, Long> memberCounts = getMemberCounts(groups);
        Map<UUID, Long> pendingJoinRequestCounts = getPendingJoinRequestCounts(groups);

        return new TravelGroupPageView(
                groups,
                getCurrentUserTravelGroups(groups, joinedGroupIds, ownedGroupIds),
                getExploreTravelGroups(groups, joinedGroupIds, ownedGroupIds),
                joinedGroupIds,
                ownedGroupIds,
                getDeletableOwnedGroupIds(groups, joinedGroupIds, ownedGroupIds, memberCounts),
                memberCounts,
                isJoinApprovalRequired(),
                getOwnerCannotLeaveMessage(),
                getPendingJoinRequestGroupIds(groups),
                getRejectedJoinRequestGroupIds(groups),
                pendingJoinRequestCounts
        );
    }

    // Finds ids of groups joined by the current user
    public Set<UUID> getJoinedGroupIds(List<TravelGroup> groups) {
        if (groups.isEmpty()) {
            return Set.of();
        }

        Member member = currentUserService.getCurrentUser();

        // Let the repository find all matching ids in one query
        return travelGroupMemberRepository.findGroupIdsByMemberAndGroupIn(member, groups);
    }

    // Checks whether the current user owns the given group
    public boolean isCurrentUserOwner(TravelGroup group) {
        Member member = currentUserService.getCurrentUser();
        return group.getOwner() != null
                && group.getOwner().getUserId().equals(member.getUserId());
    }

    // Returns the standard message shown when an owner cannot leave
    public String getOwnerCannotLeaveMessage() {
        return OWNER_CANNOT_LEAVE_MESSAGE;
    }

    // Finds ids of groups owned by the current user
    public Set<UUID> getOwnedGroupIds(List<TravelGroup> groups) {
        Member member = currentUserService.getCurrentUser();

        return groups.stream()
                .filter(group -> group.getOwner() != null)
                .filter(group -> group.getOwner().getUserId().equals(member.getUserId()))
                .map(TravelGroup::getGroupId)
                .collect(Collectors.toSet());
    }

    // Finds owned groups that the current user may delete
    public Set<UUID> getDeletableOwnedGroupIds(List<TravelGroup> groups) {
        Set<UUID> joinedGroupIds = getJoinedGroupIds(groups);
        Set<UUID> ownedGroupIds = getOwnedGroupIds(groups);
        Map<UUID, Long> memberCounts = getMemberCounts(groups);

        return getDeletableOwnedGroupIds(groups, joinedGroupIds, ownedGroupIds, memberCounts);
    }

    // Finds deletable owned groups from preloaded membership data
    private Set<UUID> getDeletableOwnedGroupIds(List<TravelGroup> groups,
                                                Set<UUID> joinedGroupIds,
                                                Set<UUID> ownedGroupIds,
                                                Map<UUID, Long> memberCounts) {
        // Reuse the prepared ids and counts from the overview page
        return groups.stream()
                .filter(group -> ownedGroupIds.contains(group.getGroupId()))
                .filter(group -> joinedGroupIds.contains(group.getGroupId()))
                .filter(group -> memberCounts.getOrDefault(group.getGroupId(), 0L) == 1)
                .map(TravelGroup::getGroupId)
                .collect(Collectors.toSet());
    }

    // Filters groups that belong in the current user's joined or owned section
    private List<TravelGroup> getCurrentUserTravelGroups(List<TravelGroup> groups,
                                                         Set<UUID> joinedGroupIds,
                                                         Set<UUID> ownedGroupIds) {
        return groups.stream()
                .filter(group -> joinedGroupIds.contains(group.getGroupId()) || ownedGroupIds.contains(group.getGroupId()))
                .toList();
    }

    // Filters groups that belong in the explore section
    private List<TravelGroup> getExploreTravelGroups(List<TravelGroup> groups,
                                                     Set<UUID> joinedGroupIds,
                                                     Set<UUID> ownedGroupIds) {
        return groups.stream()
                .filter(group -> !joinedGroupIds.contains(group.getGroupId()) && !ownedGroupIds.contains(group.getGroupId()))
                .toList();
    }

    // Finds group ids where the current user has a pending join request
    public Set<UUID> getPendingJoinRequestGroupIds(List<TravelGroup> groups) {
        return getJoinRequestGroupIds(groups, JoinRequestStatus.PENDING);
    }

    // Finds group ids where the current user has a rejected join request
    public Set<UUID> getRejectedJoinRequestGroupIds(List<TravelGroup> groups) {
        return getJoinRequestGroupIds(groups, JoinRequestStatus.REJECTED);
    }

    // Counts pending join requests for each group
    public Map<UUID, Long> getPendingJoinRequestCounts(List<TravelGroup> groups) {
        // Keep zero values for groups without pending requests
        Map<UUID, Long> requestCounts = groups.stream()
                .collect(Collectors.toMap(
                        TravelGroup::getGroupId,
                        group -> 0L,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        if (groups.isEmpty()) {
            return requestCounts;
        }

        // Fill the map with real pending request totals from one grouped query
        joinRequestRepository.countByGroupInAndStatus(groups, JoinRequestStatus.PENDING)
                .forEach(row -> requestCounts.put((UUID) row[0], (Long) row[1]));

        return requestCounts;
    }

    // Returns the current user's join request status for one group
    public JoinRequestStatus getCurrentUserJoinRequestStatus(TravelGroup group) {
        Member member = currentUserService.getCurrentUser();
        return joinRequestRepository.findByGroupAndMember(group, member)
                .map(JoinRequest::getStatus)
                .orElse(null);
    }

    // Shows pending requests and invitations only to the owner who can accept or reject them
    public List<JoinRequest> getVisiblePendingRequests(TravelGroup group) {
        if (!isCurrentUserOwner(group)) {
            return List.of();
        }

        return joinRequestRepository.findAllByGroupAndStatusOrderByRequestedAtAsc(group, JoinRequestStatus.PENDING);
    }

    // Reads the system setting that controls whether joins require owner approval
    public boolean isJoinApprovalRequired() {
        return systemSettingsService.isTravelGroupJoinApprovalEnabled();
    }

    // Loads memberships for a group including member and shared location data
    public List<TravelGroupMember> getMembersForGroup(TravelGroup group) {
        return travelGroupMemberRepository.findAllByGroup(group);
    }

    // Returns newest activity log entries for the detail page
    public Page<TravelGroupActivityLogView> getActivityLogPage(TravelGroup group, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 25));

        // Keeping this paged so the log stays usable when the group gets busy
        return travelGroupActivityLogRepository.findAllByGroup(
                group,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).map(TravelGroupActivityLogView::from);
    }

    // count activity log
    public long countActivityLogEntries(TravelGroup group) {
        return travelGroupActivityLogRepository.countByGroup(group);
    }

    // Returns the logged-in member's row for prefilling the location sharing form
    public TravelGroupMember getCurrentUserMembership(TravelGroup group) {
        Member member = currentUserService.getCurrentUser();
        return travelGroupMemberRepository.findByGroupAndMember(group, member)
                .orElse(null);
    }

    // Only the current owner can edit a travel group
    public boolean canCurrentUserEditTravelGroup(TravelGroup group) {
        return isCurrentUserOwner(group);
    }

    // Allows deletion only when the current owner is the sole member
    public boolean canCurrentUserDeleteTravelGroup(TravelGroup group) {
        return isCurrentUserOwner(group)
                && isCurrentUserMember(group)
                && travelGroupMemberRepository.countByGroup(group) == 1;
    }

    // Lists members who can receive ownership from the current owner
    public List<TravelGroupMember> getOwnershipTransferCandidates(TravelGroup group) {
        if (group.getOwner() == null) {
            return List.of();
        }

        return travelGroupMemberRepository.findAllByGroup(group)
                .stream()
                .filter(membership -> membership.getMember() != null)
                .filter(membership -> !membership.getMember().getUserId().equals(group.getOwner().getUserId()))
                .toList();
    }

    // Deletes all travel groups and related records for an activity being removed
    @Transactional
    public void deleteTravelGroupsForActivity(UUID activityId) {
        List<TravelGroup> groups = travelGroupRepository.findAllByActivity_Id(activityId);

        for (TravelGroup group : groups) {
            deleteTravelGroupWithRelations(group);
        }
    }

    // Creates a travel group with its chat conversation and owner membership
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

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Activity not found"
                ));

        // Check visibility before creating a group for a hidden activity
        ensureActivityVisible(activity);

        // Reuse the same validation rules for create and edit
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

        String normalizedDepartureLocation = normalizeRequiredRouteLocation(location, departureLocation);
        String displayLocation = normalizeOptionalText(location);
        if (displayLocation == null) {
            displayLocation = normalizedDepartureLocation;
        }

        // Save the travel group first so the conversation and membership can point to it
        TravelGroup group = new TravelGroup(maxMembers, displayLocation, mode);
        group.setDepartureLocation(normalizedDepartureLocation);
        group.setDepartureLatitude(departureLatitude);
        group.setDepartureLongitude(departureLongitude);
        group.setDepartureTime(departureTime);
        group.setEstimatedArrivalTime(estimatedArrivalTime);
        group.setArrivalLatitude(activity.getLatitude());
        group.setArrivalLongitude(activity.getLongitude());

        Member owner = currentUserService.getCurrentUser();

        group.setActivity(activity);
        group.setOwner(owner);

        TravelGroup savedGroup = travelGroupRepository.save(group);

        // Every travel group gets its own chat conversation
        Conversation conversation = new Conversation();
        conversation.setTravelGroup(savedGroup);
        conversation.setCreatedAt(LocalDateTime.now());

        savedGroup.setConversation(conversation);

        Conversation savedConversation = conversationRepository.save(conversation);

        // The creator is added as the first member automatically
        TravelGroupMember ownerMembership = new TravelGroupMember();
        ownerMembership.setGroup(savedGroup);
        ownerMembership.setMember(owner);
        travelGroupMemberRepository.save(ownerMembership);

        addConversationMemberIfMissing(savedConversation, owner);
        recordActivity(savedGroup, owner, null, null, TravelGroupActivityType.CREATED);

        return savedGroup;
    }

    // Updates travel group route capacity and timing details for the owner
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
        // Only the owner can change route capacity and timing
        ensureCurrentUserOwns(group);

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
            // Prevent editing the capacity below the amount of students already joined
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Maximum members cannot be lower than the current member count"
            );
        }

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

        TravelGroup updatedGroup = travelGroupRepository.save(group);
        recordActivity(updatedGroup, currentUserService.getCurrentUser(), null, null, TravelGroupActivityType.UPDATED);

        return updatedGroup;
    }

    // Creates a pending join request for the current user
    @Transactional
    public void requestToJoinTravelGroup(UUID groupId) {
        TravelGroup group = getTravelGroupByIdForUpdate(groupId);

        Member member = currentUserService.getCurrentUser();

        // Owners already control the group so they should not create join requests
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

        // This count runs after the row lock so two students joining at the same time
        // cannot both see the same last free seat
        long memberCount = travelGroupMemberRepository.countByGroup(group);

        if (!group.hasAvailableSpots(memberCount)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Travel group is full"
            );
        }

        JoinRequest joinRequest = joinRequestRepository.findByGroupAndMember(group, member)
                .orElseGet(JoinRequest::new);

        if (joinRequest.getStatus() == JoinRequestStatus.PENDING) {
            // Avoid showing duplicate pending requests for the same student and group
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

        JoinRequest savedJoinRequest = joinRequestRepository.save(joinRequest);
        recordActivity(group, member, null, savedJoinRequest, TravelGroupActivityType.JOIN_REQUESTED);
        notificationService.notifyJoinRequestReceived(group.getOwner(), member, group);
    }

    // Joins directly or routes to the approval flow based on system settings
    @Transactional
    public void joinTravelGroup(UUID groupId) {
        if (isJoinApprovalRequired()) {
            requestToJoinTravelGroup(groupId);
            return;
        }

        joinTravelGroupDirectly(groupId);
    }

    // Accepts a pending request and adds the requester as a group member
    @Transactional
    public void acceptJoinRequest(Integer requestId) {
        JoinRequest joinRequest = getJoinRequestById(requestId);
        TravelGroup group = getTravelGroupByIdForUpdate(joinRequest.getGroup().getGroupId());

        // The locked group is checked again so only the current owner can accept it
        ensureCurrentUserOwns(group);

        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending join requests can be accepted"
            );
        }

        boolean alreadyJoined =
                travelGroupMemberRepository.existsByGroupAndMember(group, joinRequest.getMember());

        if (alreadyJoined) {
            // If the student was added another way, just finish the pending request cleanly
            addConversationMemberIfMissing(group.getConversation(), joinRequest.getMember());
            joinRequest.setStatus(JoinRequestStatus.ACCEPTED);
            joinRequest.setRespondedAt(LocalDateTime.now());
            joinRequestRepository.save(joinRequest);
            recordActivity(group, currentUserService.getCurrentUser(), joinRequest.getMember(), joinRequest, TravelGroupActivityType.JOIN_REQUEST_ACCEPTED);
            return;
        }

        // Capacity is checked while the group row is locked so concurrent joins must wait
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
        addConversationMemberIfMissing(group.getConversation(), joinRequest.getMember());
        recordActivity(group, joinRequest.getMember(), null, null, TravelGroupActivityType.JOINED);

        // Notify everyone only when this accepted request fills the final seat
        long newCount = travelGroupMemberRepository.countByGroup(group);
        if (!group.hasAvailableSpots(newCount)) {
            List<Member> allMembers = travelGroupMemberRepository.findAllByGroup(group)
                    .stream()
                    .map(TravelGroupMember::getMember)
                    .toList();
            notificationService.notifyGroupFull(allMembers, group);
        }

        joinRequest.setStatus(JoinRequestStatus.ACCEPTED);
        joinRequest.setRespondedAt(LocalDateTime.now());
        joinRequestRepository.save(joinRequest);
        recordActivity(group, currentUserService.getCurrentUser(), joinRequest.getMember(), joinRequest, TravelGroupActivityType.JOIN_REQUEST_ACCEPTED);
        notificationService.notifyJoinRequestAccepted(joinRequest.getMember(), group);
    }

    // Rejects a pending join request for a group owned by the current user
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
        recordActivity(joinRequest.getGroup(), currentUserService.getCurrentUser(), joinRequest.getMember(), joinRequest, TravelGroupActivityType.JOIN_REQUEST_REJECTED);
        notificationService.notifyJoinRequestRejected(joinRequest.getMember(), joinRequest.getGroup());
    }

    // Removes the current member from a group and deletes empty groups
    @Transactional
    public void leaveTravelGroup(UUID groupId) {

        TravelGroup group = getTravelGroupByIdForUpdate(groupId);

        Member member = currentUserService.getCurrentUser();

        if (isCurrentUserOwner(group)) {
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

        travelGroupMemberRepository.delete(membership);
        // Keep the chat members in sync with the travel group members
        removeConversationMemberIfPresent(group.getConversation(), member);
        recordActivity(group, member, null, null, TravelGroupActivityType.LEFT);

        if (group.getOwner() != null) {
            notificationService.notifyMemberLeft(group.getOwner(), member, group);
        }

        long remainingMembers = travelGroupMemberRepository.countByGroup(group);

        if (remainingMembers == 0) {
            // Clean up the group if nobody is left after the user leaves
            deleteTravelGroupWithRelations(group);
        }
    }

    // Deletes a group when the current owner is the only member
    @Transactional
    public void deleteOwnedTravelGroup(UUID groupId) {
        TravelGroup group = getTravelGroupByIdForUpdate(groupId);
        ensureCurrentUserOwns(group);

        // Owners must transfer ownership first when other students are still inside
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

    // Moves ownership from the current owner to another existing group member
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

        Member previousOwner = currentUserService.getCurrentUser();

        group.setOwner(newOwner);
        TravelGroup savedGroup = travelGroupRepository.save(group);
        recordActivity(savedGroup, previousOwner, newOwner, null, TravelGroupActivityType.OWNERSHIP_TRANSFERRED);

        return savedGroup;
    }

    // Lets a joined member share or update the location shown on the group detail page
    @Transactional
    public void updateCurrentMemberLocation(UUID groupId, String address, Double latitude, Double longitude) {
        TravelGroup group = getTravelGroupById(groupId);
        TravelGroupMember membership = getRequiredCurrentUserMembership(group);
        String normalizedAddress = normalizeOptionalText(address);

        if (normalizedAddress == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shared location address is required");
        }

        validateCoordinates(latitude, longitude, "Shared location");

        Location location = membership.getLocation();
        if (location == null) {
            // First share creates a Location row and later shares update the same row
            location = new Location();
            membership.setLocation(location);
        }

        location.setAddress(normalizedAddress);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setTimestamp(LocalDateTime.now());

        travelGroupMemberRepository.save(membership);
    }

    // Removes only the current user's shared location and keeps their membership
    @Transactional
    public void clearCurrentMemberLocation(UUID groupId) {
        TravelGroup group = getTravelGroupById(groupId);
        TravelGroupMember membership = getRequiredCurrentUserMembership(group);

        if (membership.getLocation() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No shared location to remove");
        }

        membership.setLocation(null);
        travelGroupMemberRepository.save(membership);
    }

    // Creates a pending invite for an existing student by reusing the join request flow
    @Transactional
    public void inviteMemberToTravelGroup(UUID groupId, String inviteeEmail) {
        TravelGroup group = getTravelGroupByIdForUpdate(groupId);
        Member inviter = currentUserService.getCurrentUser();

        if (!travelGroupMemberRepository.existsByGroupAndMember(group, inviter)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only joined members can invite someone");
        }

        String normalizedEmail = normalizeOptionalText(inviteeEmail);
        if (normalizedEmail == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite email is required");
        }

        Member invitee = memberRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No student found with that email"
                ));

        if (invitee.getUserId().equals(inviter.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot invite yourself");
        }

        // Do not create an invite for someone who is already in the group
        if (travelGroupMemberRepository.existsByGroupAndMember(group, invitee)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This student already joined the group");
        }

        // The group is locked here too so an invite cannot be created for a group that became full meanwhile
        long memberCount = travelGroupMemberRepository.countByGroup(group);
        if (!group.hasAvailableSpots(memberCount)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Travel group is full");
        }

        Optional<JoinRequest> existingJoinRequest = joinRequestRepository.findByGroupAndMember(group, invitee);
        if (existingJoinRequest.isPresent() && existingJoinRequest.get().getStatus() == JoinRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This student already has a pending invite");
        }

        // Reuse rejected or old requests so the unique group member constraint stays happy
        JoinRequest joinRequest = existingJoinRequest.orElseGet(JoinRequest::new);
        joinRequest.setGroup(group);
        joinRequest.setMember(invitee);
        joinRequest.setStatus(JoinRequestStatus.PENDING);
        joinRequest.setRequestedAt(LocalDateTime.now());
        joinRequest.setRespondedAt(null);

        JoinRequest savedJoinRequest = joinRequestRepository.save(joinRequest);
        recordActivity(group, invitee, null, savedJoinRequest, TravelGroupActivityType.JOIN_REQUESTED);
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

    // Loads a join request or returns a not found error
    private JoinRequest getJoinRequestById(Integer requestId) {
        return joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Join request not found"
                ));
    }

    // Protects seats by locking the group row before capacity checks and inserts
    // In PostgreSQL this becomes SELECT FOR UPDATE so simultaneous joins wait their turn
    private TravelGroup getTravelGroupByIdForUpdate(UUID groupId) {
        TravelGroup group = travelGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel group not found"
                ));
        ensureActivityVisible(group);
        return group;
    }

    // Shared guard for actions that require the current user to already be in the group
    private TravelGroupMember getRequiredCurrentUserMembership(TravelGroup group) {
        Member member = currentUserService.getCurrentUser();
        return travelGroupMemberRepository.findByGroupAndMember(group, member)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only joined members can do this"
                ));
    }

    // Adds the current member directly when approval mode is disabled
    private void joinTravelGroupDirectly(UUID groupId) {
        TravelGroup group = getTravelGroupByIdForUpdate(groupId);

        Member member = currentUserService.getCurrentUser();

        if (isCurrentUserOwner(group)) {
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

        // The group was loaded with a database lock so this capacity check uses the latest committed count
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
        addConversationMemberIfMissing(group.getConversation(), member);
        recordActivity(group, member, null, null, TravelGroupActivityType.JOINED);

        if (group.getOwner() != null) {
            notificationService.notifyMemberJoined(group.getOwner(), member, group);
        }

        long newCount = travelGroupMemberRepository.countByGroup(group);
        if (!group.hasAvailableSpots(newCount)) {
            List<Member> allMembers = travelGroupMemberRepository.findAllByGroup(group)
                    .stream()
                    .map(TravelGroupMember::getMember)
                    .toList();
            notificationService.notifyGroupFull(allMembers, group);
        }

        joinRequestRepository.findByGroupAndMember(group, member)
                .ifPresent(joinRequest -> {
                    joinRequest.setStatus(JoinRequestStatus.ACCEPTED);
                    joinRequest.setRespondedAt(LocalDateTime.now());
                    joinRequestRepository.save(joinRequest);
                });
    }

    // Removes join requests memberships conversation and the group record
    private void deleteTravelGroupWithRelations(TravelGroup group) {
        travelGroupActivityLogRepository.deleteAllByGroup(group);

        // Delete request rows before the group because they still point to it
        List<JoinRequest> joinRequests = joinRequestRepository.findAllByGroup(group);
        if (!joinRequests.isEmpty()) {
            joinRequestRepository.deleteAll(joinRequests);
        }

        // Remove memberships before deleting the travel group itself
        List<TravelGroupMember> memberships = travelGroupMemberRepository.findAllByGroup(group);
        if (!memberships.isEmpty()) {
            travelGroupMemberRepository.deleteAll(memberships);
        }

        Conversation conversation = group.getConversation();
        if (conversation != null) {
            // Clear chat members and break the two way link before deleting the conversation
            List<MemberConversation> conversationMembers = memberConversationRepository.findAllByConversation(conversation);
            if (!conversationMembers.isEmpty()) {
                memberConversationRepository.deleteAll(conversationMembers);
            }

            group.setConversation(null);
            conversation.setTravelGroup(null);
            conversationRepository.delete(conversation);
        }

        travelGroupRepository.delete(group);
    }

    // Ensures the current user owns the given group before owner-only actions
    private void ensureCurrentUserOwns(TravelGroup group) {
        if (!isCurrentUserOwner(group)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the group owner can manage this travel group"
            );
        }
    }

    private void addConversationMemberIfMissing(Conversation conversation, Member member) {
        if (conversation != null && !memberConversationRepository.existsByConversationAndMember(conversation, member)) {
            MemberConversation memberConversation = new MemberConversation();
            memberConversation.setConversation(conversation);
            memberConversation.setMember(member);
            memberConversationRepository.save(memberConversation);
        }
    }

    private void removeConversationMemberIfPresent(Conversation conversation, Member member) {
        if (conversation != null) {
            memberConversationRepository.findByConversationAndMember(conversation, member)
                    .ifPresent(memberConversationRepository::delete);
        }
    }

    private void recordActivity(TravelGroup group,
                                Member actor,
                                Member targetMember,
                                JoinRequest joinRequest,
                                TravelGroupActivityType type) {
        // Small history row for the notification page of this travel group
        TravelGroupActivityLog activityLog = new TravelGroupActivityLog();
        activityLog.setGroup(group);
        activityLog.setActor(actor);
        activityLog.setTargetMember(targetMember);
        activityLog.setJoinRequest(joinRequest);
        activityLog.setType(type);
        activityLog.setCreatedAt(LocalDateTime.now());
        travelGroupActivityLogRepository.save(activityLog);
    }

    // Ensures the activity behind a travel group is visible to the current user
    private void ensureActivityVisible(TravelGroup group) {
        ensureActivityVisible(group.getActivity());
    }

    // Ensures an activity is visible to the current user
    private void ensureActivityVisible(Activity activity) {
        if (!activityPolicyService.isVisibleTo(activity, currentUserService.getCurrentUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Activity is not public yet");
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
        if (maxMembers == null || maxMembers <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max members must be positive");
        }

        if (normalizeRequiredRouteLocation(location, departureLocation) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departure location is required");
        }

        validateCoordinates(departureLatitude, departureLongitude);

        if (mode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transport mode is required");
        }

        if (departureTime == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departure time is required");
        }

        // Compare with minutes precision because the form also submits minute based times
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        if (departureTime.isBefore(now)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Departure time cannot be in the past"
            );
        }

        if (activity.getDate() != null && activity.getTime() != null) {
            // A travel group should leave before the activity actually starts
            LocalDateTime activityDateTime = LocalDateTime.of(activity.getDate(), activity.getTime());

            if (departureTime.isAfter(activityDateTime)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Departure time must be before the activity starts"
                    );
            }
        }

        if (estimatedArrivalTime != null) {
            if (estimatedArrivalTime.isBefore(departureTime)) {
                // Arrival cannot be earlier than the chosen departure time
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Estimated arrival time cannot be before departure time"
                );
            }

            if (activity.getDate() != null && activity.getTime() != null) {
                LocalDateTime activityDateTime = LocalDateTime.of(activity.getDate(), activity.getTime());
                if (estimatedArrivalTime.isAfter(activityDateTime)) {
                    // The estimated arrival should still be before the event begins
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Estimated arrival time must be before the activity starts"
                    );
                }
            }
        }
    }

    // Chooses the departure location text that is required for route planning
    private static String normalizeRequiredRouteLocation(String location, String departureLocation) {
        String normalizedDepartureLocation = normalizeOptionalText(departureLocation);
        if (normalizedDepartureLocation != null) {
            return normalizedDepartureLocation;
        }

        return normalizeOptionalText(location);
    }

    // Trims optional text and converts blank values to null
    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    // Validates departure coordinates
    private static void validateCoordinates(Double latitude, Double longitude) {
        validateCoordinates(latitude, longitude, "Departure");
    }

    // Validates a latitude and longitude pair with a custom error label
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