package ee.ioc.phon.android.speak.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "combos")
data class Combo1(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shortLabel: String = "und",
    val longLabel: String = "und",
    val componentName: String
)

@Entity(
    tableName = "key_value_pairs", foreignKeys = [ForeignKey(
        entity = Combo1::class,
        parentColumns = ["id"],
        childColumns = ["comboId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class KeyValuePair(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val key: String,
    val value: String, // Everything is mapped to a string
    val comboId: Int
)
