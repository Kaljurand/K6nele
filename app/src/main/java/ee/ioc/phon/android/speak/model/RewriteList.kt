package ee.ioc.phon.android.speak.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rewrite_list")
data class RewriteList(
        @PrimaryKey val rewriteListId: Long,
        val name: String,
        val isEnabled: Boolean
)