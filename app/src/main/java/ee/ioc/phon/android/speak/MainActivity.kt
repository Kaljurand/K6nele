package ee.ioc.phon.android.speak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import ee.ioc.phon.android.speak.model.ComboDao
import ee.ioc.phon.android.speak.model.ComboListDao
import ee.ioc.phon.android.speak.model.ServiceDao
import ee.ioc.phon.android.speak.model.ComboRepository
import ee.ioc.phon.android.speak.ui.AppNavHost

// TODO: Replace with your actual Room database class
import androidx.room.Room
import ee.ioc.phon.android.speak.model.ComboDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Room database (replace ComboDatabase::class.java with your actual database class)
        val db = Room.databaseBuilder(
            applicationContext,
            ComboDatabase::class.java,
            "combo-db"
        ).build()
        val repository = ComboRepository(
            db.serviceDao(),
            db.comboDao(),
            db.comboListDao()
        )
        setContent {
            MaterialTheme {
                AppNavHost(repository = repository)
            }
        }
    }
}
