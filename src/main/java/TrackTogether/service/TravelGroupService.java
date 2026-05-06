package TrackTogether.service;

import TrackTogether.domain.*;
import TrackTogether.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TravelGroupService {

    // Repository used to store and retrieve TravelGroup entities
    private final TravelGroupRepository travelGroupRepository;
    // Repository used to store and retrieve TravelGroup entities
    private final ActivityRepository activityRepository;
    private final ConversationRepository conversationRepository;
    private final TravelGroupMemberRepository travelGroupMemberRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final CurrentUserService currentUserService;

    public TravelGroupService(TravelGroupRepository travelGroupRepository,
                              ActivityRepository activityRepository,
                              ConversationRepository conversationRepository,
                              TravelGroupMemberRepository travelGroupMemberRepository,
                              JoinRequestRepository joinRequestRepository,
                              CurrentUserService currentUserService) {
        this.travelGroupRepository = travelGroupRepository;
        this.activityRepository = activityRepository;
        this.conversationRepository = conversationRepository;
        this.travelGroupMemberRepository = travelGroupMemberRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.currentUserService = currentUserService;
    }

    // Get all travel groups
    public List<TravelGroup> getAllTravelGroups() {
        return travelGroupRepository.findAll();
    }

    public TravelGroup getTravelGroupById(UUID groupId) {
        return travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel group not found"
                ));
    }

    public List<TravelGroup> getTravelGroupsForActivity(UUID activityId) {
        return travelGroupRepository.findAllByActivity_Id(activityId);
    }

    public boolean isCurrentUserMember(TravelGroup group) {
        Member member = currentUserService.getCurrentUser();
        return travelGroupMemberRepository.existsByGroupAndMember(group, member);
    }

    public long getMemberCount(TravelGroup group) {
        return travelGroupMemberRepository.countByGroup(group);
    }

    public Map<UUID, Long> getMemberCounts(List<TravelGroup> groups) {
        return groups.stream()
                .collect(Collectors.toMap(
                        TravelGroup::getGroupId,
                        this::getMemberCount
                ));
    }

    public Set<UUID> getJoinedGroupIds(List<TravelGroup> groups) {
        Member member = currentUserService.getCurrentUser();

        return groups.stream()
                .filter(group -> travelGroupMemberRepository.existsByGroupAndMember(group, member))
                .map(TravelGroup::getGroupId)
                .collect(Collectors.toSet());
    }

    public boolean isCurrentUserOwner(TravelGroup group) {
        Member member = currentUserService.getCurrentUser();
        return group.getOwner() != null
                && group.getOwner().getUserId().equals(member.getUserId());
    }

    public Set<UUID> getOwnedGroupIds(List<TravelGroup> groups) {
        Member member = currentUserService.getCurrentUser();

        return groups.stream()
                .filter(group -> group.getOwner() != null)
                .filter(group -> group.getOwner().getUserId().equals(member.getUserId()))
                .map(TravelGroup::getGroupId)
                .collect(Collectors.toSet());
    }

    public Set<UUID> getPendingJoinRequestGroupIds(List<TravelGroup> groups) {
        return getJoinRequestGroupIds(groups, JoinRequestStatus.PENDING);
    }

    public Set<UUID> getRejectedJoinRequestGroupIds(List<TravelGroup> groups) {
        return getJoinRequestGroupIds(groups, JoinRequestStatus.REJECTED);
    }

    public Map<UUID, Long> getPendingJoinRequestCounts(List<TravelGroup> groups) {
        return groups.stream()
                .collect(Collectors.toMap(
                        TravelGroup::getGroupId,
                        group -> joinRequestRepository.countByGroupAndStatus(group, JoinRequestStatus.PENDING)
                ));
    }

    public JoinRequestStatus getCurrentUserJoinRequestStatus(TravelGroup group) {
        Member member = currentUserService.getCurrentUser();
        return joinRequestRepository.findByGroupAndMember(group, member)
                .map(JoinRequest::getStatus)
                .orElse(null);
    }

    public List<JoinRequest> getPendingJoinRequestsForGroup(TravelGroup group) {
        ensureCurrentUserOwns(group);
        return joinRequestRepository.findAllByGroupAndStatusOrderByRequestedAtAsc(group, JoinRequestStatus.PENDING);
    }

    public List<TravelGroupMember> getMembersForGroup(TravelGroup group) {
        return travelGroupMemberRepository.findAllByGroup(group);
    }

    @Transactional
    public void deleteTravelGroupsForActivity(UUID activityId) {
        List<TravelGroup> groups = travelGroupRepository.findAllByActivity_Id(activityId);

        for (TravelGroup group : groups) {
            List<JoinRequest> joinRequests = joinRequestRepository.findAllByGroup(group);
            if (!joinRequests.isEmpty()) {
                joinRequestRepository.deleteAll(joinRequests);
            }

            List<TravelGroupMember> memberships = travelGroupMemberRepository.findAllByGroup(group);
            if (!memberships.isEmpty()) {
                travelGroupMemberRepository.deleteAll(memberships);
            }

            Conversation conversation = group.getConversation();
            if (conversation != null) {
                conversation.setTravelGroup(null);
                group.setConversation(null);
                conversationRepository.save(conversation);
            }

            travelGroupRepository.delete(group);
        }
    }

    // Creates a new TravelGroup for a given activity
    @Transactional
    public TravelGroup createTravelGroup(UUID activityId,
                                         Integer maxMembers,
                                         String location,
                                         TransportMode mode) {

        // Retrieve the activity from the database
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Activity not found"
                ));

        // Validate input first
        if (maxMembers == null || maxMembers <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max members must be positive");
        }

        if (location == null || location.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location is required");
        }

        if (mode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transport mode is required");
        }

        // Create a new TravelGroup with the provided information
        TravelGroup group = new TravelGroup(maxMembers, location, mode);
        Member owner = currentUserService.getCurrentUser();

        // Link the group to the activity and remember who created it
        group.setActivity(activity);
        group.setOwner(owner);

        // Save the group first in the database
        TravelGroup savedGroup = travelGroupRepository.save(group);

        // Create conversation
        Conversation conversation = new Conversation();
        conversation.setTravelGroup(savedGroup);
        conversation.setCreatedAt(LocalDateTime.now());

        savedGroup.setConversation(conversation);

        conversationRepository.save(conversation);

        TravelGroupMember ownerMembership = new TravelGroupMember();
        ownerMembership.setGroup(savedGroup);
        ownerMembership.setMember(owner);
        travelGroupMemberRepository.save(ownerMembership);

        return savedGroup;
    }

    @Transactional
    public void requestToJoinTravelGroup(UUID groupId) {
        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel group not found"
                ));

        Member member = currentUserService.getCurrentUser();

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

    @Transactional
    public void joinTravelGroup(UUID groupId) {
        requestToJoinTravelGroup(groupId);
    }

    @Transactional
    public void acceptJoinRequest(Integer requestId) {
        JoinRequest joinRequest = getJoinRequestById(requestId);
        TravelGroup group = joinRequest.getGroup();

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
            joinRequest.setStatus(JoinRequestStatus.ACCEPTED);
            joinRequest.setRespondedAt(LocalDateTime.now());
            joinRequestRepository.save(joinRequest);
            return;
        }

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

    @Transactional
    public void leaveTravelGroup(UUID groupId) {

        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel group not found"
                ));

        Member member = currentUserService.getCurrentUser();

        if (isCurrentUserOwner(group)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Group owners cannot leave their own travel group"
            );
        }

        TravelGroupMember membership = travelGroupMemberRepository
                .findByGroupAndMember(group, member)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Member is not part of this travel group"
                ));

        // remove membership
        travelGroupMemberRepository.delete(membership);

        // check if group is empty
        // If the last member leaves, remove the travel group to prevent empty or inactive groups
        long remainingMembers = travelGroupMemberRepository.countByGroup(group);

        if (remainingMembers == 0) {
            List<JoinRequest> joinRequests = joinRequestRepository.findAllByGroup(group);
            if (!joinRequests.isEmpty()) {
                joinRequestRepository.deleteAll(joinRequests);
            }

            Conversation conversation = group.getConversation();
            if (conversation != null) {
                conversation.setTravelGroup(null);
                group.setConversation(null);
                conversationRepository.save(conversation);
            }

            travelGroupRepository.delete(group);
        }
    }

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

    private JoinRequest getJoinRequestById(Integer requestId) {
        return joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Join request not found"
                ));
    }

    private void ensureCurrentUserOwns(TravelGroup group) {
        if (!isCurrentUserOwner(group)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the group creator can manage join requests"
            );
        }
    }
}