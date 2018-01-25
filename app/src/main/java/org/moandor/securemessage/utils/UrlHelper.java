package org.moandor.securemessage.utils;

public class UrlHelper {
    private static final String HOST = "45.32.40.29:2053";

    private static final String HTTP_HOST = "http://" + HOST + "/";
    private static final String API_BASE = HTTP_HOST + "api/";

    private static final String WEB_SOCKET_HOST = "ws://" + HOST + "/";

    public static final String API_ACCOUNTS = API_BASE + "accounts/";
    public static final String API_PRE_KEYS = API_BASE + "pre_keys/";

    public static final String WEB_SOCKET_MESSAGE = WEB_SOCKET_HOST + "message/";
}
