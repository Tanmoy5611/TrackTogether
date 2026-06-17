package TrackTogether.service;

import TrackTogether.domain.Message;

import java.util.List;
import java.util.UUID;

/*
 Holds a window of messages surrounding a reported message.
 {@code flaggedMessageId} identifies which entry in {@code messages} is the reported one.
 */
public record ChatContext(UUID flaggedMessageId, List<Message> messages) {
}
