package statusbar.finder;

import android.content.Context;
import android.media.MediaMetadata;
import androidx.core.util.Pair;
import cn.zhaiyifan.lyric.LyricUtils;
import cn.zhaiyifan.lyric.model.Lyric;
import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.moji4j.MojiConverter;
import com.moji4j.MojiDetector;
import statusbar.finder.livedata.GetResult;
import statusbar.finder.misc.CheckLanguageUtil;
import statusbar.finder.provider.ILrcProvider;
import statusbar.finder.provider.KugouProvider;
import statusbar.finder.provider.MusixMatchProvider;
import statusbar.finder.provider.NeteaseProvider;
import statusbar.finder.provider.QQMusicProvider;
import statusbar.finder.provider.utils.LyricSearchUtil;
import statusbar.finder.sql.ActiveManager;
import statusbar.finder.sql.OriginManager;
import statusbar.finder.sql.ResultManager;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class LrcGetter {
    private static final ILrcProvider[] providers = {
            new NeteaseProvider(),
            new KugouProvider(),
            new QQMusicProvider(),
            new MusixMatchProvider()
    };
    private static MessageDigest messageDigest;

    public static Lyric getLyric(Context context, MediaMetadata mediaMetadata, String sysLang, String packageName, boolean requireTranslate) {
        return getLyric(context, new ILrcProvider.MediaInfo(mediaMetadata), sysLang, packageName, requireTranslate);
    }

    public static Lyric getLyric(Context context, ILrcProvider.MediaInfo mediaInfo, String sysLang, String packageName, boolean requireTranslate) {
        DatabaseHelper.init(context);
        MojiDetector detector = new MojiDetector();
        MojiConverter converter = new MojiConverter();

        if (messageDigest == null) {
            try {
                messageDigest = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException e) {
                e.fillInStackTrace();
                return null;
            }
        }
        Pair<ILrcProvider.LyricResult, Long> databaseResult = getActiveLyricFromDatabase(mediaInfo, packageName);
        ILrcProvider.LyricResult currentResult = databaseResult.first;

        if (currentResult == null) {
            searchLyricsResultByInfo(mediaInfo, requireTranslate, databaseResult.second, sysLang);
            currentResult = getActiveLyricFromDatabaseByOriginId(databaseResult.second);
        } else {
            GetResult.getInstance().notifyResult(new GetResult.Data(mediaInfo, currentResult));
            return LyricUtils.parseLyric(currentResult);
        }

        if (currentResult == null && (!detector.hasKana(mediaInfo.getTitle()) && detector.hasLatin(mediaInfo.getTitle()))) {
            try {
                ILrcProvider.MediaInfo hiraganaMediaInfo = mediaInfo.clone();
                hiraganaMediaInfo.setTitle(converter.convertRomajiToHiragana(mediaInfo.getTitle()));
                if (detector.hasLatin(hiraganaMediaInfo.getTitle())) {
                    GetResult.getInstance().notifyResult(new GetResult.Data(mediaInfo, null));
                    return null;
                }
                searchLyricsResultByInfo(hiraganaMediaInfo, requireTranslate, databaseResult.second, sysLang);
                currentResult = getActiveLyricFromDatabaseByOriginId(databaseResult.second);
                if (currentResult == null) {
                    hiraganaMediaInfo.setTitle(converter.convertRomajiToKatakana(mediaInfo.getTitle()));
                    searchLyricsResultByInfo(hiraganaMediaInfo, requireTranslate, databaseResult.second, sysLang);
                    currentResult = getActiveLyricFromDatabaseByOriginId(databaseResult.second);
                }
            } catch (CloneNotSupportedException e) {
                e.fillInStackTrace();
            }
        }


        if (currentResult != null) {
            currentResult.mDataOrigin = ILrcProvider.DataOrigin.INTERNET;
            GetResult.getInstance().notifyResult(new GetResult.Data(mediaInfo, currentResult));
            return LyricUtils.parseLyric(currentResult);
        } else {
            GetResult.getInstance().notifyResult(new GetResult.Data(mediaInfo, null));
            return null;
        }
    }

    private static void searchLyricsResultByInfo(ILrcProvider.MediaInfo mediaInfo, boolean requireTranslate, Long originId, String sysLang) {
        String bestMatchSource = null;
        long bestMatchDistance = 0;
        for (ILrcProvider provider : providers) {
            try {
                ILrcProvider.LyricResult lyricResult = provider.getLyric(mediaInfo, requireTranslate);
                if (lyricResult != null) {
                    String allLyrics;
                    if (requireTranslate && lyricResult.mTranslatedLyric != null) {
                        allLyrics = LyricUtils.getAllLyrics(false, lyricResult.mTranslatedLyric);
                    } else {
                        allLyrics = LyricUtils.getAllLyrics(false, lyricResult.mLyric);
                    }
                    if (!CheckLanguageUtil.isJapanese(allLyrics)) {
                        switch (sysLang) {
                            case "zh-CN":
                                if (requireTranslate && lyricResult.mTranslatedLyric != null) {
                                    lyricResult.mTranslatedLyric = ZhConverterUtil.toSimple(lyricResult.mTranslatedLyric);
                                } else {
                                    lyricResult.mLyric = ZhConverterUtil.toSimple(lyricResult.mLyric);
                                }
                                break;
                            case "zh-TW":
                                if (requireTranslate && lyricResult.mTranslatedLyric != null) {
                                    lyricResult.mTranslatedLyric = ZhConverterUtil.toTraditional(lyricResult.mTranslatedLyric);
                                } else {
                                    lyricResult.mLyric = ZhConverterUtil.toTraditional(lyricResult.mLyric);
                                }
                            default:
                                break;
                        }
                    }
                    ResultManager.insertResultData(originId, lyricResult);
                    if (LyricSearchUtil.isLyricContent(lyricResult.mLyric) && (bestMatchSource == null || bestMatchDistance > lyricResult.mDistance)) {
                        bestMatchSource = lyricResult.mSource;
                    }
                }
            } catch (IOException e) {
                e.fillInStackTrace();
            }
        }
        if (bestMatchSource != null) {
            Result bestMatchResult = ResultManager.getResultByOriginIdAndProvider(originId, bestMatchSource);
            ActiveManager.insertActiveLog(originId, bestMatchResult.getId());
        }
    }

    private static Pair<ILrcProvider.LyricResult, Long> getActiveLyricFromDatabase(ILrcProvider.MediaInfo mediaInfo, String packageName) {
        Long originId = OriginManager.insertOrGetMediaInfoId(mediaInfo, packageName);
        ILrcProvider.LyricResult lyricResult = getActiveLyricFromDatabaseByOriginId(originId);
        return new Pair<>(lyricResult, originId);
    }

    private static ILrcProvider.LyricResult getActiveLyricFromDatabaseByOriginId(Long originId) {
        Long resultId = ActiveManager.getResultIdByOriginId(originId);
        if (resultId == null) {return null;}
        return new ILrcProvider.LyricResult(ResultManager.getResultDataById(resultId));
    }
}
