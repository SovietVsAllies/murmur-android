package org.moandor.securemessage.utils;

import org.moandor.securemessage.GlobalContext;

public class NotifyException extends Exception {
    public NotifyException(String message) {
        super(message);
    }

    public NotifyException(int messageRes) {
        super(GlobalContext.getInstance().getString(messageRes));
    }
}
