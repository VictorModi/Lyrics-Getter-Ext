package cn.zhaiyifan.lyric.model;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Lyric {
    private static final String TAG = Lyric.class.getSimpleName();

    public String title; // 歌曲名称
    public String artist; // 歌曲作家
    public String album; // 歌曲专辑
    public String by;
    public String author;
    public int offset; // 偏移值
    public long length; // 歌曲长度
    public List<Sentence> sentenceList = new ArrayList<Sentence>(100);
    public List<Sentence> transSentenceList = new ArrayList<Sentence>(100);

    @NotNull
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Title: ").append(title).append("\n")
                .append("Artist: ").append(artist).append("\n")
                .append("Album: ").append(album).append("\n")
                .append("By: ").append(by).append("\n")
                .append("Author: ").append(author).append("\n")
                .append("Length: ").append(length).append("\n")
                .append("Offset: ").append(offset).append("\n");
        if (sentenceList != null) {
            for (Sentence sentence : sentenceList) {
                stringBuilder.append(sentence.toString()).append("\n");
            }
        }
        if (transSentenceList != null) {
            stringBuilder.append ("--- Translate Lyrics ---\n");
            for (Sentence sentence : transSentenceList) {
                stringBuilder.append(sentence.toString()).append("\n");
            }
        }
        return stringBuilder.toString();
    }

    public void addSentence(List<Sentence> sentenceList,String content, long time) {
        sentenceList.add(new Sentence(content, time));
    }

    public static class SentenceComparator implements Comparator<Sentence> {
        @Override
        public int compare(Sentence sent1, Sentence sent2) {
            return (int) (sent1.fromTime - sent2.fromTime);
        }
    }

    public static class Sentence {
        public String content;
        public long fromTime;

        public Sentence(String content, long fromTime) {
            this.content = content;
            this.fromTime = fromTime;
        }

        @NotNull
        public String toString() {
            return fromTime + ": " + content;
        }
    }
}