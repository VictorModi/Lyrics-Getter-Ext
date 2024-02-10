package statusbar.finder.broadcast;

import androidx.lifecycle.LiveData;

/**
 * LyricGetterExt - statusbar.finder.broadcast
 * &#064;description: some desc
 * &#064;author: VictorModi
 * &#064;email: victormodi@outlook.com
 * &#064;date: 2024/2/10 12:33
 */
public class AppsChangedLiveData extends LiveData<Void> {
    private static AppsChangedLiveData instance;

    public static AppsChangedLiveData getInstance() {
        if (instance == null) {
            instance = new AppsChangedLiveData();
        }
        return instance;
    }

    public void notifyChange() {
        setValue(null);
    }
}
