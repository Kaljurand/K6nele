package ee.ioc.phon.android.speak.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "combos")
data class ComboEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val comboId: String,
    val shortLabel: String,
    val longLabel: String,
    val enabled: Boolean = true,
    val position: Int = 0
)
