package statusbar.finder;

import android.content.Context;
import android.media.MediaMetadata;
import androidx.core.util.Pair;
import cn.zhaiyifan.lyric.LyricUtils;
import cn.zhaiyifan.lyric.model.Lyric;
import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.moji4j.MojiConverter;
import com.moji4j.MojiDetector;
import statusbar.finder.data.DataOrigin;
import statusbar.finder.data.LyricResult;
import statusbar.finder.data.MediaInfo;
import statusbar.finder.livedata.LyricsResultChange;
import statusbar.finder.misc.CheckLanguageUtil;
import statusbar.finder.provider.ILrcProvider;
import statusbar.finder.provider.KugouProvider;
import statusbar.finder.provider.MusixMatchProvider;
import statusbar.finder.provider.NeteaseProvider;
import statusbar.finder.provider.QQMusicProvider;
import statusbar.finder.provider.utils.LyricSearchUtil;
import statusbar.finder.sql.ActiveManager;
import statusbar.finder.sql.OriginManager;
import statusbar.finder.sql.ResManager;

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

    public static Lyric getLyric(Context context, MediaMetadata mediaMetadata, String sysLang, String packageName) {
        return getLyric(context, new MediaInfo(mediaMetadata), sysLang, packageName);
    }

    public static Lyric getLyric(Context context, MediaInfo mediaInfo, String sysLang, String packageName) {
        DatabaseHelper.INSTANCE.init(context);
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
        Pair<LyricResult, Long> databaseResult = getActiveLyricFromDatabase(mediaInfo, packageName);
        LyricResult currentResult = databaseResult.first;

        if (currentResult == null) {
            searchLyricsResultByInfo(mediaInfo, databaseResult.second, sysLang);
            currentResult = getActiveLyricFromDatabaseByOriginId(databaseResult.second);
        } else {
            LyricsResultChange.Companion.getInstance().notifyResult(new LyricsResultChange.Data(mediaInfo, currentResult));
            return LyricUtils.parseLyric(currentResult);
        }

        if (currentResult == null && (!detector.hasKana(mediaInfo.getTitle()) && detector.hasLatin(mediaInfo.getTitle()))) {
            MediaInfo hiraganaMediaInfo = mediaInfo.clone();
            hiraganaMediaInfo.setTitle(converter.convertRomajiToHiragana(mediaInfo.getTitle()));
            if (detector.hasLatin(hiraganaMediaInfo.getTitle())) {
                LyricsResultChange.Companion.getInstance().notifyResult(new LyricsResultChange.Data(mediaInfo, null));
                return null;
            }
            searchLyricsResultByInfo(hiraganaMediaInfo, databaseResult.second, sysLang);
            currentResult = getActiveLyricFromDatabaseByOriginId(databaseResult.second);
            if (currentResult == null) {
                hiraganaMediaInfo.setTitle(converter.convertRomajiToKatakana(mediaInfo.getTitle()));
                searchLyricsResultByInfo(hiraganaMediaInfo, databaseResult.second, sysLang);
                currentResult = getActiveLyricFromDatabaseByOriginId(databaseResult.second);
            }
        }


        if (currentResult != null) {
            currentResult.setDataOrigin(DataOrigin.INTERNET);
            LyricsResultChange.Companion.getInstance().notifyResult(new LyricsResultChange.Data(mediaInfo, currentResult));
            return LyricUtils.parseLyric(currentResult);
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
                    ResManager.INSTANCE.insertResData(originId, lyricResult);
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
            Res bestMatchResult = ResManager.INSTANCE.getResByOriginIdAndProvider(originId, bestMatchSource);
            assert bestMatchResult != null;
            ActiveManager.INSTANCE.insertActiveLog(originId, bestMatchResult.getId());
        }
    }

    private static Pair<LyricResult, Long> getActiveLyricFromDatabase(MediaInfo mediaInfo, String packageName) {
        Long originId = OriginManager.insertOrGetMediaInfoId(mediaInfo, packageName);
        LyricResult lyricResult = getActiveLyricFromDatabaseByOriginId(originId);
        return new Pair<>(lyricResult, originId);
    }

    private static LyricResult getActiveLyricFromDatabaseByOriginId(Long originId) {
        Long resultId = ActiveManager.INSTANCE.getResultIdByOriginId(originId);
        if (resultId == null) {return null;}
        Res res = ResManager.INSTANCE.getResById(resultId);
        if (res == null) {return null;}
        return new LyricResult(res);
    }
}
