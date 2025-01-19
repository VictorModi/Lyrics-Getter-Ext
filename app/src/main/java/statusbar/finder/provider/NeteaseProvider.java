package statusbar.finder.provider;

import android.media.MediaMetadata;
import android.util.Log;
import android.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import statusbar.finder.provider.utils.HttpRequestUtil;
import statusbar.finder.provider.utils.LyricSearchUtil;

import java.io.IOException;
import java.util.Locale;

public class NeteaseProvider implements ILrcProvider {

    private static final String NETEASE_BASE_URL = "http://music.163.com/api/";

    private static final String NETEASE_SEARCH_URL_FORMAT = NETEASE_BASE_URL + "search/get?s=%s&type=1&offset=0&limit=5";
    private static final String NETEASE_LRC_URL_FORMAT = NETEASE_BASE_URL + "song/lyric?os=pc&id=%d&lv=-1&kv=-1&tv=-1";

    @Override
    public LyricResult getLyric(MediaMetadata data, boolean requireTranslate) throws IOException {
        return getLyric(new ILrcProvider.MediaInfo(data), requireTranslate);
    }

    @Override
    public LyricResult getLyric(ILrcProvider.MediaInfo mediaInfo, boolean requireTranslate) throws IOException {
        String searchUrl = String.format(NETEASE_SEARCH_URL_FORMAT, LyricSearchUtil.getSearchKey(mediaInfo));
        Log.d("searchUrl", searchUrl);
        JSONObject searchResult;
        try {
            searchResult = HttpRequestUtil.getJsonResponse(searchUrl);
            if (searchResult != null && searchResult.getLong("code") == 200) {
                JSONArray array = searchResult.getJSONObject("result").getJSONArray("songs");
                Pair<String, MediaInfo> pair = getLrcUrl(array, mediaInfo);
                if (pair != null) {
                    JSONObject lrcJson = HttpRequestUtil.getJsonResponse(pair.first);
                    if (lrcJson == null) {
                        return null;
                    }
                    LyricResult result = new LyricResult();
                    result.mLyric = lrcJson.getJSONObject("lrc").getString("lyric");
                    try {
                        result.mTranslatedLyric = lrcJson.getJSONObject("tlyric").getString("lyric");
                    } catch (JSONException e) {
                        result.mTranslatedLyric = null;
                    }
                    result.mDistance = pair.second.getDistance();
                    result.mSource = "Netease";
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
        long currentID = -1;
        long minDistance = 10000;
        String resultSoundName = null;
        JSONArray resultArtists = null;
        String resultAlbumName = null;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String soundName = jsonObject.getString("name");
            JSONArray artists = jsonObject.getJSONArray("artists");
            String albumName = jsonObject.getJSONObject("album").getString("name");
            long dis = LyricSearchUtil.calculateSongInfoDistance(songTitle, songArtist, songAlbum, soundName, LyricSearchUtil.parseArtists(artists, "name"), albumName);
            if (dis < minDistance) {
                minDistance = dis;
                currentID = jsonObject.getLong("id");
                resultSoundName = soundName;
                resultArtists = artists;
                resultAlbumName = albumName;
            }
        }
        if (currentID == -1) {
            return null;
        }
        return new Pair<>(String.format(Locale.getDefault(), NETEASE_LRC_URL_FORMAT, currentID), new MediaInfo(resultSoundName, LyricSearchUtil.parseArtists(resultArtists, "name"), resultAlbumName, minDistance, -1));
    }
}
