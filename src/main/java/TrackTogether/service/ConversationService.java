package TrackTogether.service;

import TrackTogether.domain.Conversation;
import TrackTogether.domain.ConversationType;
import TrackTogether.domain.Member;
import TrackTogether.domain.MemberConversation;
import TrackTogether.domain.MemberConversationRole;
import TrackTogether.domain.TravelGroup;
import TrackTogether.domain.TravelGroupMember;
import TrackTogether.repository.ConversationRepository;
import TrackTogether.repository.MemberConversationRepository;
import TrackTogether.repository.MemberRepository;
import TrackTogether.repository.MessageRepository;
import TrackTogether.repository.ReportHistoryRepository;
import TrackTogether.repository.ReportRepository;
import TrackTogether.repository.TravelGroupMemberRepository;
import TrackTogether.repository.TravelGroupRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final CurrentUserService currentUserService;
    private final MemberRepository memberRepository;
    private final MemberConversationRepository memberConversationRepository;
    private final MessageRepository messageRepository;
    private final ReportHistoryRepository reportHistoryRepository;
    private final ReportRepository reportRepository;
    private final TravelGroupRepository travelGroupRepository;
    private final TravelGroupMemberRepository travelGroupMemberRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               CurrentUserService currentUserService,
                               MemberRepository memberRepository,
                               MemberConversationRepository memberConversationRepository,
                               MessageRepository messageRepository,
                               ReportHistoryRepository reportHistoryRepository,
                               ReportRepository reportRepository,
                               TravelGroupRepository travelGroupRepository,
                               TravelGroupMemberRepository travelGroupMemberRepository) {
        this.conversationRepository = conversationRepository;
        this.currentUserService = currentUserService;
        this.memberRepository = memberRepository;
        this.memberConversationRepository = memberConversationRepository;
        this.messageRepository = messageRepository;
        this.reportHistoryRepository = reportHistoryRepository;
        this.reportRepository = reportRepository;
        this.travelGroupRepository = travelGroupRepository;
        this.travelGroupMemberRepository = travelGroupMemberRepository;
    }

    public List<Conversation> findAllForCurrentMember() {
        Member currentMember = currentUserService.getCurrentUser();
        return conversationRepository.findByMembers_Member_UserId(currentMember.getUserId());
    }

    public List<Conversation> findDirectConversationsForCurrentMember() {
        Member currentMember = currentUserService.getCurrentUser();
        return conversationRepository.findByMembers_Member_UserIdAndTravelGroupIsNull(currentMember.getUserId()).stream()
                .filter(conversation -> conversation.getType() == ConversationType.DIRECT)
                .toList();
    }

    @Transactional
    public List<Conversation> findGroupConversationsForCurrentMember() {
        Member currentMember = currentUserService.getCurrentUser();
        List<Conversation> travelGroupConversations = travelGroupMemberRepository.findAllByMember(currentMember)
                .stream()
                .map(TravelGroupMember::getGroup)
                .map(group -> conversationRepository.findByTravelGroup_GroupId(group.getGroupId())
                        .orElseGet(() -> {
                            Conversation conversation = new Conversation();
                            conversation.setTravelGroup(group);
                            conversation.setType(ConversationType.TRAVEL_GROUP);
                            conversation.setCreatedAt(LocalDateTime.now());
                            return conversationRepository.save(conversation);
                        }))
                .peek(conversation -> addMemberToConversationIfMissing(conversation, currentMember))
                .toList();

        List<Conversation> customGroupConversations = conversationRepository
                .findByMembers_Member_UserIdAndType(currentMember.getUserId(), ConversationType.CUSTOM_GROUP);

        return java.util.stream.Stream.concat(travelGroupConversations.stream(), customGroupConversations.stream())
                .distinct()
                .toList();
    }

    @Transactional
    public Conversation createCustomGroupConversation(String title, List<UUID> memberIds) {
        String normalizedTitle = normalizeCustomGroupTitle(title);
        Set<UUID> selectedMemberIds = memberIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(memberIds);

        Member currentMember = currentUserService.getCurrentUser();
        selectedMemberIds.remove(currentMember.getUserId());
        if (selectedMemberIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose at least one member");
        }

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.CUSTOM_GROUP);
        conversation.setTitle(normalizedTitle);
        conversation.setCreatedAt(LocalDateTime.now());

        Conversation savedConversation = conversationRepository.save(conversation);
        addMemberToConversationIfMissing(savedConversation, currentMember, MemberConversationRole.OWNER);

        selectedMemberIds.stream()
                .map(memberId -> memberRepository.findById(memberId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found")))
                .forEach(member -> addMemberToConversationIfMissing(
                        savedConversation,
                        member,
                        MemberConversationRole.MEMBER
                ));

        return savedConversation;
    }

    @Transactional(readOnly = true)
    public Conversation findGroupConversationForCurrentMember(UUID conversationId) {
        Conversation conversation = findConversationForCurrentMember(conversationId);
        if (conversation.getType() == ConversationType.DIRECT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This conversation is not a group chat");
        }

        return conversation;
    }

    @Transactional(readOnly = true)
    public List<MemberConversation> findGroupMembersForCurrentMember(UUID conversationId) {
        Conversation conversation = findGroupConversationForCurrentMember(conversationId);
        return memberConversationRepository.findAllByConversation(conversation);
    }

    @Transactional(readOnly = true)
    public List<Member> findAvailableMembersForCustomGroup(UUID conversationId) {
        Conversation conversation = findCustomGroupForCurrentMember(conversationId);
        List<UUID> existingMemberIds = memberConversationRepository.findAllByConversation(conversation)
                .stream()
                .map(MemberConversation::getMember)
                .map(Member::getUserId)
                .toList();

        return memberRepository.findAll().stream()
                .filter(member -> !existingMemberIds.contains(member.getUserId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isCurrentMemberCustomGroupOwner(UUID conversationId) {
        Conversation conversation = findCustomGroupForCurrentMember(conversationId);
        return getCurrentMembership(conversation).getRole() == MemberConversationRole.OWNER;
    }

    @Transactional
    public void renameCustomGroup(UUID conversationId, String title) {
        Conversation conversation = findCustomGroupOwnedByCurrentMember(conversationId);
        conversation.setTitle(normalizeCustomGroupTitle(title));
        conversationRepository.save(conversation);
    }

    @Transactional
    public void addMemberToCustomGroup(UUID conversationId, UUID memberId) {
        Conversation conversation = findCustomGroupOwnedByCurrentMember(conversationId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        addMemberToConversationIfMissing(conversation, member);
    }

    @Transactional
    public void removeMemberFromCustomGroup(UUID conversationId, UUID memberId) {
        Conversation conversation = findCustomGroupOwnedByCurrentMember(conversationId);
        Member currentMember = currentUserService.getCurrentUser();
        if (currentMember.getUserId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use leave group to remove yourself");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        MemberConversation membership = memberConversationRepository.findByConversationAndMember(conversation, member)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member is not part of this group"));
        ensureOwnerRemains(conversation, membership);
        memberConversationRepository.delete(membership);
    }

    @Transactional
    public void updateCustomGroupMemberRole(UUID conversationId,
                                            UUID memberId,
                                            MemberConversationRole role) {
        Conversation conversation = findCustomGroupOwnedByCurrentMember(conversationId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        MemberConversation membership = memberConversationRepository.findByConversationAndMember(conversation, member)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member is not part of this group"));

        if (membership.getRole() == role) {
            return;
        }
        if (membership.getRole() == MemberConversationRole.OWNER && role == MemberConversationRole.MEMBER) {
            ensureOwnerRemains(conversation, membership);
        }

        membership.setRole(role);
        memberConversationRepository.save(membership);
    }

    @Transactional
    public void leaveCustomGroup(UUID conversationId) {
        Conversation conversation = findCustomGroupForCurrentMember(conversationId);
        MemberConversation membership = getCurrentMembership(conversation);
        ensureOwnerRemains(conversation, membership);
        memberConversationRepository.delete(membership);
    }

    @Transactional
    public void deleteCustomGroupIfOnlyMember(UUID conversationId) {
        Conversation conversation = findCustomGroupForCurrentMember(conversationId);
        List<MemberConversation> memberships = memberConversationRepository.findAllByConversation(conversation);
        if (memberships.size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can only delete this group when you are the last member");
        }

        reportHistoryRepository.deleteAllByReport_Message_Conversation(conversation);
        reportRepository.deleteAllByMessage_Conversation(conversation);
        messageRepository.deleteAllByConversation(conversation);
        memberConversationRepository.deleteAll(memberships);
        conversationRepository.delete(conversation);
    }

    public Conversation findOrCreateConversation(UUID memberId) {
        Member currentMember = currentUserService.getCurrentUser();
        Member selectedMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        return conversationRepository.findConversationByMembers(currentMember, selectedMember)
                .orElseGet(() -> {
                    Conversation conversation = new Conversation();
                    conversation.setType(ConversationType.DIRECT);
                    conversation.setCreatedAt(LocalDateTime.now());
                    Conversation savedConversation = conversationRepository.save(conversation);

                    MemberConversation currentMemberConversation = new MemberConversation();
                    currentMemberConversation.setConversation(savedConversation);
                    currentMemberConversation.setMember(currentMember);
                    currentMemberConversation.setRole(MemberConversationRole.MEMBER);
                    memberConversationRepository.save(currentMemberConversation);

                    MemberConversation selectedMemberConversation = new MemberConversation();
                    selectedMemberConversation.setConversation(savedConversation);
                    selectedMemberConversation.setMember(selectedMember);
                    selectedMemberConversation.setRole(MemberConversationRole.MEMBER);
                    memberConversationRepository.save(selectedMemberConversation);

                    return savedConversation;
                });
    }

    @Transactional
    public Conversation openTravelGroupConversation(UUID groupId) {
        Member currentMember = currentUserService.getCurrentUser();
        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel group not found"));

        if (!travelGroupMemberRepository.existsByGroupAndMember(group, currentMember)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Join this travel group before opening its chat");
        }

        Conversation conversation = conversationRepository.findByTravelGroup_GroupId(groupId)
                .orElseGet(() -> {
                    Conversation newConversation = new Conversation();
                    newConversation.setTravelGroup(group);
                    newConversation.setType(ConversationType.TRAVEL_GROUP);
                    newConversation.setCreatedAt(LocalDateTime.now());
                    return conversationRepository.save(newConversation);
                });

        addMemberToConversationIfMissing(conversation, currentMember);
        return conversation;
    }

    @Transactional(readOnly = true)
    public String getConversationDisplayName(Conversation conversation) {
        if (conversation.getType() == ConversationType.CUSTOM_GROUP) {
            return conversation.getTitle();
        }

        if (conversation.getTravelGroup() != null) {
            TravelGroup travelGroup = conversation.getTravelGroup();
            if (travelGroup.getActivity() != null && travelGroup.getActivity().getName() != null) {
                return travelGroup.getActivity().getName();
            }
            if (travelGroup.getLocation() != null && !travelGroup.getLocation().isBlank()) {
                return travelGroup.getLocation();
            }
            return "Group chat";
        }

        return getOtherMemberName(conversation);
    }

    public void addMemberToConversationIfMissing(Conversation conversation, Member member) {
        addMemberToConversationIfMissing(conversation, member, MemberConversationRole.MEMBER);
    }

    public void addMemberToConversationIfMissing(Conversation conversation,
                                                 Member member,
                                                 MemberConversationRole role) {
        if (!memberConversationRepository.existsByConversationAndMember(conversation, member)) {
            MemberConversation memberConversation = new MemberConversation();
            memberConversation.setConversation(conversation);
            memberConversation.setMember(member);
            memberConversation.setRole(role);
            memberConversationRepository.save(memberConversation);
        }
    }

    @Transactional(readOnly = true)
    public String getOtherMemberName(Conversation conversation) {
        Member currentMember = currentUserService.getCurrentUser();

        Conversation loadedConversation = findById(conversation.getConversationId());

        return loadedConversation.getMembers()
                .stream()
                .map(MemberConversation::getMember)
                .filter(member -> !member.getUserId().equals(currentMember.getUserId()))
                .map(Member::getName)
                .findFirst()
                .orElse("Conversation");
    }

    @Transactional(readOnly = true)
    public UUID getOtherMemberId(Conversation conversation) {
        Member currentMember = currentUserService.getCurrentUser();

        Conversation loadedConversation = findById(conversation.getConversationId());

        return loadedConversation.getMembers()
                .stream()
                .filter(mc -> !mc.getMember().getUserId().equals(currentMember.getUserId()))
                .map(mc -> mc.getMember().getUserId())
                .findFirst()
                .orElse(null);
    }

    private Conversation findById(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
    }

    private Conversation findConversationForCurrentMember(UUID conversationId) {
        Conversation conversation = findById(conversationId);
        getCurrentMembership(conversation);
        return conversation;
    }

    private Conversation findCustomGroupForCurrentMember(UUID conversationId) {
        Conversation conversation = findConversationForCurrentMember(conversationId);
        if (conversation.getType() != ConversationType.CUSTOM_GROUP) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This action is only available for custom group chats");
        }

        return conversation;
    }

    private Conversation findCustomGroupOwnedByCurrentMember(UUID conversationId) {
        Conversation conversation = findCustomGroupForCurrentMember(conversationId);
        if (getCurrentMembership(conversation).getRole() != MemberConversationRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group owner can manage this group");
        }

        return conversation;
    }

    private MemberConversation getCurrentMembership(Conversation conversation) {
        Member currentMember = currentUserService.getCurrentUser();
        return memberConversationRepository.findByConversationAndMember(conversation, currentMember)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this conversation"));
    }

    private void ensureOwnerRemains(Conversation conversation, MemberConversation membership) {
        if (membership.getRole() == MemberConversationRole.OWNER && countOwners(conversation) <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assign another owner before removing the last owner");
        }
    }

    private long countOwners(Conversation conversation) {
        return memberConversationRepository.findAllByConversation(conversation)
                .stream()
                .filter(membership -> membership.getRole() == MemberConversationRole.OWNER)
                .count();
    }

    private static String normalizeCustomGroupTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group chat name is required");
        }

        return title.trim();
    }
}