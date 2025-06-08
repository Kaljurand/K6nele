package ee.ioc.phon.android.speak.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ee.ioc.phon.android.speak.model.ComboRepository
import ee.ioc.phon.android.speak.viewmodel.ComboDetailsViewModel
import ee.ioc.phon.android.speak.viewmodel.ComboListViewModel
import ee.ioc.phon.android.speak.viewmodel.ServicePickerViewModel

sealed class Screen(val route: String) {
    object ComboList : Screen("comboList")
    object ServicePicker : Screen("servicePicker")
    data class ComboDetails(val comboId: Long) : Screen("comboDetails/{comboId}") {
        companion object {
            fun createRoute(comboId: Long) = "comboDetails/$comboId"
        }
    }
}

@Composable
fun AppNavHost(
    repository: ComboRepository,
    startDestination: String = Screen.ComboList.route
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.ComboList.route) {
            val viewModel = remember { ComboListViewModel(repository, "IME") }
            ComboListScreen(
                viewModel = viewModel,
                onComboClick = { combo -> navController.navigate(Screen.ComboDetails.createRoute(combo.comboId)) },
                onCloneCombo = { /* TODO: implement clone */ },
                onRemoveCombo = { combo -> viewModel.removeCombo(combo) },
                onAddCombo = { navController.navigate(Screen.ServicePicker.route) },
                onLoadCombos = { /* TODO: implement load */ },
                onSaveCombos = { /* TODO: implement save */ }
            )
        }
        composable(Screen.ServicePicker.route) {
            val viewModel = remember { ServicePickerViewModel(repository) }
            ServicePickerScreen(
                viewModel = viewModel,
                onServiceSelected = { /* TODO: handle service selection */ },
                onConfigureService = { /* TODO: handle service config */ }
            )
        }
        composable("comboDetails/{comboId}") { backStackEntry ->
            val comboId = backStackEntry.arguments?.getString("comboId")?.toLongOrNull() ?: return@composable
            val viewModel = remember { ComboDetailsViewModel(repository, comboId) }
            ComboDetailsScreen(
                viewModel = viewModel,
                onSave = { /* TODO: handle save */ },
                onConfigureService = { /* TODO: handle config */ }
            )
        }
    }
}
