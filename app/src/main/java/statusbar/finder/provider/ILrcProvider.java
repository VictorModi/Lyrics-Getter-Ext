package statusbar.finder.provider;

import android.media.MediaMetadata;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Locale;

public interface ILrcProvider {
    LyricResult getLyric(MediaMetadata data) throws IOException;
    LyricResult getLyric(MediaInfo mediaInfo) throws IOException;

    class LyricResult {
        public String mLyric;
        public String mTranslatedLyric;
        public long mDistance = 0;
        public String mSource = "Local";
        public int mOffset = 0;
        public MediaInfo resultInfo;

        public String toSting() {
            return "Distance: " + mDistance + "\n" +
                    "Source: " + mSource + "\n" +
                    "Offset: " + mOffset + "\n" +
                    "Lyric: " + mLyric + "\n" +
                    "TranslatedLyric: " + mTranslatedLyric + "\n" +
                    "RealInfo: " + resultInfo;
        }
    }

    @Data
    class MediaInfo {
        private String title;
        private String artist;
        private String album;
        private long duration;
        private long distance;

        public MediaInfo(String title, String artist, String album, long distance) {
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.distance = distance;
        }
//        public MediaInfo(String title, String artist, String album, long duration) {
//            this.title = title;
//            this.artist = artist;
//            this.album = album;
//            this.duration = duration;
//        }

        public MediaInfo(MediaMetadata mediaMetadata) {
            this.title = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            this.artist = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            this.album = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
            this.duration = mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION);

            if (this.title == null) {
                this.title = "";
            }
            if (this.artist == null) {
                this.artist = "";
            }
            if (this.album == null) {
                this.album = "";
            }
        }

        @NotNull
        @Override
        public MediaInfo clone() throws CloneNotSupportedException {
            return (MediaInfo) super.clone();
        }
    }

}
