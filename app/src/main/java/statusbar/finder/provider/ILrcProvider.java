package statusbar.finder.provider;

import android.media.MediaMetadata;
import statusbar.finder.data.model.LyricResult;
import statusbar.finder.data.model.MediaInfo;

import java.io.IOException;

public interface ILrcProvider {
    @Deprecated LyricResult getLyric(MediaMetadata data) throws IOException;
    LyricResult getLyric(MediaInfo mediaInfo) throws IOException;
}
