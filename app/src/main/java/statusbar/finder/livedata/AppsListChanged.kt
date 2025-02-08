package statusbar.finder.livedata;

import androidx.lifecycle.LiveData;

/**
 * LyricGetterExt - statusbar.finder.livedata
 * @description: Used for notifying observers about changes in the list of apps.
 * This LiveData emits a null value to indicate that the list of apps has been changed.
 * Singleton class, use getInstance() method to obtain the instance.
 * @author: VictorModi
 * @email: victormodi@outlook.com
 * @date: 2024/2/10 12:33
 */
public class AppsListChanged extends LiveData<Void> {
    private static AppsListChanged instance;

    public static AppsListChanged getInstance() {
        return instance != null ? instance : (instance = new AppsListChanged());
    }

    public void notifyChange() {
        setValue(null);
    }
}
