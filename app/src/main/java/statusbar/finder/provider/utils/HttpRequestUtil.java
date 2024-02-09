package statusbar.finder.provider.utils;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.CookieHandler;
import java.net.CookieManager;

public class HttpRequestUtil {
    /*
       定义缺省 Cookie Manager, 用于记录 Cookie
     */
    static {
    // 创建 Cookie 管理器
        CookieHandler.setDefault(new CookieManager());
    }

    /**
     * 通过网络请求JSON结果，并返回JSON对象。
     *
     * @param url 请求的地址
     * @return  JSON结果
     * @throws IOException 网络异常
     * @throws JSONException JSON解析器异常
     */
    public static JSONObject getJsonResponse(String url) throws IOException, JSONException {
        return getJsonResponse(url, null);
    }

    public static JSONObject getJsonResponse(String url, String referer) throws IOException, JSONException {
        HttpURLConnection connection = null;
        do {
            if (connection == null) { // 测定 connection 为 null 时，视为第一次请求
                connection = getHttpConnection(url, referer);
            } else { // 否则非第一次请求，说明有重定向
                connection = getHttpConnection(connection.getHeaderField("Location"), referer);
            }
        } while (connection.getResponseCode() == 301 || connection.getResponseCode() == 302); // 处理重定向, 但目前只有天杀的 MusixMatch 需要我做这一步
        return handleStreamToJson(connection);
    }

    private static HttpURLConnection getHttpConnection(String url, String referer) throws IOException {
        URL httpUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) httpUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:77.0) Gecko/20100101 Firefox/77.0");
        if (!TextUtils.isEmpty(referer)) {
            connection.setRequestProperty("Referer", referer);
        }
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.connect();
        return connection;
    }

    private static JSONObject handleStreamToJson(HttpURLConnection connection) throws IOException {
        InputStream in = connection.getInputStream();
        byte[] data = readStream(in);
        // Log.d("data", new String(data));
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(new String(data));
        } catch (JSONException e) {
            e.fillInStackTrace();
            return null;
        }
        in.close();
        connection.disconnect();
        return jsonObject;
    }

    public static byte[] readStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            bout.write(buffer, 0, len);
        }
        bout.close();
        inputStream.close();

        return bout.toByteArray();
    }
}
