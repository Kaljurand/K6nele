package ee.ioc.phon.android.speak

import androidx.room.TypeConverter
import java.text.ParseException
import java.util.regex.Pattern

object Converters {
    @TypeConverter
    @JvmStatic
    fun stringToPattern(str: String?): Pattern? {
        return if (str == null) null else try {
            Pattern.compile(str)
        } catch (e: ParseException) {
            null
        }
    }

    @TypeConverter
    @JvmStatic
    fun patternToString(pattern: Pattern?): String? {
        return pattern?.pattern()
    }
}