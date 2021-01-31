package ee.ioc.phon.android.speak.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// TODO: does the ID need to be Long?
// TODO: declare name to be unique (and index by it?)
@Entity(tableName = "rewrite_list", indices = arrayOf(Index(value = ["name"], unique = true)))
data class RewriteList(
        val name: String,
        val isEnabled: Boolean = false) {
    @PrimaryKey(autoGenerate = true)
    var rewriteListId: Long = 0
}