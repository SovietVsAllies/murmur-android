package org.moandor.securemessage.models;

import java.util.UUID;

public class Conversation {
    public Conversation(UUID id) {
        mId = id;
    }

    public UUID getId() {
        return mId;
    }

    private UUID mId;
}
