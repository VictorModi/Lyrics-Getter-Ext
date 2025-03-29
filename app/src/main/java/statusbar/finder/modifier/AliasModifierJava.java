package statusbar.finder.modifier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import statusbar.finder.Alias;
import statusbar.finder.data.model.MediaInfo;
import statusbar.finder.data.repository.AliasRepository;

public class AliasModifierJava implements Modifier {
    @Nullable
    @Override
    public MediaInfo modify(@NotNull MediaInfo mediaInfo, long originId) {
        Alias alias = AliasRepository.INSTANCE.getAlias(originId);
        if (alias != null) {
            MediaInfo updatedMediaInfo = new MediaInfo(
                    alias.getTitle() != null ? alias.getTitle() : mediaInfo.getTitle(),
                    alias.getArtist() != null ? alias.getArtist() : mediaInfo.getArtist(),
                    alias.getAlbum() != null ? alias.getAlbum() : mediaInfo.getAlbum(),
                    -1L,
                    -1L
            );
            updatedMediaInfo.setTitle(alias.getTitle() != null ? alias.getTitle() : mediaInfo.getTitle());
            updatedMediaInfo.setArtist(alias.getArtist() != null ? alias.getArtist() : mediaInfo.getArtist());
            updatedMediaInfo.setAlbum(alias.getAlbum() != null ? alias.getAlbum() : mediaInfo.getAlbum());
            return updatedMediaInfo;
        }
        return null;
    }
}
