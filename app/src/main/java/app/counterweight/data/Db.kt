package app.counterweight.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import app.counterweight.domain.Engraved
import app.counterweight.domain.Laid
import app.counterweight.domain.Mark
import app.counterweight.domain.MarkKind
import app.counterweight.domain.Outcome
import app.counterweight.domain.Sphere
import app.counterweight.domain.Stone

class Converters {
    @TypeConverter fun sphere(s: Sphere): String = s.name
    @TypeConverter fun sphere(s: String): Sphere = Sphere.entries.firstOrNull { it.name == s } ?: Sphere.FUTURE
    @TypeConverter fun outcome(o: Outcome?): String? = o?.name
    @TypeConverter fun outcome(s: String?): Outcome? = s?.let { n -> Outcome.entries.firstOrNull { it.name == n } }
    @TypeConverter fun mark(k: MarkKind): String = k.name
    @TypeConverter fun mark(s: String): MarkKind = MarkKind.entries.firstOrNull { it.name == s } ?: MarkKind.KEEP
}

@Dao
interface ChamberDao {
    @Query("SELECT * FROM stone ORDER BY at, id") suspend fun stones(): List<Stone>
    @Query("SELECT * FROM laid ORDER BY at, id") suspend fun laid(): List<Laid>
    @Query("SELECT * FROM mark ORDER BY at") suspend fun marks(): List<Mark>
    @Query("SELECT * FROM engraved ORDER BY id") suspend fun engraved(): List<Engraved>

    @Insert suspend fun insert(s: Stone): Long
    @Update suspend fun update(s: Stone)
    @Insert suspend fun insert(l: Laid): Long
    @Upsert suspend fun mark(m: Mark)
    @Query("DELETE FROM mark WHERE weight = :weight AND kind = :kind") suspend fun unmark(weight: String, kind: MarkKind)
    @Insert suspend fun insert(e: Engraved): Long
    @Update suspend fun update(e: Engraved)
    @Query("DELETE FROM engraved WHERE id = :id") suspend fun deleteEngraved(id: Long)
    @Query("DELETE FROM stone WHERE id = :id") suspend fun deleteStone(id: Long)
    @Query("DELETE FROM laid WHERE stone = :id") suspend fun deleteLaid(id: Long)
}

@Database(entities = [Stone::class, Laid::class, Mark::class, Engraved::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class ChamberDb : RoomDatabase() {
    abstract fun dao(): ChamberDao

    companion object {
        fun create(c: Context): ChamberDb = Room.databaseBuilder(c, ChamberDb::class.java, "chamber.db").build()
        fun memory(c: Context): ChamberDb = Room.inMemoryDatabaseBuilder(c, ChamberDb::class.java).build()
    }
}
