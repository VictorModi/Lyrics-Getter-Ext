package statusbar.finder.provider;

import android.media.MediaMetadata;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import statusbar.finder.Origin;
import statusbar.finder.Result;

import java.io.IOException;

public interface ILrcProvider {
    @Deprecated LyricResult getLyric(MediaMetadata data, boolean requireTranslate) throws IOException;
    LyricResult getLyric(MediaInfo mediaInfo, boolean requireTranslate) throws IOException;

    class LyricResult {
        public String mLyric;
        public String mTranslatedLyric;
        public long mDistance = 0;
        public String mSource = "Local";
        public int mOffset = 0;
        public MediaInfo mResultInfo;
        public DataOrigin mDataOrigin = DataOrigin.UNDEFINED;

        public String toSting() {
            return "Distance: " + mDistance + "\n" +
                    "Source: " + mSource + "\n" +
                    "Offset: " + mOffset + "\n" +
                    "Lyric: " + mLyric + "\n" +
                    "TranslatedLyric: " + mTranslatedLyric + "\n" +
                    "ResultInfo: " + mResultInfo;
        }

        public LyricResult(Result result) {
            this.mLyric = result.getLyric();
            this.mTranslatedLyric = result.getTranslated_lyric();
            this.mDistance = result.getDistance() != null ? result.getDistance() : -1;
            this.mOffset = Math.toIntExact(result.getLyric_offset());
            this.mSource = result.getProvider();
            this.mDataOrigin = DataOrigin.DATABASE;
            this.mResultInfo = new MediaInfo(
                    result.getTitle() != null ? result.getTitle() : "",
                    result.getArtist() != null ? result.getArtist() : "",
                    result.getAlbum() != null ? result.getAlbum() : "",
                    result.getDistance() != null ? result.getDistance() : -1,
                    -1
            );
        }

        public LyricResult() {}
    }

    enum DataOrigin {
        UNDEFINED,
        INTERNET,
        DATABASE;

        public String getCapitalizedName() {
            // 将枚举值转换为首字母大写的字符串
            String enumName = this.name().toLowerCase();
            return Character.toUpperCase(enumName.charAt(0)) + enumName.substring(1);
        }
    }

    @Data
    class MediaInfo {
        private String title;
        private String artist;
        private String album;
        private long duration;
        private long distance;

        public MediaInfo(String title, String artist, String album, long distance, long duration) {
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.distance = distance;
            this.duration = duration;
        }

        public MediaInfo(MediaMetadata mediaMetadata) {
            this.title = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            this.artist = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            this.album = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
            this.duration = mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            this.distance = -1;

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

        public MediaInfo(Origin originData) {
            this.title = originData.getTitle();
            this.artist = originData.getArtist();
            this.album = originData.getAlbum();
            this.duration = originData.getDuration() != null ? originData.getDuration() : -1;
            this.distance = -1;
        }

        @NotNull
        @Override
        public MediaInfo clone() throws CloneNotSupportedException {
            return (MediaInfo) super.clone();
        }
    }

}
