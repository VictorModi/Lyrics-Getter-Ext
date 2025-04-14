package statusbar.finder.hook.superlyric;

import android.content.Context;
import android.os.RemoteException;

import com.github.kyuubiran.ezxhelper.Log;
import com.hchen.superlyricapi.ISuperLyric;
import com.hchen.superlyricapi.SuperLyricData;
import com.hchen.superlyricapi.SuperLyricPush;

import java.util.Objects;
import java.util.function.Consumer;

import statusbar.finder.BuildConfig;

public class SuperLyricHelper {
    private static ISuperLyric iSuperLyric;
    private static String lastLyric;

    public static void registerSuperLyricController(Context context) {
        if (context == null) return;

        SuperLyricPush.registerSuperLyricController(context, new Consumer<ISuperLyric>() {
            @Override
            public void accept(ISuperLyric iSuperLyric) {
                SuperLyricHelper.iSuperLyric = iSuperLyric;
                Log.i("Successfully connected to ISuperLyric: " + iSuperLyric, null);
            }
        });
    }

    public static void sendLyric(String lyric) {
        sendLyric(lyric,
            new SuperLyricData()
                .setPackageName(BuildConfig.APPLICATION_ID)
                .setLyric(lyric)
        );
    }

    public static void sendLyric(String lyric, SuperLyricData data) {
        if (iSuperLyric == null) return;
        if (lyric == null || lyric.isEmpty()) return;
        if (Objects.equals(lastLyric, lyric)) return;

        try {
            lastLyric = lyric;
            iSuperLyric.onSuperLyric(data.setLyric(lastLyric));
        } catch (RemoteException e) {
            Log.e("sendLyric: ", e);
        }
    }

    public static void sendStop() {
        sendStop(
            new SuperLyricData()
                .setPackageName(BuildConfig.APPLICATION_ID)
        );
    }

    public static void sendStop(SuperLyricData data) {
        if (iSuperLyric == null) return;

        try {
            iSuperLyric.onStop(data);
        } catch (RemoteException e) {
            Log.e("sendStop: ", e);
        }
    }
}
