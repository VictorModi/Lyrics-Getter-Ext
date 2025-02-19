package cn.zhaiyifan.lyric.model;

import org.jetbrains.annotations.NotNull;
import statusbar.finder.data.model.LyricResult;
import statusbar.finder.data.model.MediaInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Lyric {
    private static final String TAG = Lyric.class.getSimpleName();

    public String title; // 歌曲名称
    public String artist; // 歌曲作家
    public String album; // 歌曲专辑
    public String by;
    public String author;
    public long offset; // 偏移值
    public long length; // 歌曲长度
    public List<Sentence> sentenceList = new ArrayList<>(100);
    public List<Sentence> translatedSentenceList = new ArrayList<>(100);
    public MediaInfo originalMediaInfo;
    public String packageName;
    public LyricResult lyricResult;

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
        if (translatedSentenceList != null) {
            stringBuilder.append ("--- Translate Lyrics ---\n");
            for (Sentence sentence : translatedSentenceList) {
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

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Sentence sentence = (Sentence) o;
            return fromTime == sentence.fromTime && Objects.equals(content, sentence.content);
        }

        @Override
        public int hashCode() {
            return Objects.hash(content, fromTime);
        }

        @NotNull
        public String toString() {
            return fromTime + ": " + content;
        }
    }
}
