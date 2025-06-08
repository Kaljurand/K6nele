package ee.ioc.phon.android.speak.model

import androidx.room.*

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: String, // ComponentName flattened string
    val name: String,
    val description: String?,
    val iconRes: String?, // Could be a URI or resource name
    val settingsActivity: String?
)

@Entity(tableName = "combos")
data class ComboEntity(
    @PrimaryKey(autoGenerate = true) val comboId: Long = 0,
    val serviceId: String, // Foreign key to ServiceEntity
    val shortLabel: String,
    val longLabel: String,
    val tinyLabel: String,
    val enabled: Boolean = true,
    val inputLanguage: String?,
    val extras: String?, // JSON-encoded map of RecognizerIntent and service-specific extras
    val comboListType: String // "IME" or "PANEL"
)

@Entity(tableName = "combo_lists")
data class ComboListEntity(
    @PrimaryKey val type: String, // "IME" or "PANEL"
    val order: String // JSON-encoded list of comboIds in order
)
