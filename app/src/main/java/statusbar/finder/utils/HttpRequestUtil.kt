package statusbar.finder.utils

/**
 * LyricGetterExt - statusbar.finder.utils
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/21 09:56
 */

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.CookieHandler
import java.net.CookieManager
import java.net.HttpURLConnection
import java.net.URL

object HttpRequestUtil {
    // 初始化默认 Cookie 管理器
    init {
        CookieHandler.setDefault(CookieManager())
    }

    /**
     * 获取指定URL的JSON响应。
     *
     * @param url 请求的URL
     * @return 返回从URL获取的JSON响应的JSONObject对象
     * @throws IOException 如果发生I/O错误
     */
    fun getJsonResponse(url: String): JSONObject? {
        return getJsonResponse(url, null)
    }

    /**
     * 获取指定URL的JSON响应。
     *
     * @param url 请求的URL
     * @param referer 请求的引用页
     * @return 返回从URL获取的JSON响应的JSONObject对象
     * @throws IOException 如果发生I/O错误
     */
    fun getJsonResponse(url: String, referer: String?): JSONObject? {
        var connection: HttpURLConnection = createHttpURLConnection(url, referer)

        do {
            val requestUrl = connection.getHeaderField("Location") ?: url
            connection = createHttpURLConnection(requestUrl, referer)
        } while (connection.responseCode == 301 || connection.responseCode == 302)

        return convertStreamToJSONObject(connection)
    }

    /**
     * 创建并返回一个HTTP连接对象。
     *
     * @param url 请求的URL
     * @param referer 请求的引用页
     * @return 返回一个HttpURLConnection对象
     * @throws IOException 如果发生I/O错误
     */
    private fun createHttpURLConnection(url: String, referer: String?): HttpURLConnection {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:77.0) Gecko/20100101 Firefox/77.0")
            referer?.let {
                setRequestProperty("Referer", it)
            }
            connectTimeout = 5000
            readTimeout = 5000
            connect()
        }
        return connection
    }

    /**
     * 读取输入流并转换为JSON对象
     */

    private fun convertStreamToJSONObject(connection: HttpURLConnection): JSONObject? {
        return connection.inputStream.use { inputStream ->
            val data = inputStream.readStream()
            try {
                JSONObject(String(data))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * 扩展函数：将 InputStream 读取为 ByteArray
     */
    private fun InputStream.readStream(): ByteArray {
        return use { input ->
            ByteArrayOutputStream().use { output ->
                val buffer = ByteArray(1024)
                var length: Int
                while (input.read(buffer).also { length = it } != -1) {
                    output.write(buffer, 0, length)
                }
                output.toByteArray()
            }
        }
    }
}

