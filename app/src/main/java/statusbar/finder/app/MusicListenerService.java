package statusbar.finder.app;

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
import android.os.*;
import android.service.notification.NotificationListenerService;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import cn.zhaiyifan.lyric.LyricUtils;
import cn.zhaiyifan.lyric.model.Lyric;
import com.hchen.superlyricapi.SuperLyricData;
import com.hchen.superlyricapi.SuperLyricPush;
import com.hchen.superlyricapi.SuperLyricTool;
import statusbar.finder.BuildConfig;
import statusbar.finder.CSLyricHelper;
import statusbar.finder.LrcGetter;
import statusbar.finder.R;
import statusbar.finder.app.event.AppsListChanged;
import statusbar.finder.app.event.LyricSentenceUpdate;
import statusbar.finder.app.event.LyricsChange;
import statusbar.finder.app.event.LyricsResultChange;
import statusbar.finder.data.db.DatabaseHelper;
import statusbar.finder.data.model.MediaInfo;
import statusbar.finder.data.repository.ResRepository;
import statusbar.finder.hook.tool.Tool;
import statusbar.finder.misc.Constants;

import java.util.*;

import static statusbar.finder.misc.Constants.*;

public class MusicListenerService extends NotificationListenerService {
    private MediaSessionManager mMediaSessionManager;
    private MediaController mMediaController;
    private NotificationManager mNotificationManager;

    private final ArrayList<String> mTargetPackageList = new ArrayList<>();
    private Observer<Void> mAppsListChangedObserver;
    private Observer<LyricsResultChange.Data> mGetResultObserver;

    private SharedPreferences mSharedPreferences;

    private Lyric mLyric;
    private String requiredLrcTitle;
    private Notification mLyricNotification;
    private LyricsResultChange.Data mCurrentResult;
    private MediaInfo mCurrentMediaInfo;
    private long mLastSentenceFromTime = -1;

    @SuppressLint("ConstantLocale")
    public final static String systemLanguage = Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();
    private String drawBase64;
    private LrcUpdateThread curLrcUpdateThread;
    public static MusicListenerService instance;
    public CSLyricHelper.PlayInfo playInfo;
    private static Lyric.Sentence lastSentence;

    private final Handler mHandler = new Handler(Objects.requireNonNull(Looper.myLooper())) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_LYRIC_UPDATE_DONE && msg.getData().getString("title", "").equals(requiredLrcTitle)) {
                if (msg.obj != null) {
                    Lyric lyric = (Lyric) msg.obj;
                    LyricsChange.Companion.getInstance().notifyResult(
                            new LyricsChange.Data(lyric, ResRepository.INSTANCE.getProvidersMapByOriginId(lyric.lyricResult.getOriginId())));
                }
            }
        }
    };

    private final Runnable mLyricUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mMediaController == null ||
                    mMediaController.getPlaybackState() == null ||
                    mMediaController.getPlaybackState().getState() != PlaybackState.STATE_PLAYING) {
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
                    stopLyric(state);
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
            mCurrentMediaInfo = new MediaInfo(metadata);
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
                startSearch();
            }
        }
    };

    public void startSearch() {
        if (curLrcUpdateThread == null || !curLrcUpdateThread.isAlive()) {
            curLrcUpdateThread = new LrcUpdateThread(
                    getApplicationContext(),
                    mHandler,
                    mMediaController.getMetadata(),
                    mMediaController.getPackageName()
            );
            curLrcUpdateThread.start();
        }
    }

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

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        if (Tool.INSTANCE.getXpActivation()) {
            stopSelf();
            return;
        }
        instance = this;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mLyricNotification = buildLrcNotification();
        mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mAppsListChangedObserver = data -> {
             updateTargetPackageList();
             bindMediaListeners();
        };
        drawBase64 = SuperLyricTool.drawableToBase64(getDrawable(R.drawable.ic_statusbar_icon));
        playInfo = new CSLyricHelper.PlayInfo(drawBase64, getPackageName());
        DatabaseHelper.INSTANCE.init(getApplicationContext());
        mGetResultObserver = data -> {
            mCurrentResult = data;
            mLyricNotification = buildLrcNotification(data);
            mNotificationManager.notify(NOTIFICATION_ID_LRC, mLyricNotification);
        };

        Observer<LyricsChange.Data> mLyricsChangeObserver = data -> mLyric = data.getLyric();
        AppsListChanged.Companion.getInstance().observeForever(mAppsListChangedObserver);
        LyricsResultChange.Companion.getInstance().observeForever(mGetResultObserver);
        LyricsChange.Companion.getInstance().observeForever(mLyricsChangeObserver);
        updateTargetPackageList();
        bindMediaListeners();
    }

    @Override
    public void onListenerDisconnected() {
        stopLyric();
        unBindMediaListeners();
        AppsListChanged.Companion.getInstance().removeObserver(mAppsListChangedObserver);
        LyricsResultChange.Companion.getInstance().removeObserver(mGetResultObserver);
        super.onListenerDisconnected();
    }

    private Notification buildLrcNotification() {
        return buildLrcNotification(null);
    }

    private Notification buildLrcNotification(Object data) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_LRC);
        builder.setSmallIcon(R.drawable.ic_music).setOngoing(true);
        PendingIntent resultPendingIntent =
                TaskStackBuilder.create(this).addNextIntentWithParentStack(new Intent(this, LyricsActivity.class)).getPendingIntent(0,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (data != null) {
            String contentText = "Incorrect data type, please send this message to the developer.\nType: " + Object.class.getName();
            if (data instanceof LyricSentenceUpdate.Data) {
                contentText = notificationLyricsContentText((LyricSentenceUpdate.Data) data);
            } else if (data instanceof LyricsResultChange.Data) {
                contentText = notificationResultContentText((LyricsResultChange.Data) data);
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
            if (mCurrentMediaInfo.getAlbum().isBlank()) {
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

    private String notificationResultContentText(LyricsResultChange.Data data) {
        if (data.getResult() == null || data.getResult().getResultInfo() == null) {
            return "Failed to retrieve lyrics! Bad luck!";
        }
        String contentTextResult;
        contentTextResult = String.format(
                "Result: %s - %s",
                data.getResult().getResultInfo().getTitle(),
                data.getResult().getResultInfo().getArtist()
        );
        if (data.getResult().getResultInfo().getAlbum().isBlank()) {
            contentTextResult += " - " +  data.getResult().getResultInfo().getAlbum();
        }
        contentTextResult += "\n" +
                String.format("Source: %s (%s)",
                        data.getResult().getSource(),
                        data.getResult().getDataOrigin().getCapitalizedName()
                );
        return contentTextResult;
    }

    private String notificationLyricsContentText(LyricSentenceUpdate.Data data) {
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
        stopLyric(new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PAUSED, 0L, 1.0f)
                .setActions(
                        PlaybackState.ACTION_PLAY |
                                PlaybackState.ACTION_PAUSE |
                                PlaybackState.ACTION_PLAY_PAUSE
                )
                .build());
    }

    private void stopLyric(PlaybackState playbackState) {
        mHandler.removeCallbacks(mLyricUpdateRunnable);
        mNotificationManager.cancel(NOTIFICATION_ID_LRC);
        SuperLyricPush.onStop(
                new SuperLyricData()
                        .setPackageName(BuildConfig.APPLICATION_ID)
                        .setPlaybackState(playbackState)
        ); // 状态暂停
        playInfo.isPlaying = false;
        CSLyricHelper.pause(getApplicationContext(), playInfo);
    }

    public Lyric getLyric(){
        return mLyric;
    }
    private void updateLyric(long position) {
        if (mNotificationManager == null || mLyric == null) {
            return;
        }
        Lyric.Sentence sentence = LyricUtils.getSentence(mLyric.sentenceList, position, 0 ,mLyric.offset);
        if (sentence == null) return;
        if (sentence.equals(lastSentence)) return;
        int delay = calculateDelay(position);
        if (sentence.fromTime != mLastSentenceFromTime) {
            if (sentence.content.isBlank()) return;
            String curLyric;
            String translateType = mSharedPreferences.getString(PREFERENCE_KEY_TRANSLATE_TYPE, "origin");
            Lyric.Sentence translatedSentence = getTranslatedSentence(position);
            curLyric = switch (translateType) {
                case "translated" ->
                        translatedSentence != null ? translatedSentence.content.trim() :
                                sentence.content.trim();
                case "both" ->
                        sentence.content.trim() + (
                                translatedSentence != null ?
                                        ("\n\r" + translatedSentence.content.trim()) :
                                        "");
                default -> sentence.content.trim();
            };
            int adjustment = "both".equals(translateType) && translatedSentence != null ? delay / 2 : delay;
            delay = adjustment - 3;
            delay = Math.max(delay, 1);
            if (mSharedPreferences.getBoolean(PREFERENCE_KEY_FORCE_REPEAT, false)
                    && lastSentence != null
                    && lastSentence.content != null
                    && lastSentence.content.equals(sentence.content)) {

                curLyric = insertZeroWidthSpace(curLyric);
            }

            LyricSentenceUpdate.Data data = new LyricSentenceUpdate.Data(
                    sentence.content.trim(),
                    translatedSentence != null ? translatedSentence.content.trim() : null,
                    LyricUtils.getSentenceIndex(mLyric.sentenceList, position, 0 ,mLyric.offset),
                    delay);
            LyricSentenceUpdate.Companion.getInstance().notifyLyrics(data);
            // mLyricNotification = buildLrcNotification(data);
            mNotificationManager.notify(NOTIFICATION_ID_LRC, mLyricNotification);
            // EventTools.INSTANCE.sendLyric(getApplicationContext(), curLyric, true, drawBase64, false, "", getPackageName(), delay);
            Log.d("updateLyric: ", String.format("Lyric: %s , delay: %d", curLyric, delay));
            SuperLyricPush.onSuperLyric(
                    new SuperLyricData()
                            .setPackageName(BuildConfig.APPLICATION_ID)
                            .setBase64Icon(drawBase64)
                            .setLyric(curLyric)
                            .setDelay(delay)
                            .setPlaybackState(mMediaController.getPlaybackState())
                            .setMediaMetadata(mMediaController.getMetadata())
            );
            playInfo.isPlaying = true;
            CSLyricHelper.updateLyric(
                    getApplicationContext(),
                    playInfo,
                    new CSLyricHelper.LyricData(curLyric)
            );
            mLyricNotification.tickerText = curLyric;
            mLyricNotification.when = System.currentTimeMillis();
            mNotificationManager.notify(NOTIFICATION_ID_LRC, mLyricNotification);
            mLastSentenceFromTime = sentence.fromTime;
            lastSentence = sentence;
        }
    }

    public int calculateDelay(long position) {
        // 注意: 结果的单位为秒 (Second)
        int nextFoundIndex = LyricUtils.getSentenceIndex(mLyric.sentenceList, position, 0, mLyric.offset) + 1;

        // 如果没有更多的歌词，直接返回 1
        if (nextFoundIndex >= mLyric.sentenceList.size()) {
            return 1;
        }

        // 计算延迟时间
        int delay = (int) ((mLyric.sentenceList.get(nextFoundIndex).fromTime - position) / 1000);

        // 如果开启翻译状态并且翻译歌词列表不为空，减半延迟
        if ("origin".equals(mSharedPreferences.getString(PREFERENCE_KEY_TRANSLATE_TYPE, "origin")) &&
                        getTranslatedSentence(position) != null) {
            delay /= 2;
        }

        // 确保延迟时间最小为 1
        return Math.max(delay, 1);
    }

    public static String insertZeroWidthSpace(String input) {
        if (input == null || input.isEmpty() || input.length() < 2) {
            return input;
        }
        Random random = new Random();
        int position = 1 + random.nextInt(input.length() - 1);
        StringBuilder modifiedString = new StringBuilder(input);
        modifiedString.insert(position, '\u200B');
        return modifiedString.toString();

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

        public LrcUpdateThread(Context context, Handler handler, MediaMetadata data, String packageName) {
            super();
            this.data = data;
            this.handler = handler;
            this.context = context;
            this.packageName = packageName;
        }

        @Override
        public void run() {
            if (handler == null) return;
            Lyric lrc = LrcGetter.INSTANCE.getLyric(context, data, systemLanguage, packageName);
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
