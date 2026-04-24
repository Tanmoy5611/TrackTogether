package TrackTogether.service;

import TrackTogether.domain.*;
import TrackTogether.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TravelGroupService {

    // Repository used to store and retrieve TravelGroup entities
    private final TravelGroupRepository travelGroupRepository;
    // Repository used to store and retrieve TravelGroup entities
    private final ActivityRepository activityRepository;
    private final ConversationRepository conversationRepository;
    private final TravelGroupMemberRepository travelGroupMemberRepository;
    private final CurrentUserService currentUserService;

    public TravelGroupService(TravelGroupRepository travelGroupRepository,
                              ActivityRepository activityRepository,
                              ConversationRepository conversationRepository,
                              TravelGroupMemberRepository travelGroupMemberRepository,
                              CurrentUserService currentUserService) {
        this.travelGroupRepository = travelGroupRepository;
        this.activityRepository = activityRepository;
        this.conversationRepository = conversationRepository;
        this.travelGroupMemberRepository = travelGroupMemberRepository;
        this.currentUserService = currentUserService;
    }

    // Get all travel groups
    public List<TravelGroup> getAllTravelGroups() {
        return travelGroupRepository.findAll();
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


        // Link the group to the activity
        group.setActivity(activity);

        // Save the group first in the database
        TravelGroup savedGroup = travelGroupRepository.save(group);

        // Create conversation
        Conversation conversation = new Conversation();
        conversation.setTravelGroup(savedGroup);
        conversation.setCreatedAt(LocalDateTime.now());

        savedGroup.setConversation(conversation);

        conversationRepository.save(conversation);

        return savedGroup;
    }

    @Transactional
    public void joinTravelGroup(UUID groupId) {

        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel group not found"
                ));

        Member member = currentUserService.getCurrentUser();

        // Prevent duplicate joins
        boolean alreadyJoined =
                travelGroupMemberRepository.existsByGroupAndMember(group, member);

        if (alreadyJoined) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Member already joined this travel group"
            );
        }

        // Validate available spots
        long memberCount = travelGroupMemberRepository.countByGroup(group);

        if (!group.hasAvailableSpots(memberCount)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Travel group is full"
            );
        }

        // Create membership
        TravelGroupMember groupMember = new TravelGroupMember();
        groupMember.setGroup(group);
        groupMember.setMember(member);

        travelGroupMemberRepository.save(groupMember);
    }

    @Transactional
    public void leaveTravelGroup(UUID groupId) {

        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel group not found"
                ));

        Member member = currentUserService.getCurrentUser();

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
            Conversation conversation = group.getConversation();
            if (conversation != null) {
                conversation.setTravelGroup(null);
                group.setConversation(null);
                conversationRepository.save(conversation);
            }

            travelGroupRepository.delete(group);
        }
    }
}