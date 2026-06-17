package TrackTogether.webapi;

import TrackTogether.service.ChatRealtimeService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatRealtimeService chatRealtimeService;

    public ChatWebSocketHandler(ChatRealtimeService chatRealtimeService) {
        this.chatRealtimeService = chatRealtimeService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID conversationId = extractConversationId(session);
        if (conversationId == null || !chatRealtimeService.register(conversationId, session)) {
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        chatRealtimeService.handleClientMessage(session, message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        chatRealtimeService.unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        chatRealtimeService.unregister(session);
        session.close(CloseStatus.SERVER_ERROR);
    }

    private static UUID extractConversationId(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }

        String path = session.getUri().getPath();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == path.length() - 1) {
            return null;
        }

        try {
            return UUID.fromString(path.substring(lastSlash + 1));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
