package statusbar.finder.utils;

import com.moji4j.MojiDetector;
import statusbar.finder.data.model.MediaInfo;

import static statusbar.finder.utils.LyricSearchUtil.getSearchKey;

public class CheckLanguageUtil {

    private static MojiDetector detector;
//    public static boolean isJapenese(String text) {
//        Set<Character.UnicodeBlock> japaneseUnicodeBlocks = new HashSet<Character.UnicodeBlock>() {{
//            add(Character.UnicodeBlock.HIRAGANA);
//            add(Character.UnicodeBlock.KATAKANA);
//            add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
//        }};
//
//        for (char c : text.toCharArray()) {
//            if (japaneseUnicodeBlocks.contains(Character.UnicodeBlock.of(c))) {
//                return true;
//            } else
//                return false;
//        }
//        return false;
//    }

    private static MojiDetector getDetector() {
        return detector != null ? detector : (detector = new MojiDetector());
    }

    public static boolean isJapanese(String str) {
        return getDetector().hasKana(str) || getDetector().hasKanji(str);
    }

    public static boolean isLatin(String str) {
        return getDetector().hasLatin(str);
    }

    public static boolean isNoJapaneseButLatin(MediaInfo mediaInfo) {
        String searchKey = getSearchKey(mediaInfo);
        return !isJapanese(searchKey) && isLatin(searchKey);
    }
}
