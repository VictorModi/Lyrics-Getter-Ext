package statusbar.finder.livedatas;

import android.util.Pair;
import androidx.lifecycle.LiveData;
import statusbar.finder.provider.ILrcProvider;

/**
 * LyricGetterExt - statusbar.finder.livedatas
 *
 * @description: Coming soon.
 * @author: VictorModi
 * @email: victormodi@outlook.com
 * @date: 2024/2/14 13:32
 */
public class GetResult extends LiveData<Pair<ILrcProvider.MediaInfo, ILrcProvider.LyricResult>> {
    private static GetResult instance;

    public static GetResult getInstance() {
        if (instance == null) {
            instance = new GetResult();
        }
        return instance;
    }

    public void notifyResult(Pair<ILrcProvider.MediaInfo, ILrcProvider.LyricResult> data) {
        postValue(data);
    }
}
