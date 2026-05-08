package io.github.kowx712.mmuautoqr.ui.navigation3

import android.os.Parcelable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.kowx712.mmuautoqr.R
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey, Parcelable {
    @Serializable
    @Parcelize
    data object Home : Route

    @Serializable
    @Parcelize
    data object Users : Route

    @Serializable
    @Parcelize
    data object Settings : Route

    val titleResId: Int
        get() = when (this) {
            Home -> R.string.bottom_nav_home
            Users -> R.string.bottom_nav_users
            Settings -> R.string.bottom_nav_settings
        }

    val filledIcon: ImageVector
        get() = when (this) {
            Home -> Icons.Filled.Home
            Users -> Icons.Filled.People
            Settings -> Icons.Filled.Settings
        }

    val outlinedIcon: ImageVector
        get() = when (this) {
            Home -> Icons.Outlined.Home
            Users -> Icons.Outlined.People
            Settings -> Icons.Outlined.Settings
        }
}
