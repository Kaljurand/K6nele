package ee.ioc.phon.android.speak.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val shortLabel: String,
    val longLabel: String
)

@Entity(tableName = "key_value_pairs", foreignKeys = [
    ForeignKey(
        entity = Item::class,
        parentColumns = ["id"],
        childColumns = ["itemId"],
        onDelete = ForeignKey.CASCADE
    )
])
data class KeyValuePair(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val key: String,
    val value: String, // To simplify, we'll store everything as string
    val itemId: Int
)
