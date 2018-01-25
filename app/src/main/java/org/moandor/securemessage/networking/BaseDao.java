package org.moandor.securemessage.networking;

import org.moandor.securemessage.utils.NotifyException;

public interface BaseDao<T> {
    T execute() throws NotifyException;
}
