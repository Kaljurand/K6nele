package ee.ioc.phon.android.speak.model

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ServiceEntity::class, ComboEntity::class, ComboListEntity::class],
    version = 1
)
abstract class ComboDatabase : RoomDatabase() {
    abstract fun serviceDao(): ServiceDao
    abstract fun comboDao(): ComboDao
    abstract fun comboListDao(): ComboListDao
}
