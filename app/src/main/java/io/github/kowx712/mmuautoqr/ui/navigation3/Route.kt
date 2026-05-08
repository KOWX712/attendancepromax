package io.github.kowx712.mmuautoqr.ui.navigation3

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey, Parcelable {
    @Serializable
    @Parcelize
    data object Main : Route
}
