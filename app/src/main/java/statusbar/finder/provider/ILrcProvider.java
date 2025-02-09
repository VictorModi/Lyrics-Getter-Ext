package statusbar.finder.provider;

import android.media.MediaMetadata;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import statusbar.finder.Origin;
import statusbar.finder.Res;
import statusbar.finder.data.LyricResult;
import statusbar.finder.data.MediaInfo;

import java.io.IOException;

public interface ILrcProvider {
    @Deprecated LyricResult getLyric(MediaMetadata data) throws IOException;
    LyricResult getLyric(MediaInfo mediaInfo) throws IOException;
}
