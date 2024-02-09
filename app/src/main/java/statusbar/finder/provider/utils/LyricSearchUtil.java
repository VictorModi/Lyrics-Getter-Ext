package statusbar.finder.provider.utils;

import android.media.MediaMetadata;
import android.os.Build;
import android.text.TextUtils;

import com.github.houbb.opencc4j.util.ZhConverterUtil;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import statusbar.finder.misc.checkStringLang;
import statusbar.finder.provider.ILrcProvider;

public class LyricSearchUtil {

    private static final Pattern LyricContentPattern = Pattern.compile("(\\[\\d\\d:\\d\\d\\.\\d{0,3}]|\\[\\d\\d:\\d\\d])[^\\r\\n]");

    private static void convertIfNecessary(String input) {
        if (ZhConverterUtil.isTraditional(input) && !(checkStringLang.isJapanese(input))) {
            input = ZhConverterUtil.toSimple(input);
        }
    }

    public static String getSearchKey(String title, String artist, String album) {
        String ret;

        convertIfNecessary(title);
        convertIfNecessary(artist);
        convertIfNecessary(album);

        if (!TextUtils.isEmpty(artist)) {
            ret = artist + "-" + title;
        } else if (!TextUtils.isEmpty(album)) {
            ret = album + "-" + title;
        } else {
            ret = title;
        }
        if (!TextUtils.isEmpty(ret)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return URLEncoder.encode(ret, StandardCharsets.UTF_8);
            }
            try {
                return URLEncoder.encode(ret, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return ret;
            }
        } else {
            return ret;
        }
    }

    public static String getSearchKey(MediaMetadata metadata) {
        return getSearchKey(metadata.getString(MediaMetadata.METADATA_KEY_TITLE), metadata.getString(MediaMetadata.METADATA_KEY_ALBUM), metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
    }

    public static String getSearchKey(ILrcProvider.MediaInfo mediaInfo) {
        return getSearchKey(mediaInfo.getTitle(), mediaInfo.getArtist(), mediaInfo.getAlbum());
    }

    public static String parseArtists(JSONArray jsonArray, String key) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < jsonArray.length(); i++) {
                stringBuilder.append(jsonArray.getJSONObject(i).getString(key));
                if (i < jsonArray.length() - 1) stringBuilder.append('/');
            }
            return stringBuilder.toString();
        } catch (JSONException e) {
            e.fillInStackTrace();
        }
        return null;
    }

    public static long calculateSongInfoDistance(String realTitle, String realArtist, String realAlbum, String title, String artist, String album) {
        if (ZhConverterUtil.isTraditional(realTitle)) {
            if (!(checkStringLang.isJapanese(realTitle)))
                realTitle = ZhConverterUtil.toSimple(realTitle);
        }
        if (ZhConverterUtil.isTraditional(realArtist)) {
            if (!(checkStringLang.isJapanese(realArtist)))
                realArtist = ZhConverterUtil.toSimple(realArtist);
        }
        if (ZhConverterUtil.isTraditional(realAlbum)) {
            if (!(checkStringLang.isJapanese(realAlbum)))
                realAlbum = ZhConverterUtil.toSimple(realAlbum);
        }

        if (!realTitle.contains(title) && !title.contains(realTitle) || TextUtils.isEmpty(title)) {
            return 10000;
        }
        long res = levenshtein(title, realTitle) * 100L;
        res += levenshtein(artist, realArtist) * 10L;
        res += levenshtein(album, realAlbum);
        return res;
    }

    public static long calculateSongInfoDistance(ILrcProvider.MediaInfo mediaInfo, String title, String artist, String album) {
        return calculateSongInfoDistance(mediaInfo.getTitle(), mediaInfo.getArtist(), mediaInfo.getAlbum(), title, artist, album);
    }

    public static long calculateSongInfoDistance(MediaMetadata metadata, String title, String artist, String album) {
        return calculateSongInfoDistance(
                metadata.getString(MediaMetadata.METADATA_KEY_TITLE),
                metadata.getString(MediaMetadata.METADATA_KEY_ARTIST),
                metadata.getString(MediaMetadata.METADATA_KEY_ALBUM),
                title,
                artist,
                album
        );
    }

    public static int levenshtein(CharSequence a, CharSequence b) {
        if (TextUtils.isEmpty(a)) {
            return TextUtils.isEmpty(b) ? 0 : b.length();
        } else if (TextUtils.isEmpty(b)) {
            return TextUtils.isEmpty(a) ? 0 : a.length();
        }
        final int lenA = a.length(), lenB = b.length();
        int[][] dp = new int[lenA+1][lenB+1];
        int flag = 0;
        for (int i = 0; i <= lenA; i++) {
            for (int j = 0; j <= lenB; j++) dp[i][j] = lenA + lenB;
        }
        for(int i=1; i <= lenA; i++) dp[i][0] = i;
        for(int j=1; j <= lenB; j++) dp[0][j] = j;
        for (int i = 1; i <= lenA; i++) {
            for (int j = 1; j <= lenB; j++) {
                if (a.charAt(i-1) == b.charAt(j-1)) {
                    flag = 0;
                } else {
                    flag = 1;
                }
                dp[i][j] = Math.min(dp[i-1][j-1] + flag, Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1));
            }
        }
        return dp[lenA][lenB];
    }

    public static boolean isLyricContent(String content) {
        if (TextUtils.isEmpty(content)) return false;
        return LyricContentPattern.matcher(content).find();
    }

}
