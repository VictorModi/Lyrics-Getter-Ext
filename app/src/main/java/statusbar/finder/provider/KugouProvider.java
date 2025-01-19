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

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

public class KugouProvider implements ILrcProvider {

    private static final String KUGOU_BASE_URL = "https://lyrics.kugou.com/";
    private static final String KUGOU_SEARCH_URL_FORMAT = KUGOU_BASE_URL + "search?ver=1&man=yes&client=pc&keyword=%s&duration=%d";
    private static final String KUGOU_LRC_URL_FORMAT = KUGOU_BASE_URL + "download?ver=1&client=pc&id=%d&accesskey=%s&fmt=lrc&charset=utf8";

    @Override
    public LyricResult getLyric(MediaMetadata data, boolean requireTranslate) throws IOException {
        return getLyric(new ILrcProvider.MediaInfo(data), requireTranslate);
    }

    @Override
    public LyricResult getLyric(ILrcProvider.MediaInfo mediaInfo, boolean requireTranslate) throws IOException {
        String searchUrl = String.format(Locale.getDefault(), KUGOU_SEARCH_URL_FORMAT, LyricSearchUtil.getSearchKey(mediaInfo), mediaInfo.getDuration());
        JSONObject searchResult;
        try {
            searchResult = HttpRequestUtil.getJsonResponse(searchUrl);
            if (searchResult != null && searchResult.getLong("status") == 200) {
                JSONArray array = searchResult.getJSONArray("candidates");
                Pair<String, MediaInfo> pair = getLrcUrl(array, mediaInfo);
                if(pair != null){
                    JSONObject lrcJson = HttpRequestUtil.getJsonResponse(pair.first);
                    if (lrcJson == null) {
                        return null;
                    }
                    LyricResult result = new LyricResult();
                    result.mLyric = unescapeHtml4(new String(Base64.decode(lrcJson.getString("content").getBytes(), Base64.DEFAULT)));
                    result.mDistance = pair.second.getDistance();
                    result.mSource = "Kugou";
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

//    private static Pair<String, Long> getLrcUrl(JSONArray jsonArray, MediaMetadata mediaMetadata) throws JSONException {
//        return getLrcUrl(jsonArray, mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE), mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM), mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
//    }

    private static Pair<String, MediaInfo> getLrcUrl(JSONArray jsonArray, ILrcProvider.MediaInfo mediaInfo) throws JSONException {
        return getLrcUrl(jsonArray, mediaInfo.getTitle(), mediaInfo.getArtist(), mediaInfo.getAlbum());
    }

    private static Pair<String, MediaInfo> getLrcUrl(JSONArray jsonArray, String songTitle, String songArtist, String songAlbum) throws JSONException {
        String currentAccessKey = "";
        long minDistance = 10000;
        long currentId = -1;
        String resultSoundName = null;
        String resultArtist = null;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String soundName = jsonObject.getString("song");
            String artist = jsonObject.getString("singer");
            long dis = LyricSearchUtil.calculateSongInfoDistance(songTitle, songArtist, songAlbum, soundName, artist, null);
            if (dis < minDistance) {
                minDistance = dis;
                currentId = jsonObject.getLong("id");
                currentAccessKey = jsonObject.getString("accesskey");
                resultSoundName = soundName;
                resultArtist = artist;
            }
        }
        if (currentId == -1) {
            return null;
        }
        return new Pair<>(String.format(Locale.getDefault(), KUGOU_LRC_URL_FORMAT, currentId, currentAccessKey), new MediaInfo(resultSoundName, resultArtist, null, minDistance, -1));
    }
}
