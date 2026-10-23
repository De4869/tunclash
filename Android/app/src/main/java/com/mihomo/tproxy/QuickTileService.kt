package com.mihomo.tproxy

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.mihomo.tproxy.core.CoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Quick Settings tile: one tap to start / stop the proxy without opening the app.
 * TileService already runs in the app process, so it can use the same root runtime.
 */
class QuickTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        refreshTile()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
    }

    override fun onClick() {
        scope.launch {
            val core = CoreManager(applicationContext)
            runCatching { if (core.status()) core.stop() else core.start() }
            delay(800)
            refreshTile()
        }
    }

    private fun refreshTile() {
        scope.launch {
            val running = runCatching { CoreManager(applicationContext).status() }.getOrDefault(false)
            withContext(Dispatchers.Main) {
                qsTile?.let { tile ->
                    tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    tile.label = "Mihomo"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tile.subtitle = if (running) "已连接" else "未运行"
                    }
                    tile.updateTile()
                }
            }
        }
    }
}
