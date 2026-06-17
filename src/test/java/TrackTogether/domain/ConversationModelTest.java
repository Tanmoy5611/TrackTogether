package TrackTogether.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationModelTest {

    @Test
    void legacyConversationWithoutTypeFallsBackToDirect() {
        Conversation conversation = new Conversation();

        assertEquals(ConversationType.DIRECT, conversation.getType());
    }

    @Test
    void legacyConversationWithTravelGroupFallsBackToTravelGroup() {
        Conversation conversation = new Conversation();
        conversation.setTravelGroup(new TravelGroup());

        assertEquals(ConversationType.TRAVEL_GROUP, conversation.getType());
    }

    @Test
    void explicitCustomGroupTypeAndTitleAreStored() {
        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.CUSTOM_GROUP);
        conversation.setTitle("Project team");

        assertEquals(ConversationType.CUSTOM_GROUP, conversation.getType());
        assertEquals("Project team", conversation.getTitle());
    }

    @Test
    void legacyMembershipWithoutRoleFallsBackToMember() {
        MemberConversation membership = new MemberConversation();

        assertEquals(MemberConversationRole.MEMBER, membership.getRole());
    }

    @Test
    void explicitOwnerRoleIsStored() {
        MemberConversation membership = new MemberConversation();
        membership.setRole(MemberConversationRole.OWNER);

        assertEquals(MemberConversationRole.OWNER, membership.getRole());
    }
}