package statusbar.finder;

// https://github.com/VictorModi/Lyrics-Getter-Ext/issues/19
// https://github.com/tomakino/CSLyric/blob/main/api.md

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.JsonWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringWriter;

/**
 * statusbar.finder.CSLyricHelper 提供了一些静态方法来更新和控制歌词显示。
 */
@SuppressLint("MissingPermission")
public class CSLyricHelper {


    private static Intent createUpdateIntent(PlayInfo playInfo, LyricData lyricData) {
        Intent intent = new Intent("com.makino.cslyric.getter.action.LYRIC");
        intent.putExtra("type", "update");
        intent.putExtra("lyricData", lyricData.toString());
        intent.putExtra("playInfo", playInfo.toString());
        return intent;
    }
    /**
     * 发送更新歌词信息的广播。
     *
     * @param context 上下文，用于发送广播。
     * @param playInfo 歌曲播放信息。
     * @param lyricData 歌词数据。
     */
    public static void updateLyric(Context context, PlayInfo playInfo, LyricData lyricData) {
        context.sendBroadcast(createUpdateIntent(playInfo, lyricData));
    }

    public static void updateLyricAsUser(Context context, PlayInfo playInfo, LyricData lyricData, UserHandle user) {
        context.sendBroadcastAsUser(createUpdateIntent(playInfo, lyricData), user);
    }

    private static Intent createPauseIntent(PlayInfo playInfo) {
        Intent intent = new Intent("com.makino.cslyric.getter.action.LYRIC");
        intent.putExtra("type", "pause");
        intent.putExtra("playInfo", playInfo.toString());
        return intent;
    }

    /**
     * 发送暂停控制信息的广播。
     *
     * @param context 上下文，用于发送广播。
     * @param playInfo 歌曲播放信息。
     */
    public static void pause(Context context, PlayInfo playInfo) {
        context.sendBroadcast(createPauseIntent(playInfo));
    }

    public static void pauseAsUser(Context context, PlayInfo playInfo, UserHandle user) {
        context.sendBroadcastAsUser(createPauseIntent(playInfo), user);
    }

    /**
     * 发送正在播放信息的广播。
     *
     * @param context 上下文，用于发送广播。
     * @param playInfo 歌曲播放信息。
     */
    public static void playing(Context context, PlayInfo playInfo) {
        Intent intent = new Intent("com.makino.cslyric.getter.action.LYRIC");
        intent.putExtra("type", "playing");
        intent.putExtra("playInfo", playInfo.toString());
        context.sendBroadcast(intent);
    }

    /**
     * LyricData 类用于封装歌词信息，并实现其字符串表示形式的生成。
     */
    public static class LyricData {

        public String lyric;

        public LyricData(String lyric) {
            this.lyric = lyric;
        }

        @NotNull
        @Override
        public String toString() {
            StringWriter sw = new StringWriter();
            try (JsonWriter jw = new JsonWriter(sw)) {
                jw.beginObject();
                jw.name("lyric").value(lyric);
                jw.endObject();
            } catch (IOException e) {
                e.fillInStackTrace();
            }
            return sw.toString();
        }
    }

    /**
     * PlayInfo 类用于封装播放信息，并实现其字符串表示形式的生成。
     */
    public static class PlayInfo {

        public String icon;

        public String packageName;

        public boolean isPlaying;

        public PlayInfo(String icon, String packageName) {
            this.icon = icon;
            this.packageName = packageName;
        }

        @NotNull
        @Override
        public String toString() {
            StringWriter sw = new StringWriter();
            try (JsonWriter jw = new JsonWriter(sw)) {
                jw.beginObject();
                jw.name("icon").value(icon);
                jw.name("packageName").value(packageName);
                jw.name("isPlaying").value(isPlaying);
                jw.endObject();
            } catch (IOException e) {
                e.fillInStackTrace();
            }
            return sw.toString();
        }
    }
}

