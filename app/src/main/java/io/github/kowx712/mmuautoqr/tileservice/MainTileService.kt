package io.github.kowx712.mmuautoqr.tileservice

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import io.github.kowx712.mmuautoqr.MainActivity

class MainTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        TileServiceCompat.startActivityAndCollapse(this, PendingIntentActivityWrapper(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT,
            true
        ))
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile
        if (tile != null) {
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }
}
