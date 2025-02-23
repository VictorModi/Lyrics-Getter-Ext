package statusbar.finder.utils;

import android.text.TextUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpRequestUtil {
    /*
       定义缺省 Cookie Manager, 用于记录 Cookie
     */
    static {
    // 创建 Cookie 管理器
        CookieHandler.setDefault(new CookieManager());
    }

    /**
     * 获取指定URL的JSON响应。
     *
     * @param url 请求的URL
     * @return  返回从URL获取的JSON响应的JSONObject对象
     * @throws IOException 如果发生I/O错误
     * @throws JSONException 如果解析JSON响应时发生错误
     */
    public static JSONObject getJsonResponse(String url) throws IOException, JSONException {
        return getJsonResponse(url, null);
    }

    /**
     * 获取指定URL的JSON响应。
     *
     * @param url 请求的URL
     * @param referer 请求的引用页
     * @return 返回从URL获取的JSON响应的JSONObject对象
     * @throws IOException 如果发生I/O错误
     * @throws JSONException 如果解析JSON响应时发生错误
     */
    public static JSONObject getJsonResponse(String url, String referer) throws IOException, JSONException {
        // 初始化连接对象
        HttpURLConnection connection = null;

        // 重定向处理：如果响应码为301或302，则继续重定向直到得到非重定向响应
        do {
            // 获取HTTP连接对象
            connection = createHttpURLConnection(connection == null ? url : connection.getHeaderField("Location"), referer);
        } while (connection.getResponseCode() == 301 || connection.getResponseCode() == 302);

        // 将响应流转换为JSONObject对象并返回
        return convertStreamToJSONObject(connection);
    }


    /**
     * 创建并返回一个HTTP连接对象。
     *
     * @param url 请求的URL
     * @param referer 请求的引用页
     * @return 返回一个HTTPURLConnection对象
     * @throws IOException 如果发生I/O错误
     */
    private static HttpURLConnection createHttpURLConnection(String url, String referer) throws IOException {
        // 创建URL对象
        URL httpUrl = new URL(url);
        // 打开连接
        HttpURLConnection connection = (HttpURLConnection) httpUrl.openConnection();
        // 设置请求方法为GET
        connection.setRequestMethod("GET");
        // 设置User-Agent
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:77.0) Gecko/20100101 Firefox/77.0");
        // 设置Referer
        if (!TextUtils.isEmpty(referer)) {
            connection.setRequestProperty("Referer", referer);
        }
        // 设置连接超时时间
        connection.setConnectTimeout(5000);
        // 设置读取超时时间
        connection.setReadTimeout(5000);
        // 连接
        connection.connect();
        return connection;
    }


    private static JSONObject convertStreamToJSONObject(HttpURLConnection connection) throws IOException {
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
