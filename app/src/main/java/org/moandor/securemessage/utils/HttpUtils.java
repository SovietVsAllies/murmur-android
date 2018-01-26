package org.moandor.securemessage.utils;

import org.json.JSONException;
import org.json.JSONObject;
import org.moandor.securemessage.GlobalContext;
import org.moandor.securemessage.R;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

public class HttpUtils {
    private static final int CONNECT_TIMEOUT = 10 * 1000;
    private static final int READ_TIMEOUT = 10 * 1000;

    public static String doGet(String urlAddress, HttpParams params) throws NotifyException {
        try {
            if (params != null) {
                urlAddress += '?' + params.encodeUrl();
            }
            URL url = new URL(urlAddress);
            Proxy proxy = getProxy();
            HttpURLConnection connection;
            if (proxy != null) {
                connection = (HttpURLConnection) url.openConnection(proxy);
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }
            connection.setRequestMethod("GET");
            connection.setDoOutput(false);
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Charset", "UTF-8");
            connection.connect();
            return handleResponse(connection);
        } catch (IOException e) {
            e.printStackTrace();
            throw new NotifyException(GlobalContext.getInstance().getString(
                    R.string.network_error));
        }
    }

    public static String doPost(String urlAddress, String data) throws NotifyException {
        try {
            URL url = new URL(urlAddress);
            Proxy proxy = getProxy();
            HttpURLConnection connection;
            if (proxy != null) {
                connection = (HttpURLConnection) url.openConnection(proxy);
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.connect();
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.write(data.getBytes());
            out.flush();
            out.close();
            return handleResponse(connection);
        } catch (IOException e) {
            e.printStackTrace();
            throw new NotifyException(GlobalContext.getInstance().getString(
                    R.string.network_error));
        }
    }

    private static Proxy getProxy() {
        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        if (!TextUtils.isEmpty(proxyHost) && !TextUtils.isEmpty(proxyPort)) {
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.valueOf(proxyPort)));
        } else {
            return null;
        }
    }

    private static String readResult(HttpURLConnection connection) throws NotifyException {
        try (BufferedReader buffer = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = buffer.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            throw new NotifyException(GlobalContext.getInstance().getString(
                    R.string.network_error));
        }
    }

    private static String handleResponse(HttpURLConnection connection) throws NotifyException {
        int responseCode;
        try {
            responseCode = connection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            connection.disconnect();
            throw new NotifyException(GlobalContext.getInstance().getString(
                    R.string.network_error));
        }
        if (responseCode != HttpURLConnection.HTTP_OK) {
            handleError(connection, responseCode);
        }
        return readResult(connection);
    }

    private static void handleError(HttpURLConnection connection, int httpCode)
            throws NotifyException {
        String result = readError(connection);
        try {
            JSONObject json = new JSONObject(result);
            String err = json.optString("message");
            int code = json.optInt("code");
            throw new NotifyException(GlobalContext.getInstance().getString(
                    R.string.error_code_message, code, err));
        } catch (JSONException e) {
            e.printStackTrace();
            throw new NotifyException(GlobalContext.getInstance().getString(
                    R.string.unknown_error, "HTTP " + httpCode + '\n' + result));
        }
    }

    private static String readError(HttpURLConnection connection) throws NotifyException {
        InputStream in = connection.getErrorStream();
        if (in == null) {
            throw new NotifyException(GlobalContext.getInstance().getString(
                    R.string.network_error));
        }
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(in))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = buffer.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            throw new NotifyException(GlobalContext.getInstance().getString(
                    R.string.network_error));
        }
    }
}
