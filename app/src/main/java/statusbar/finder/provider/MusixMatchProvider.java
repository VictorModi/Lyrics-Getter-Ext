package statusbar.finder.provider;

import android.media.MediaMetadata;
import android.os.Build;
import android.util.Log;
import android.util.Pair;
import com.github.houbb.opencc4j.util.ZhConverterUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import statusbar.finder.misc.CheckLanguageUtil;
import statusbar.finder.provider.utils.HttpRequestUtil;
import statusbar.finder.provider.utils.LyricSearchUtil;
import statusbar.finder.provider.utils.UnicodeUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static statusbar.finder.provider.utils.LyricSearchUtil.extractLyric;

public class MusixMatchProvider implements ILrcProvider {

    private static final String MUSIXMATCH_BASE_URL = "https://apic.musixmatch.com/ws/1.1/";
    private static final String MUSIXMATCH_TOKEN_URL_FORMAT = MUSIXMATCH_BASE_URL + "token.get?guid=%s&app_id=android-player-v1.0&format=json";
    private static final String MUSIXMATCH_LRC_URL_FORMAT = MUSIXMATCH_BASE_URL + "macro.subtitles.get?tags=playing&subtitle_format=lrc&usertoken=%s&track_id=%d&app_id=android-player-v1.0&format=json";
    private static final String MUSIXMATCH_SEARCH_URL_FORMAT = MUSIXMATCH_BASE_URL + "macro.search?app_id=android-player-v1.0&usertoken=%s&q=%s";
    private static final String MUSIXMATCH_LRC_SEARCH_URL_FORMAT = MUSIXMATCH_BASE_URL + "macro.subtitles.get?tags=playing&subtitle_format=lrc&usertoken=%s&q_track=%s&q_artist=%s&q_album=%s&app_id=android-player-v1.0&format=json";
    private static final String MUSIXMATCH_TRANSLATED_LRC_URL_FORMAT = MUSIXMATCH_BASE_URL + "crowd.track.translations.get?usertoken=%s&translation_fields_set=minimal&selected_language=%s&track_id=%d&comment_format=text&part=user&format=json&app_id=android-player-v1.0&tags=playing";
    private static String musixMatchUserToken;

    @Override
    public LyricResult getLyric(MediaMetadata data, boolean requireTranslate) throws IOException {
        return getLyric(new ILrcProvider.MediaInfo(data), requireTranslate);
    }

    @Override
    public LyricResult getLyric(ILrcProvider.MediaInfo mediaInfo, boolean requireTranslate) throws IOException {
        if (musixMatchUserToken  == null) {
            musixMatchUserToken = getMusixMatchUserToken(getRandomId());
            if (musixMatchUserToken  == null) {
                return null;
            }
        }
        String searchUrl = String.format(Locale.getDefault(), MUSIXMATCH_SEARCH_URL_FORMAT, musixMatchUserToken , LyricSearchUtil.getSearchKey(mediaInfo));
        JSONObject searchResult;
        try{
            searchResult = HttpRequestUtil.getJsonResponse(searchUrl);
            Log.d("searchUrl", searchUrl);
            if (searchResult != null && searchResult.getJSONObject("message").getJSONObject("header").getLong("status_code") == 200) {
                JSONArray array = searchResult.getJSONObject("message").getJSONObject("body").getJSONObject("macro_result_list").getJSONArray("track_list");
                Pair<String, MediaInfo> pair = getLrcUrl(array, mediaInfo);
                Log.d("pair", String.valueOf(pair));
                LyricResult result = new LyricResult();
                long trackId = -1;
                JSONObject infoJson;
                if (pair != null) {
                    JSONObject lrcJson = HttpRequestUtil.getJsonResponse(pair.first);
                    if (lrcJson == null) {
                        return null;
                    }
                    result.mLyric = lrcJson.getJSONObject("message").getJSONObject("body").getJSONObject("macro_calls").getJSONObject("track.subtitles.get").getJSONObject("message").getJSONObject("body").getJSONArray("subtitle_list").getJSONObject(0).getJSONObject("subtitle").getString("subtitle_body");
                    infoJson = lrcJson.getJSONObject("message").getJSONObject("body").getJSONObject("macro_calls").getJSONObject("matcher.track.get").getJSONObject("message").getJSONObject("body").getJSONObject("track");
                    result.mDistance = pair.second.getDistance();
                    result.mResultInfo = pair.second;
                } else {
                    // 无法通过 id 寻找到歌词时
                    // 则尝试使用直接搜索歌词的方法
                    String lrcUrl;
                    String track = toSimpleURLEncode(mediaInfo.getTitle());
                    String artist = toSimpleURLEncode(mediaInfo.getArtist());
                    String album = toSimpleURLEncode(mediaInfo.getAlbum());
                    lrcUrl = String.format(Locale.getDefault(), MUSIXMATCH_LRC_SEARCH_URL_FORMAT,
                            musixMatchUserToken,
                            track,
                            artist,
                            album);
                    Log.d("lrcUrl", lrcUrl);
                    JSONObject lrcJson = HttpRequestUtil.getJsonResponse(lrcUrl);
                    if (lrcJson == null) {
                        return null;
                    }
                    JSONObject subTitleJson = lrcJson.getJSONObject("message").getJSONObject("body").getJSONObject("macro_calls").getJSONObject("track.subtitles.get").getJSONObject("message").getJSONObject("body").getJSONArray("subtitle_list").getJSONObject(0).getJSONObject("subtitle");
                    infoJson = lrcJson.getJSONObject("message").getJSONObject("body").getJSONObject("macro_calls").getJSONObject("matcher.track.get").getJSONObject("message").getJSONObject("body").getJSONObject("track");
                    result.mLyric = subTitleJson.getString("subtitle_body");
                }
                String soundName = infoJson.getString("track_name");
                String albumName = infoJson.getString("album_name");
                String artistName = infoJson.getString("artist_name");
                result.mDistance = result.mDistance == 0 ? LyricSearchUtil.calculateSongInfoDistance(mediaInfo, soundName, artistName, albumName) : result.mDistance;
                result.mSource = "MusixMatch";
                result.mResultInfo = new MediaInfo(soundName, artistName, albumName, -1, result.mDistance);
                if (requireTranslate){result.mTranslatedLyric = getTranslatedLyric(result.mLyric, trackId);}
                return result;
            }
        } catch (JSONException e) {
            e.fillInStackTrace();
            return null;
        }
        return null;
    }

//    private static Pair<String, Long> getLrcUrl(JSONArray jsonArray, MediaMetadata mediaMetadata) throws JSONException {
//        return getLrcUrl(jsonArray, mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE), mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM), mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
//    }

    private static Pair<String, MediaInfo> getLrcUrl(JSONArray jsonArray, ILrcProvider.MediaInfo mediaInfo) throws JSONException {
        return getLrcUrl(jsonArray, mediaInfo.getTitle(), mediaInfo.getArtist(), mediaInfo.getAlbum());
    }

    private static Pair<String, MediaInfo> getLrcUrl(JSONArray jsonArray, String songTitle, String songArtist, String songAlbum) throws JSONException {
        long currentID = -1;
        long minDistance = 100000;
        String resultSoundName = null;
        String resultArtistName = null;
        String resultAlbumName = null;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i).getJSONObject("track");
            String soundName = jsonObject.getString("track_name");
            String albumName = jsonObject.getString("album_name");
            String artistName = jsonObject.getString("artist_name");
            long dis = LyricSearchUtil.calculateSongInfoDistance(songTitle, songArtist, songAlbum, soundName, artistName, albumName);
            if (dis <= minDistance) {
                minDistance = dis;
                resultSoundName = soundName;
                resultArtistName = artistName;
                resultAlbumName = albumName;
                currentID = jsonObject.getLong("track_id");
            }
        }
        if (currentID == -1) {return null;}
        return new Pair<>(String.format(Locale.getDefault(), MUSIXMATCH_LRC_URL_FORMAT, musixMatchUserToken, currentID), new MediaInfo(resultSoundName, resultArtistName, resultAlbumName, minDistance, -1));
    }

    private static String toSimpleURLEncode(String input) {
        String result = input;
        if (input != null) {
            if (!CheckLanguageUtil.isJapanese(input)) {
                result = ZhConverterUtil.toSimple(result);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result = URLEncoder.encode(result, StandardCharsets.UTF_8);
            } else {
                try {
                    result = URLEncoder.encode(result, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    return null;
                }
            }
            return result;
        } else {
            return null;
        }
    }

    private String getTranslatedLyric(String lyricText, long trackId) {
        String[] languageOptions = {"zh", "tw"};

        for (String selectLang : languageOptions) {
            JSONArray translateLyricsList = getTranslationsList(trackId, selectLang);
            List<String> modifiedLyricText = new ArrayList<>();
            if (translateLyricsList != null) {
                for (String lyricLine : lyricText.split("\n")) {
                    boolean isMatched = false;
                    String[] lyric = extractLyric(lyricLine);
                    if (lyric == null) {
                        continue;
                    }
                    for (int curLyricLine = 0; curLyricLine < translateLyricsList.length(); curLyricLine ++) {
                        String snippet;
                        String description;
                        try {
                            JSONObject curLyricObject = translateLyricsList.getJSONObject(curLyricLine).getJSONObject("translation");
                            snippet = UnicodeUtil.unicodeStr2String(curLyricObject.getString("snippet"));
                            description = UnicodeUtil.unicodeStr2String(curLyricObject.getString("description"));
                        } catch (JSONException e) {
                            e.fillInStackTrace();
                            return null;
                        }

                        if (Objects.equals(lyric[1], snippet)) {
                            modifiedLyricText.add("[" + lyric[0] + "] " + description);
                            isMatched = true;
                            break;
                        }
                    }

                    if (!isMatched) {
                        modifiedLyricText.add("[" + lyric[0] + "]");
                    }
                }
                return String.join("\n", modifiedLyricText);
            }
        }
        return null;
    }


    private static JSONArray getTranslationsList(long trackId, String selectLang) { // 获取翻译歌词列表
        String transLyricURL = String.format(Locale.getDefault(), MUSIXMATCH_TRANSLATED_LRC_URL_FORMAT, musixMatchUserToken, selectLang, trackId);

        try {
            JSONObject transResult = HttpRequestUtil.getJsonResponse(transLyricURL);
            JSONObject header = transResult.getJSONObject("message").getJSONObject("header");
            int statusCode = header.getInt("status_code");

            if (statusCode != 200) {
                return null;
            }

            JSONArray translationsList = transResult.getJSONObject("message").getJSONObject("body").getJSONArray("translations_list");
            return translationsList.length() > 0 ? translationsList : null;
        } catch (JSONException | IOException e) {
            e.fillInStackTrace();
            return null;
        }
    }

    private String getMusixMatchUserToken(String guid) { // 获取 MusixMatch Token
        String result;
        try{
            // Form Google
            JSONObject tokenJson;
            String tokenURL = String.format(Locale.getDefault(), MUSIXMATCH_TOKEN_URL_FORMAT, guid);
            tokenJson = HttpRequestUtil.getJsonResponse(tokenURL);
            result = tokenJson.getJSONObject("message").getJSONObject("body").getString("user_token");
        } catch (JSONException | IOException e) {
            e.fillInStackTrace();
            return null;
        }
        return result;
    }

    private static String getRandomId() {
        long value = (long) (new Random().nextDouble() * Long.MAX_VALUE);
        String code = convertToBase36(value);
        code = code.replaceAll("[^a-zA-Z]", ""); // Keep only letters
        return code.substring(2, Math.min(8, code.length() - 2));
    }

    private static String convertToBase36(long value) {
        // https://github.com/WXRIW/Lyricify-Lyrics-Helper/blob/master/Lyricify.Lyrics.Helper/Providers/Web/MusixMatch/Api.cs
        final char[] chars = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
        int radix = chars.length;
        char[] result = new char[13];
        int index = 12;

        do {
            result[index--] = chars[(int) (value % radix)];
            value /= radix;
        } while (value > 0 && index >= 0);

        return new String(result, index + 1, 12 - index);
    }
}
