package ee.ioc.phon.android.speak.model

import androidx.room.Embedded
import androidx.room.Relation

data class RewriteListWithRules(
        @Embedded val user: RewriteList,
        @Relation(
                parentColumn = "rewriteListId",
                entityColumn = "ownerId"
        )
        val rewriteRules: List<RewriteRule>
)