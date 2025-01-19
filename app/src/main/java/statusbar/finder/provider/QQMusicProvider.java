package statusbar.finder.provider;

import android.media.MediaMetadata;
import android.util.Base64;
import android.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import statusbar.finder.provider.utils.HttpRequestUtil;
import statusbar.finder.provider.utils.LyricSearchUtil;

import java.io.IOException;
import java.util.Locale;

public class QQMusicProvider implements ILrcProvider {
    private static final String QM_BASE_URL = "https://c.y.qq.com/";
    private static final String QM_REFERER = "https://y.qq.com";
    private static final String QM_SEARCH_URL_FORMAT = QM_BASE_URL + "soso/fcgi-bin/client_search_cp?w=%s&format=json";
    private static final String QM_LRC_URL_FORMAT = QM_BASE_URL + "lyric/fcgi-bin/fcg_query_lyric_yqq.fcg?songmid=%s&format=json";

    @Override
    public LyricResult getLyric(MediaMetadata data, boolean requireTranslate) throws IOException {
        return getLyric(new ILrcProvider.MediaInfo(data), requireTranslate);
    }

    @Override
    public LyricResult getLyric(ILrcProvider.MediaInfo mediaInfo, boolean requireTranslate) throws IOException {
        String searchUrl = String.format(Locale.getDefault(), QM_SEARCH_URL_FORMAT, LyricSearchUtil.getSearchKey(mediaInfo));
        JSONObject searchResult;
        try {
            searchResult = HttpRequestUtil.getJsonResponse(searchUrl, QM_REFERER);
            if (searchResult != null && searchResult.getLong("code") == 0) {
                JSONArray array = searchResult.getJSONObject("data").getJSONObject("song").getJSONArray("list");
                Pair<String, MediaInfo> pair = getLrcUrl(array, mediaInfo);
                if (pair != null) {
                    JSONObject lrcJson = HttpRequestUtil.getJsonResponse(pair.first, QM_REFERER);
                    if (lrcJson == null) {
                        return null;
                    }
                    LyricResult result = new LyricResult();
                    result.mLyric = new String(Base64.decode(lrcJson.getString("lyric").getBytes(), Base64.DEFAULT));
                    result.mDistance = pair.second.getDistance();
                    result.mSource = "QQ";
                    result.mResultInfo = pair.second;
                    return result;
                } else {
                    return null;
                }
            }
        } catch (JSONException e) {
            e.fillInStackTrace();
            return null;
        }
        return null;
    }

    private static Pair<String, MediaInfo> getLrcUrl(JSONArray jsonArray, ILrcProvider.MediaInfo mediaInfo) throws JSONException {
        return getLrcUrl(jsonArray, mediaInfo.getTitle(), mediaInfo.getArtist(), mediaInfo.getAlbum());
    }

    private static Pair<String, MediaInfo> getLrcUrl(JSONArray jsonArray, String songTitle, String songArtist, String songAlbum) throws JSONException {
        String currentMID = "";
        long minDistance = 10000;
        String resultSoundName = null;
        String resultAlbumName = null;
        JSONArray resultSingers = null;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String soundName = jsonObject.getString("songname");
            String albumName = jsonObject.getString("albumname");
            JSONArray singers = jsonObject.getJSONArray("singer");
            long dis = LyricSearchUtil.calculateSongInfoDistance(songTitle, songArtist, songAlbum, soundName, LyricSearchUtil.parseArtists(singers, "name"), albumName);
            if (dis < minDistance) {
                minDistance = dis;
                resultSoundName = soundName;
                resultAlbumName = albumName;
                resultSingers = singers;
                currentMID = jsonObject.getString("songmid");
            }
        }
        if (currentMID.isEmpty()) {return null;}
        return new Pair<>(String.format(Locale.getDefault(), QM_LRC_URL_FORMAT, currentMID), new MediaInfo(resultSoundName, resultSingers.join(", "), resultAlbumName, minDistance, -1));
    }
}
