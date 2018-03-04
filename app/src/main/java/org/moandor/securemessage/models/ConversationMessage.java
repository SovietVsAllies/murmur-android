package org.moandor.securemessage.models;

import java.util.Date;
import java.util.UUID;

public class ConversationMessage {
    public ConversationMessage(
            UUID conversationId,
            Date dateSent,
            Date dateReceived,
            String content,
            Direction direction) {
        mConversationId = conversationId;
        mDateSent = dateSent;
        mDateReceived = dateReceived;
        mContent = content;
        mDirection = direction;
    }

    public UUID getConversationId() {
        return mConversationId;
    }

    public Date getDateSent() {
        return mDateSent;
    }

    public Date getDateReceived() {
        return mDateReceived;
    }

    public String getContent() {
        return mContent;
    }

    public Direction getDirection() {
        return mDirection;
    }

    public enum Direction {
        IN, OUT
    }

    private UUID mConversationId;
    private Date mDateSent;
    private Date mDateReceived;
    private String mContent;
    private Direction mDirection;
}
