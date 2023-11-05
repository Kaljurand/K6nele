package ee.ioc.phon.android.speak.database

import android.content.ComponentName
import androidx.room.TypeConverter
import java.util.Locale

class Converters {
    @TypeConverter
    fun fromComponentName(componentName: ComponentName): String {
        return componentName.flattenToShortString()
    }

    @TypeConverter
    fun fromLocale(locale: Locale): String {
        return locale.toString();
    }

    @TypeConverter
    fun toComponentName(componentName: String): ComponentName {
        return ComponentName.unflattenFromString(componentName)!!
    }

    @TypeConverter
    fun toLocale(locale: String): Locale {
        return Locale(locale)
    }
}