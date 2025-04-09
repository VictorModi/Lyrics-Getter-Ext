package statusbar.finder.data

import android.os.Parcel
import android.os.Parcelable
import com.hchen.superlyricapi.SuperLyricExtra

/**
 * LyricGetterExt - statusbar.finder.data
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/4/9 23:29
 */
data class ExtraData(
    var rawString: String = ""
) : SuperLyricExtra(), Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString() ?: "")

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(rawString)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ExtraData> {
        override fun createFromParcel(parcel: Parcel): ExtraData {
            return ExtraData(parcel)
        }

        override fun newArray(size: Int): Array<ExtraData?> {
            return arrayOfNulls(size)
        }
    }
}
