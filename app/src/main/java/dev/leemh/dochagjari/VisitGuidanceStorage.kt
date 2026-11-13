package dev.leemh.dochagjari

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

interface GuidanceStorage {
    fun installCatalog(json: String): FacilityCatalog
    fun search(version: String, query: String): List<IndoorDestination>
    fun destination(version: String, id: String): IndoorDestination?
    fun currentVisit(): VisitRecord?
    fun beginOrResume(version: String, destinationId: String): VisitRecord
    fun saveVisit(record: VisitRecord)
}

/** Versioned public catalog and private visits. Imports never delete prior snapshots or visits. */
class SqliteGuidanceStorage(context: Context, name: String = "visit-guidance.db") :
    SQLiteOpenHelper(context.applicationContext, name, null, 1), GuidanceStorage {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE catalog_versions (version TEXT PRIMARY KEY, sha256 TEXT NOT NULL)")
        db.execSQL("CREATE TABLE destinations (version TEXT NOT NULL REFERENCES catalog_versions(version), id TEXT NOT NULL, payload TEXT NOT NULL, PRIMARY KEY(version,id))")
        db.execSQL("CREATE TABLE destination_aliases (version TEXT NOT NULL, alias TEXT NOT NULL, destination_id TEXT NOT NULL, PRIMARY KEY(version,alias,destination_id), FOREIGN KEY(version,destination_id) REFERENCES destinations(version,id))")
        db.execSQL("CREATE TABLE visits (id TEXT PRIMARY KEY, payload TEXT NOT NULL)")
        db.execSQL("CREATE TABLE current_visit (slot INTEGER PRIMARY KEY CHECK(slot=1), visit_id TEXT NOT NULL REFERENCES visits(id))")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw SQLiteException("Explicit non-destructive guidance migration required: $oldVersion to $newVersion")
    }
    @Synchronized override fun installCatalog(json: String): FacilityCatalog {
        val parsed = VisitGuidanceCodec.catalog(json)
        val hash = MessageDigest.getInstance("SHA-256").digest(json.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val previous = db.rawQuery("SELECT sha256 FROM catalog_versions WHERE version=?", arrayOf(parsed.version)).use { if (it.moveToFirst()) it.getString(0) else null }
            if (previous != null) require(previous == hash) { "Catalog version content changed; publish a new version" }
            else {
                db.insertOrThrow("catalog_versions", null, ContentValues().apply { put("version", parsed.version); put("sha256", hash) })
                val raw = JSONObject(json).getJSONArray("destinations")
                parsed.destinations.forEachIndexed { index, item ->
                    db.insertOrThrow("destinations", null, ContentValues().apply { put("version", parsed.version); put("id", item.id); put("payload", raw.getJSONObject(index).toString()) })
                    item.aliases.map(VisitGuidanceCodec::normalize).distinct().forEach { alias ->
                        db.insertOrThrow("destination_aliases", null, ContentValues().apply { put("version", parsed.version); put("alias", alias); put("destination_id", item.id) })
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        return parsed
    }
    @Synchronized override fun search(version: String, query: String): List<IndoorDestination> {
        val normalized = VisitGuidanceCodec.normalize(query)
        if (normalized.isEmpty()) return emptyList()
        return readableDatabase.rawQuery("SELECT d.payload FROM destinations d JOIN destination_aliases a ON a.version=d.version AND a.destination_id=d.id WHERE a.version=? AND a.alias=? ORDER BY d.id", arrayOf(version, normalized)).use { cursor ->
            buildList { while (cursor.moveToNext()) add(VisitGuidanceCodec.destination(JSONObject(cursor.getString(0)))) }
        }
    }
    @Synchronized override fun destination(version: String, id: String): IndoorDestination? = readableDatabase.rawQuery(
        "SELECT payload FROM destinations WHERE version=? AND id=?", arrayOf(version, id)
    ).use { if (it.moveToFirst()) VisitGuidanceCodec.destination(JSONObject(it.getString(0))) else null }
    @Synchronized override fun currentVisit(): VisitRecord? = readableDatabase.rawQuery(
        "SELECT v.payload FROM visits v JOIN current_visit c ON v.id=c.visit_id WHERE c.slot=1", null
    ).use { if (it.moveToFirst()) VisitGuidanceCodec.decode(it.getString(0)) else null }
    @Synchronized override fun beginOrResume(version: String, destinationId: String): VisitRecord {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val current = currentVisit()
            val record = if (current != null) {
                require(current.destinationId == destinationId) { "Another visit exists; explicit new-visit flow required" }
                // Preserve the original dataset and visibility, including a dismissed visit.
                current
            } else {
                requireNotNull(destination(version, destinationId)) { "Unknown catalog destination" }
                VisitRecord(UUID.randomUUID().toString(), version, destinationId, createdAt = System.currentTimeMillis()).also {
                    db.insertOrThrow("visits", null, ContentValues().apply { put("id", it.id); put("payload", VisitGuidanceCodec.encode(it)) })
                    db.insertOrThrow("current_visit", null, ContentValues().apply { put("slot", 1); put("visit_id", it.id) })
                }
            }
            db.setTransactionSuccessful()
            return record
        } finally { db.endTransaction() }
    }
    @Synchronized override fun saveVisit(record: VisitRecord) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val previous = db.rawQuery("SELECT payload FROM visits WHERE id=?", arrayOf(record.id)).use {
                require(it.moveToFirst()) { "Cannot update a missing visit" }; VisitGuidanceCodec.decode(it.getString(0))
            }
            require(record.catalogVersion == previous.catalogVersion && record.destinationId == previous.destinationId &&
                record.createdAt == previous.createdAt && record.originLabel == previous.originLabel) { "Visit identity is immutable" }
            check(db.update("visits", ContentValues().apply { put("payload", VisitGuidanceCodec.encode(record)) }, "id=?", arrayOf(record.id)) == 1)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}
