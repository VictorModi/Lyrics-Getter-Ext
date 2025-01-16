package statusbar.finder;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import cn.lyric.getter.api.API;
import cn.lyric.getter.api.data.ExtraData;
import cn.lyric.getter.api.tools.Tools;
import cn.zhaiyifan.lyric.LyricUtils;
import cn.zhaiyifan.lyric.model.Lyric;
import statusbar.finder.livedata.AppsListChanged;
import statusbar.finder.livedata.GetResult;
import statusbar.finder.livedata.LyricsChanged;
import statusbar.finder.misc.Constants;
import statusbar.finder.provider.ILrcProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static statusbar.finder.misc.Constants.PREFERENCE_KEY_REQUIRE_TRANSLATE;

public class MusicListenerService extends NotificationListenerService {

    private static final int NOTIFICATION_ID_LRC = 1;

    private static final int MSG_LYRIC_UPDATE_DONE = 2;

    private MediaSessionManager mMediaSessionManager;
    private MediaController mMediaController;
    private NotificationManager mNotificationManager;

    private final ArrayList<String> mTargetPackageList = new ArrayList<>();
    private Observer<Void> mAppsListChangedObserver;
    private Observer<GetResult.Data> mGetResultObserver;

    private SharedPreferences mSharedPreferences;

    private Lyric mLyric;
    private String requiredLrcTitle;
    private Notification mLyricNotification;
    private GetResult.Data mCurrentResult;
    private ILrcProvider.MediaInfo mCurrentMediaInfo;
    private long mLastSentenceFromTime = -1;

    @SuppressLint("ConstantLocale")
    public final static String systemLanguage = Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();
    private String drawBase64;
    private Thread curLrcUpdateThread;
    private API lyricsGetterApi;
    public static MusicListenerService instance;
    private SharedPreferences offsetpreferences;
    private SharedPreferences translationstatusreferences;
    public String musicinfo;

    private final Handler mHandler = new Handler(Objects.requireNonNull(Looper.myLooper())) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_LYRIC_UPDATE_DONE && msg.getData().getString("title", "").equals(requiredLrcTitle)) {
                mLyric = (Lyric) msg.obj;

                if (mLyric == null) stopLyric();

            }
        }
    };

    private final Runnable mLyricUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mMediaController == null || Objects.requireNonNull(mMediaController.getPlaybackState()).getState() != PlaybackState.STATE_PLAYING) {
                stopLyric();
                return;
            }
            updateLyric(mMediaController.getPlaybackState().getPosition());
            mHandler.postDelayed(mLyricUpdateRunnable, 250);
        }
    };

    private final MediaController.Callback mMediaCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackState state) {
            super.onPlaybackStateChanged(state);
            if (state != null) {
                if (state.getState() == PlaybackState.STATE_PLAYING) {
                    startLyric();
                } else {
                    stopLyric();
                }
            }
        }

        @Override
        public void onSessionDestroyed() {
            stopLyric();
            super.onSessionDestroyed();
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            stopLyric();
            mLyric = null;
            if (metadata == null) return;
            mLyricNotification = buildLrcNotification();
            mNotificationManager.notify(NOTIFICATION_ID_LRC, mLyricNotification);
            requiredLrcTitle = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            mCurrentMediaInfo = new ILrcProvider.MediaInfo(metadata);
            if (curLrcUpdateThread == null || !curLrcUpdateThread.isAlive()) {
                curLrcUpdateThread = new LrcUpdateThread(getApplicationContext(), mHandler, metadata, mMediaController.getPackageName(), mSharedPreferences.getBoolean(PREFERENCE_KEY_REQUIRE_TRANSLATE, false));
                curLrcUpdateThread.start();
            }
        }
    };

    private final MediaSessionManager.OnActiveSessionsChangedListener onActiveSessionsChangedListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
        @Override
        public void onActiveSessionsChanged(@Nullable List<MediaController> controllers) {
            if (mMediaController != null) mMediaController.unregisterCallback(mMediaCallback);
            if (controllers == null) return;
            for (MediaController controller : controllers) {
                if (!mTargetPackageList.contains(controller.getPackageName())) continue;
                if (getMediaControllerPlaybackState(controller) == PlaybackState.STATE_PLAYING) {
                    mMediaController = controller;
                    break;
                }
            }
            if (mMediaController != null) {
                mMediaController.registerCallback(mMediaCallback);
                mMediaCallback.onMetadataChanged(mMediaController.getMetadata());
                mMediaCallback.onPlaybackStateChanged(mMediaController.getPlaybackState());
            }
        }
    };

    private int getMediaControllerPlaybackState(MediaController controller) {
        if (controller != null) {
            final PlaybackState playbackState = controller.getPlaybackState();
            if (playbackState != null) {
                return playbackState.getState();
            }
        }
        return PlaybackState.STATE_NONE;
    }

    public MusicListenerService() {
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
//        Notification notification = sbn.getNotification();
//            if (sbn.isClearable()){
//                if (notification != null) {
//                    String lyricText = String.format("%s : %s", notification.extras.getString(Notification.EXTRA_TITLE), notification.extras.getString(Notification.EXTRA_TEXT));
//                    lyricsGetterApi.sendLyric(lyricText,new ExtraData(
//                            true,
//                            drawBase64,
//                            false,
//                            getPackageName(),
//                            0
//                    ));
//                }
//        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        instance = this;
        offsetpreferences = getSharedPreferences("offset", MODE_PRIVATE);
        translationstatusreferences = getSharedPreferences("translationstatus", MODE_PRIVATE);
        lyricsGetterApi = new API();
        drawBase64 = Tools.INSTANCE.drawableToBase64(getDrawable(R.drawable.ic_statusbar_icon));
        // Log.d("systemLanguage", systemLanguage);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mLyricNotification = buildLrcNotification();
        mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mAppsListChangedObserver = data -> {
             updateTargetPackageList();
             bindMediaListeners();
        };

        mGetResultObserver = data -> {
            mCurrentResult = data;
            mLyricNotification = buildLrcNotification(data);
            mNotificationManager.notify(NOTIFICATION_ID_LRC, mLyricNotification);
        };
        AppsListChanged.getInstance().observeForever(mAppsListChangedObserver);
        GetResult.getInstance().observeForever(mGetResultObserver);
        updateTargetPackageList();
        bindMediaListeners();
    }

    @Override
    public void onListenerDisconnected() {
        stopLyric();
        unBindMediaListeners();
        AppsListChanged.getInstance().removeObserver(mAppsListChangedObserver);
        GetResult.getInstance().removeObserver(mGetResultObserver);
        super.onListenerDisconnected();
    }

    private Notification buildLrcNotification() {
        return buildLrcNotification(null);
    }

    private Notification buildLrcNotification(Object data) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_LRC);
        builder.setSmallIcon(R.drawable.ic_music).setOngoing(true);
        PendingIntent resultPendingIntent =
                TaskStackBuilder.create(this).addNextIntentWithParentStack(new Intent(this, LrcView.class)).getPendingIntent(0,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (data != null) {
            String contentText = "Incorrect data type, please send this message to the developer.\nType: " + Object.class.getName();
            if (data instanceof LyricsChanged.Data) {
                contentText = notificationLyricsContentText((LyricsChanged.Data) data);
            } else if (data instanceof GetResult.Data) {
                contentText = notificationResultContentText((GetResult.Data) data);
            }
            builder.setContentText("Tap to View Details")
                    .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(contentText))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(resultPendingIntent);
        } else {
            builder.setContentText("Still Searching...");
        }
        String contentTitleFormat = "%s - %s";
        if (mCurrentMediaInfo != null) {
            if (mCurrentMediaInfo.getAlbum() != null) {
                contentTitleFormat += " - %s";
                builder.setContentTitle(String.format(contentTitleFormat, mCurrentMediaInfo.getTitle(),
                        mCurrentMediaInfo.getArtist(), mCurrentMediaInfo.getAlbum()));
            } else {
                builder.setContentTitle(String.format("%s - %s", mCurrentMediaInfo.getTitle(),
                        mCurrentMediaInfo.getArtist()));
            }
        } else {
            builder.setContentTitle("Failed to retrieve current playback media information");
            builder.setContentText("Please check if the target application is correctly configured and if the target application is currently playing media.")
                    .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText("Please check if the target application is correctly configured and if the target application is currently playing media."))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        }
        Notification notification = builder.build();
        notification.extras.putLong("ticker_icon", R.drawable.ic_music);
        notification.extras.putBoolean("ticker_icon_switch", false);
        notification.flags |= Constants.FLAG_ALWAYS_SHOW_TICKER;
        notification.flags |= Constants.FLAG_ONLY_UPDATE_TICKER;
        return notification;
    }

    private String notificationResultContentText(GetResult.Data data) {
        if (data.getResult() == null) {
            return "Failed to retrieve lyrics! Bad luck!";
        }

        String contentTextResult;
        contentTextResult = String.format("Result: %s - %s", mCurrentResult.getResult().mResultInfo.getTitle(),
                mCurrentResult.getResult().mResultInfo.getArtist());
        if (mCurrentResult.getResult().mResultInfo.getAlbum() != null) {
            contentTextResult += " - " +  mCurrentResult.getResult().mResultInfo.getAlbum();
        }
        contentTextResult += "\n" + String.format("Source: %s (%s)", mCurrentResult.getResult().mSource, mCurrentResult.getResult().mOrigin.getCapitalizedName());
        return contentTextResult;
    }

    private String notificationLyricsContentText(LyricsChanged.Data data) {
        String contentTextResult = notificationResultContentText(mCurrentResult);
        String contentTextLyric = String.format("Lyric: %s", data.getLyric());
        if (data.getTranslatedLyric() != null) {
            contentTextLyric += "\nTranslatedLyric: " + data.getTranslatedLyric();
        }
        contentTextLyric += "\nLyricDelay: " + data.getDelay() + " sec";
        return contentTextResult + "\n" + contentTextLyric;
    }

    private void bindMediaListeners() {
        ComponentName listener = new ComponentName(this, MusicListenerService.class);
        try {
            mMediaSessionManager.addOnActiveSessionsChangedListener(onActiveSessionsChangedListener, listener);
            onActiveSessionsChangedListener.onActiveSessionsChanged(mMediaSessionManager.getActiveSessions(listener));
        } catch (SecurityException ignored){
        }
    }

    private void unBindMediaListeners() {
        if (mMediaSessionManager != null) mMediaSessionManager.removeOnActiveSessionsChangedListener(onActiveSessionsChangedListener);
        if (mMediaController != null) mMediaController.unregisterCallback(mMediaCallback);
        mMediaController = null;
    }

    private void updateTargetPackageList() {
        mTargetPackageList.clear();
        String value = mSharedPreferences.getString(Constants.PREFERENCE_KEY_TARGET_PACKAGES, "");
        String[] arr = value.split(";");
        for (String str : arr) {
            if (TextUtils.isEmpty(str)) continue;
            mTargetPackageList.add(str.trim());
        }
    }

    private void startLyric() {
        mLastSentenceFromTime = -1;
        mLyricNotification.tickerText = null;
        mNotificationManager.notify(NOTIFICATION_ID_LRC, mLyricNotification);
        mHandler.post(mLyricUpdateRunnable);
    }

    private void stopLyric() {
        mHandler.removeCallbacks(mLyricUpdateRunnable);
        mNotificationManager.cancel(NOTIFICATION_ID_LRC);
        lyricsGetterApi.clearLyric();
    }

    public Lyric getLyric(){
        return mLyric;
    }
    public void sync(){
        Lyric.Sentence sentence;
        if (translationstatusreferences.getBoolean(musicinfo, false)) {
            mLyric.offset = offsetpreferences.getInt(musicinfo + " ,tran", mLyric.offset);
            sentence = LyricUtils.getSentence(mLyric.translatedSentenceList, mMediaController.getPlaybackState().getPosition(), 0, mLyric.offset);
        } else {
            mLyric.offset = offsetpreferences.getInt(musicinfo, mLyric.offset);
            sentence = LyricUtils.getSentence(mLyric.sentenceList, mMediaController.getPlaybackState().getPosition(), 0, mLyric.offset);
        }
        String curLyric = sentence.content.trim();
        mLyricNotification.tickerText = curLyric;
        mNotificationManager.notify(NOTIFICATION_ID_LRC, mLyricNotification);
    }
    private void updateLyric(long position) {
        if (mNotificationManager == null || mLyric == null) {
            return;
        }

        musicinfo = "Title:" + mLyric.title
                + " ,Artist:" + mLyric.artist
                + " ,Album:" + mLyric.album
                + " ,By:" + mLyric.by
                + " ,Author:" + mLyric.author
                + " ,Length:" + mLyric.length;
        if (translationstatusreferences.getBoolean(musicinfo, false))
            mLyric.offset = offsetpreferences.getInt(musicinfo + " ,tran", mLyric.offset);
        else
            mLyric.offset = offsetpreferences.getInt(musicinfo, mLyric.offset);
        Lyric.Sentence sentence;
        if (translationstatusreferences.getBoolean(musicinfo, false) && !mSharedPreferences.getBoolean(PREFERENCE_KEY_REQUIRE_TRANSLATE, false))
            sentence = LyricUtils.getSentence(mLyric.translatedSentenceList, position, 0, mLyric.offset);
        else
            sentence = LyricUtils.getSentence(mLyric.sentenceList, position, 0, mLyric.offset);
        if (sentence == null) {
            return;
        }

        int delay = calculateDelay(position);
        if (sentence.fromTime != mLastSentenceFromTime) {
            if (sentence.content.isEmpty()) {
                return;
            }
            String curLyric = sentence.content.trim();
            Lyric.Sentence translatedSentence = null;
            if (mSharedPreferences.getBoolean(PREFERENCE_KEY_REQUIRE_TRANSLATE, false)) { // 增添翻译
                translatedSentence = getTranslatedSentence(position);
                if (translatedSentence != null && !Objects.equals(translatedSentence.content, "") && !Objects.equals(sentence.content, "")) {
                    curLyric += "\n\r" + translatedSentence.content.trim();
                }
            }
            LyricsChanged.Data data = new LyricsChanged.Data(sentence.content.trim(), translatedSentence != null ? translatedSentence.content.trim() : null, delay);
            LyricsChanged.getInstance().notifyLyrics(data);
            // mLyricNotification = buildLrcNotification(data);
            mNotificationManager.notify(NOTIFICATION_ID_LRC, mLyricNotification);
            // EventTools.INSTANCE.sendLyric(getApplicationContext(), curLyric, true, drawBase64, false, "", getPackageName(), delay);
            Log.d("updateLyric: ", String.format("Lyric: %s , delay: %d", curLyric, delay));
            lyricsGetterApi.sendLyric(curLyric, new ExtraData(
                    true,
                    drawBase64,
                    false,
                    getPackageName(),
                    delay
            ));
            mLyricNotification.tickerText = curLyric;
            mLyricNotification.when = System.currentTimeMillis();
            mNotificationManager.notify(NOTIFICATION_ID_LRC, mLyricNotification);
            mLastSentenceFromTime = sentence.fromTime;
        }
    }

    private int calculateDelay(long position) {
        int nextFoundIndex = LyricUtils.getSentenceIndex(mLyric.sentenceList, position, 0, mLyric.offset) + 1;

        if (nextFoundIndex >= mLyric.sentenceList.size()) {
            return 1;
        }

        int delay = (int) ((mLyric.sentenceList.get(nextFoundIndex).fromTime - position) / 1000) - 3;

        if (translationstatusreferences.getBoolean(musicinfo, false) && !mLyric.translatedSentenceList.isEmpty()) {
            delay /= 2;
        }
        return Math.max(delay, 1);
    }

    private Lyric.Sentence getTranslatedSentence(long position) {  // 获取翻译歌词
        if (!mLyric.translatedSentenceList.isEmpty()) {
            return LyricUtils.getSentence(mLyric.translatedSentenceList, position, 0, mLyric.offset);
        }
        return null;
    }


    public static class LrcUpdateThread extends Thread {
        private final Handler handler;
        private final MediaMetadata data;
        private final Context context;
        private final String packageName;
        private final boolean requireTranslate;

        public LrcUpdateThread(Context context, Handler handler, MediaMetadata data, String packageName, boolean requireTranslate) {
            super();
            this.data = data;
            this.handler = handler;
            this.context = context;
            this.packageName = packageName;
            this.requireTranslate = requireTranslate;
        }

        @Override
        public void run() {
            if (handler == null) return;
            Lyric lrc = LrcGetter.getLyric(context, data, systemLanguage, packageName, requireTranslate);
            Message message = new Message();
            message.what = MSG_LYRIC_UPDATE_DONE;
            message.obj = lrc;
            Bundle bundle = new Bundle();
            bundle.putString("title", data.getString(MediaMetadata.METADATA_KEY_TITLE));
            message.setData(bundle);
            handler.sendMessage(message);
        }
    }
}
