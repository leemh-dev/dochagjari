package dev.leemh.dochagjari

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class AppServices(val scope: CoroutineScope, val outbox: ArrivalOutbox, val profiles: ProfileRepository, val guidance: VisitGuidanceRepository? = null) {
    companion object {
        @Volatile private var instance: AppServices? = null
        fun get(application: Application): AppServices = instance ?: synchronized(this) {
            instance ?: create(application).also { instance = it }
        }
        private fun create(application: Application): AppServices {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val preferences = application.getSharedPreferences("session-journal", 0)
            val journal = FileArrivalJournal(File(application.filesDir, "arrival/outbox.json")) { preferences.getString("session", null) }
            val outbox = ArrivalOutbox(journal, HttpArrivalTransport())
            return AppServices(scope, outbox, ProfileRepository(DataStoreProfileStorage.create(application, scope), scope),
                VisitGuidanceRepository(SqliteGuidanceStorage(application), scope) { application.assets.open("visit-catalog.json").bufferedReader().use { it.readText() } }).also {
                scope.launch { outbox.recover("") }
            }
        }
    }
}
