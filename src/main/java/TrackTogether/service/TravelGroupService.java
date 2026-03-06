package TrackTogether.service;

import TrackTogether.domain.Activity;
import TrackTogether.domain.Conversation;
import TrackTogether.domain.TransportMode;
import TrackTogether.domain.TravelGroup;
import TrackTogether.repository.ActivityRepository;
import TrackTogether.repository.ConversationRepository;
import TrackTogether.repository.TravelGroupRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TravelGroupService {

    // Repository used to store and retrieve TravelGroup entities
    private final TravelGroupRepository travelGroupRepository;

    // Repository used to store and retrieve TravelGroup entities
    private final ActivityRepository activityRepository;

    private final ConversationRepository conversationRepository;

    public TravelGroupService(TravelGroupRepository travelGroupRepository,
                              ActivityRepository activityRepository,
                              ConversationRepository conversationRepository) {
        this.travelGroupRepository = travelGroupRepository;
        this.activityRepository = activityRepository;
        this.conversationRepository = conversationRepository;
    }

    // Creates a new TravelGroup for a given activity
    public TravelGroup createTravelGroup(UUID activityId,
                                         Integer maxMembers,
                                         String location,
                                         TransportMode mode) {

        // Retrieve the activity from the database
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

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

        conversationRepository.save(conversation);

        return savedGroup;
    }
}