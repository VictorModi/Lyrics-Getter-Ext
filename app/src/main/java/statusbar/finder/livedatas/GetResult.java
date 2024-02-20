package statusbar.finder.livedatas;

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
public class GetResult extends LiveData<GetResult.Data> {
    private static GetResult instance;

    public static GetResult getInstance() {
        return instance != null ? instance : (instance = new GetResult());
    }

    public void notifyResult(GetResult.Data data) {
        postValue(data);
    }

    @lombok.Data
    public static class Data {
        private ILrcProvider.MediaInfo originInfo;
        private ILrcProvider.LyricResult result;

        public Data(ILrcProvider.MediaInfo originInfo, ILrcProvider.LyricResult result) {
            this.originInfo = originInfo;
            this.result = result;
        }
    }
}
