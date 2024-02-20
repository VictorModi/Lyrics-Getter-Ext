package statusbar.finder.livedatas;

import androidx.lifecycle.LiveData;

/**
 * LyricGetterExt - statusbar.finder.livedatas
 * @description: LiveData for notifying observers about changes in lyrics.
 * This LiveData emits a Pair object containing the updated lyric content and its position.
 * Singleton class, use getInstance() method to obtain the instance.
 * @author: VictorModi
 * @email: victormodi@outlook.com
 * @date: 2024/2/13 18:09
 */
public class LyricsChanged extends LiveData<LyricsChanged.Data> {
    private static LyricsChanged instance;

    public static LyricsChanged getInstance() {
        return instance != null ? instance : (instance = new LyricsChanged());
    }

    public void notifyLyrics(LyricsChanged.Data data) {
        postValue(data);
    }

    @lombok.Data
    public static class Data {
        private String lyric;
        private String translatedLyric;
        private Integer delay;

        public Data(String lyric, String translatedLyric, Integer delay) {
            this.lyric = lyric;
            this.translatedLyric = translatedLyric;
            this.delay = delay;
        }
    }
}
