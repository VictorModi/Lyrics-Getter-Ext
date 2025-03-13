package statusbar.finder;

import android.content.Context;
import android.media.MediaMetadata;
import cn.zhaiyifan.lyric.LyricUtils;
import cn.zhaiyifan.lyric.model.Lyric;
import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.moji4j.MojiConverter;
import com.moji4j.MojiDetector;
import kotlin.Pair;
import statusbar.finder.app.event.LyricsResultChange;
import statusbar.finder.data.db.DatabaseHelper;
import statusbar.finder.data.model.DataOrigin;
import statusbar.finder.data.model.LyricResult;
import statusbar.finder.data.model.MediaInfo;
import statusbar.finder.data.repository.ActiveRepository;
import statusbar.finder.data.repository.LyricRepository;
import statusbar.finder.data.repository.ResRepository;
import statusbar.finder.provider.*;
import statusbar.finder.utils.CheckLanguageUtil;
import statusbar.finder.utils.LyricSearchUtil;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LrcGetter {
    private static final ILrcProvider[] providers = {
            new NeteaseProvider(),
            new KugouProvider(),
            new QQMusicProvider(),
            new MusixMatchProvider()
    };
    private static MessageDigest messageDigest;

    public static Lyric getLyric(Context context, MediaMetadata mediaMetadata, String sysLang, String packageName) {
        return getLyric(context, new MediaInfo(mediaMetadata), sysLang, packageName);
    }

    public static Lyric getLyric(Context context, MediaInfo mediaInfo, String sysLang, String packageName) {
        DatabaseHelper.INSTANCE.init(context);

        if (messageDigest == null) {
            try {
                messageDigest = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException e) {
                e.fillInStackTrace();
                return null;
            }
        }
        Pair<LyricResult, Long> databaseResult = LyricRepository.INSTANCE.getActiveLyricFromDatabase(mediaInfo, packageName);
        LyricResult currentResult = databaseResult.getFirst();

        if (currentResult == null) {
            searchLyricsResultByInfo(mediaInfo, databaseResult.getSecond(), sysLang);
            currentResult = LyricRepository.INSTANCE.getActiveLyricFromDatabaseByOriginId(databaseResult.getSecond());
        } else {
            LyricsResultChange.Companion.getInstance().notifyResult(new LyricsResultChange.Data(mediaInfo, currentResult));
            return LyricUtils.parseLyric(currentResult, mediaInfo, packageName);
        }
        MojiDetector detector = new MojiDetector();
        MojiConverter converter = new MojiConverter();
        if (currentResult == null && (!detector.hasKana(mediaInfo.getTitle()) && detector.hasLatin(mediaInfo.getTitle()))) {
            MediaInfo hiraganaMediaInfo = mediaInfo.clone();
            hiraganaMediaInfo.setTitle(converter.convertRomajiToHiragana(mediaInfo.getTitle()));
            if (detector.hasLatin(hiraganaMediaInfo.getTitle())) {
                LyricsResultChange.Companion.getInstance().notifyResult(new LyricsResultChange.Data(mediaInfo, null));
                return null;
            }
            searchLyricsResultByInfo(hiraganaMediaInfo, databaseResult.getSecond(), sysLang);
            currentResult = LyricRepository.INSTANCE.getActiveLyricFromDatabaseByOriginId(databaseResult.getSecond());
            if (currentResult == null) {
                hiraganaMediaInfo.setTitle(converter.convertRomajiToKatakana(mediaInfo.getTitle()));
                searchLyricsResultByInfo(hiraganaMediaInfo, databaseResult.getSecond(), sysLang);
                currentResult = LyricRepository.INSTANCE.getActiveLyricFromDatabaseByOriginId(databaseResult.getSecond());
            }
        }


        if (currentResult != null) {
            currentResult.setDataOrigin(DataOrigin.INTERNET);
            LyricsResultChange.Companion.getInstance().notifyResult(new LyricsResultChange.Data(mediaInfo, currentResult));
            return LyricUtils.parseLyric(currentResult, mediaInfo, packageName);
        } else {
            LyricsResultChange.Companion.getInstance().notifyResult(new LyricsResultChange.Data(mediaInfo, null));
            return null;
        }
    }

    private static void searchLyricsResultByInfo(MediaInfo mediaInfo, Long originId, String sysLang) {
        String bestMatchSource = null;
        long bestMatchDistance = 0;
        for (ILrcProvider provider : providers) {
            try {
                LyricResult lyricResult = provider.getLyric(mediaInfo);
                if (lyricResult != null && lyricResult.getLyric() != null) {
                    String allLyrics;
                    if (lyricResult.getTranslatedLyric() != null) {
                        allLyrics = LyricUtils.getAllLyrics(false, lyricResult.getTranslatedLyric());
                    } else {
                        allLyrics = LyricUtils.getAllLyrics(false, lyricResult.getLyric());
                    }
                    if (!CheckLanguageUtil.isJapanese(allLyrics)) {
                        switch (sysLang) {
                            case "zh-CN":
                                if (lyricResult.getTranslatedLyric() != null) {
                                    lyricResult.setTranslatedLyric(ZhConverterUtil.toSimple(lyricResult.getTranslatedLyric()));
                                } else {
                                    lyricResult.setLyric(ZhConverterUtil.toSimple(lyricResult.getLyric()));
                                }
                                break;
                            case "zh-TW":
                                if (lyricResult.getTranslatedLyric() != null) {
                                    lyricResult.setTranslatedLyric(ZhConverterUtil.toTraditional(lyricResult.getTranslatedLyric()));
                                } else {
                                    lyricResult.setLyric(ZhConverterUtil.toTraditional(lyricResult.getLyric()));
                                }
                            default:
                                break;
                        }
                    }
                    ResRepository.INSTANCE.insertResData(originId, lyricResult);
                    if (LyricSearchUtil.isLyricContent(lyricResult.getLyric())
                            && (bestMatchSource == null || bestMatchDistance > lyricResult.getDistance())) {
                        bestMatchSource = lyricResult.getSource();
                    }
                }
            } catch (IOException e) {
                e.fillInStackTrace();
            }
        }
        if (bestMatchSource != null) {
            Res bestMatchResult = ResRepository.INSTANCE.getResByOriginIdAndProvider(originId, bestMatchSource);
            assert bestMatchResult != null;
            ActiveRepository.INSTANCE.insertActiveLog(originId, bestMatchResult.getId());
        }
    }
}
