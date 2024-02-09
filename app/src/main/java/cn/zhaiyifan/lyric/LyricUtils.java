package cn.zhaiyifan.lyric;

import android.media.MediaMetadata;
import android.text.TextUtils;
import android.util.Log;

import cn.zhaiyifan.lyric.model.Lyric;
import cn.zhaiyifan.lyric.model.Lyric.Sentence;
import statusbar.finder.provider.ILrcProvider;

import java.io.*;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LyricUtils {
    private static final String TAG = LyricUtils.class.getSimpleName();


//    public static Lyric parseLyric(InputStream inputStream, String Encoding) {
//        Lyric lyric = new Lyric();
//        try {
//            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, Encoding));
//            String line;
//            while ((line = br.readLine()) != null) {
//                parseLine(line, lyric);
//            }
//            Collections.sort(lyric.sentenceList, new Lyric.SentenceComparator());
//        } catch (IOException e) {
//            e.fillInStackTrace();
//        }
//        return lyric;
//    }

    public static String getAllLyrics(boolean newLine, String lyric) {
        StringBuilder lyricsBuilder = new StringBuilder();
        String[] lines = lyric.split("\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("[") && trimmedLine.contains("]")) {
                int endIndex = trimmedLine.indexOf("]");
                String lyricText = trimmedLine.substring(endIndex + 1).trim();
                lyricsBuilder.append(lyricText).append("\n");
            }
        }
        if (newLine){
            return lyricsBuilder.toString();
        } else {
            return lyricsBuilder.toString().replace("\n", "");
        }
    }

    @Deprecated
    public static Lyric parseLyric(ILrcProvider.LyricResult lyricResult, MediaMetadata mediaMetadata) {
        return parseLyric(lyricResult, new ILrcProvider.MediaInfo(mediaMetadata));
    }

    public static Lyric parseLyric(ILrcProvider.LyricResult lyricResult, ILrcProvider.MediaInfo mediaInfo) {
        Lyric lyric = new Lyric();
        try {
            BufferedReader br = new BufferedReader(new StringReader(lyricResult.mLyric));
            String line;
            while ((line = br.readLine()) != null) {
                parseLine(lyric.sentenceList, line, lyric);
            }
            lyric.sentenceList.sort(new Lyric.SentenceComparator());
        } catch (IOException e) {
            e.fillInStackTrace();
        }
        if (lyricResult.mTranslatedLyric != null) {
            try {
                BufferedReader tbr = new BufferedReader(new StringReader(lyricResult.mTranslatedLyric));
                String transLine;
                while ((transLine = tbr.readLine()) != null) {
                    parseLine(lyric.transSentenceList, transLine, lyric);
                }
                lyric.transSentenceList.sort(new Lyric.SentenceComparator());
            } catch (IOException e) {
                e.fillInStackTrace();
            }
        }
        lyric.title = mediaInfo.getTitle();
        lyric.artist = mediaInfo.getArtist();
        lyric.album = mediaInfo.getAlbum();
        lyric.length = mediaInfo.getDuration();
        lyric.offset = lyricResult.mOffset;
        return lyric;
    }

    /**
     * Save lyric to local app directory
     *
     * @return Saved destination. Null if failed.
     */
    public static String saveLyric(Lyric lyric) {
        return "";
    }

    /**
     * Get sentence according to timestamp.
     */
    public static Sentence getSentence(List<Sentence> lyricList, long ts) {
        return getSentence(lyricList, ts, 0);
    }

    /**
     * Get sentence according to timestamp and current index.
     */
    public static Sentence getSentence(List<Sentence> lyricList, long ts, int index) {
        return getSentence(lyricList, ts, index, 0);
    }

    /**
     * Get sentence according to timestamp, current index, offset.
     */
    public static Sentence getSentence(List<Sentence> lyricList, long ts, int index, int offset) {
        int found = getSentenceIndex(lyricList, ts, index, offset);
        if (found == -1)
            return null;
        return lyricList.get(found);
    }

    /**
     * Get current index of sentence list.
     *
     * @param lyricList<Sentence>  Lyric file.
     * @param ts     Current timestamp.
     * @param index  Current index.
     * @param offset Lyric offset.
     * @return current sentence index, -1 if before first, -2 if not found.
     */
    public static int getSentenceIndex(List<Sentence> lyricList, long ts, int index, int offset) {
        if (lyricList.isEmpty() || ts < 0 || index < -1) {
            Log.d(TAG, "-1");
            return -1;
        }

        if (index >= lyricList.size())
            index = lyricList.size() - 1;
        if (index == -1)
            index = 0;

        int found = -2;

        if (lyricList.get(index).fromTime + offset > ts) {
            for (int i = index; i > -1; --i) {
                if (lyricList.get(i).fromTime + offset <= ts) {
                    found = i;
                    break;
                }
            }
            // First line of lyric is bigger than starting time.
            if (found == -2)
                found = -1;
        } else {
            for (int i = index; i < lyricList.size() - 1; ++i) {
                // Log.d(TAG, String.format("ts: %d, offset: %d, curr_ts: %d, next_ts: %d", ts, offset, lyricList.get(i).fromTime, lyricList.get(i + 1).fromTime));
                if (lyricList.get(i + 1).fromTime + offset > ts) {
                    found = i;
                    break;
                }
            }
            // If not found, return last mLyricIndex
            if (found == -2) {
                found = lyricList.size() - 1;
            }
        }

        return found;
    }

    private static boolean parseLine(List<Sentence> sentenceList, String line, Lyric lyric) {
        int lineLength = line.length();
        line = line.trim();
        int openBracketIndex, closedBracketIndex;
        openBracketIndex = line.indexOf('[', 0);

        while (openBracketIndex != -1) {
            closedBracketIndex = line.indexOf(']', openBracketIndex);
            // (1) ']' does not exist, (2) is the first character
            if (closedBracketIndex < 1)
                return false;
            String closedTag = line.substring(openBracketIndex + 1, closedBracketIndex);
            String[] colonSplited = closedTag.split(":", 2);
            if (colonSplited.length < 2)
                return false;

            if (colonSplited[0].equalsIgnoreCase(Constants.ID_TAG_TITLE)) {
                lyric.title = colonSplited[1].trim();
            } else if (colonSplited[0].equalsIgnoreCase(Constants.ID_TAG_ARTIST)) {
                lyric.artist = colonSplited[1].trim();
            } else if (colonSplited[0].equalsIgnoreCase(Constants.ID_TAG_ALBUM)) {
                lyric.album = colonSplited[1].trim();
            } else if (colonSplited[0].equalsIgnoreCase(Constants.ID_TAG_CREATOR_LRCFILE)) {
                lyric.by = colonSplited[1].trim();
            } else if (colonSplited[0].equalsIgnoreCase(Constants.ID_TAG_CREATOR_SONGTEXT)) {
                lyric.author = colonSplited[1].trim();
            } else if (colonSplited[0].equalsIgnoreCase(Constants.ID_TAG_LENGTH)) {
                lyric.length = parseTime(colonSplited[1].trim(), lyric);
            } else if (colonSplited[0].equalsIgnoreCase(Constants.ID_TAG_OFFSET)) {
                lyric.offset = parseOffset(colonSplited[1].trim());
            } else {
                if (Character.isDigit(colonSplited[0].charAt(0))) {
                    List<Long> timestampList = new LinkedList<Long>();
                    long time = parseTime(closedTag, lyric);
                    if (time != -1) {
                        timestampList.add(time);
                    }
                    //Log.d(TAG, line);
                    // We may have line like [01:38.33][01:44.01][03:22.05]Test Test
                    // [03:55.00]
                    while ((lineLength > closedBracketIndex + 2)
                            && (line.charAt(closedBracketIndex + 1) == '[')) {
                        //Log.d(TAG, String.valueOf(closedBracketIndex));
                        int nextOpenBracketIndex = closedBracketIndex + 1;
                        int nextClosedBracketIndex = line.indexOf(']', nextOpenBracketIndex + 1);
                        time = parseTime(line.substring(nextOpenBracketIndex + 1, nextClosedBracketIndex), lyric);
                        if (time != -1) {
                            timestampList.add(time);
                        }
                        closedBracketIndex = nextClosedBracketIndex;
                    }

                    String content = line.substring(closedBracketIndex + 1, line.length());
                    for (long timestamp : timestampList) {
                        lyric.addSentence(sentenceList, content, timestamp);
                    }
                } else {
                    // Ignore unknown tag
                    return true;
                }
            }
            // We may have line like [00:53.60]On a dark [00:54.85]desert highway
            openBracketIndex = line.indexOf('[', closedBracketIndex + 1);
        }
        return true;
    }

    /**
     * 把如00:00.00这样的字符串转化成 毫秒数的时间，比如 01:10.34就是一分钟加上10秒再加上340毫秒 也就是返回70340毫秒
     *
     * @param time 字符串的时间
     * @return 此时间表示的毫秒
     */
    private static long parseTime(String time, Lyric lyric) {
        String[] ss = time.split("\\:|\\.");
        // 如果 是两位以后，就非法了
        if (ss.length < 2) {
            return -1;
        } else if (ss.length == 2) {// 如果正好两位，就算分秒
            try {
                // 先看有没有一个是记录了整体偏移量的
                if (lyric.offset == 0 && ss[0].equalsIgnoreCase("offset")) {
                    lyric.offset = Integer.parseInt(ss[1]);
                    System.err.println("整体的偏移量：" + lyric.offset);
                    return -1;
                }
                int min = Integer.parseInt(ss[0]);
                int sec = Integer.parseInt(ss[1]);
                if (min < 0 || sec < 0 || sec >= 60) {
                    throw new RuntimeException("数字不合法!");
                }
                // System.out.println("time" + (min * 60 + sec) * 1000L);
                return (min * 60 + sec) * 1000L;
            } catch (Exception exe) {
                return -1;
            }
        } else if (ss.length == 3) {// 如果正好三位，就算分秒，毫秒
            try {
                int min = Integer.parseInt(ss[0]);
                int sec = Integer.parseInt(ss[1]);
                int mm = Integer.parseInt(ss[2]);
                if (min < 0 || sec < 0 || sec >= 60 || mm < 0 || mm > 999) {
                    throw new RuntimeException("数字不合法!");
                }
                // System.out.println("time" + (min * 60 + sec) * 1000L + mm);
                return (min * 60 + sec) * 1000L + mm;
            } catch (Exception exe) {
                return -1;
            }
        } else {// 否则也非法
            return -1;
        }
    }

    /**
     * 分析出整体的偏移量
     *
     * @param str 包含内容的字符串
     * @return 偏移量，当分析不出来，则返回最大的正数
     */
    private static int parseOffset(String str) {
        if (str.equalsIgnoreCase("0"))
            return 0;
        String[] ss = str.split("\\:");
        if (ss.length == 2) {
            if (ss[0].equalsIgnoreCase("offset")) {
                int os = Integer.parseInt(ss[1]);
                Log.i(TAG, "total offset：" + os);
                return os;
            } else {
                return Integer.MAX_VALUE;
            }
        } else {
            return Integer.MAX_VALUE;
        }
    }
}
