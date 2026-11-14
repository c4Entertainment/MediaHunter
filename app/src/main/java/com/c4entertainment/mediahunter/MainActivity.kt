package com.c4entertainment.mediahunter

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.c4entertainment.mediahunter.navigation.Screen
import com.c4entertainment.mediahunter.presentation.movie_info.MovieDetailsScreen
import com.c4entertainment.mediahunter.presentation.movie_info.MovieDetailsViewModel
import com.c4entertainment.mediahunter.presentation.movies.MovieScreen
import com.c4entertainment.mediahunter.presentation.movies.MovieViewModel
import com.c4entertainment.mediahunter.presentation.profile.SettingsScreen
import com.c4entertainment.mediahunter.presentation.profile.SettingsViewModel
import com.c4entertainment.mediahunter.presentation.search.SearchScreen
import com.c4entertainment.mediahunter.presentation.search.SearchViewModel
import com.c4entertainment.mediahunter.ui.theme.MediaHunterTheme
import com.c4entertainment.mediahunter.util.DarkThemeConfig
import com.c4entertainment.mediahunter.util.ConnectivityObserver
import com.c4entertainment.mediahunter.util.MovieTVFilterConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel by viewModels<MainActivityViewModel>()

    private val TAG = "MainActivity"

    @Inject
    lateinit var connectivityObserver: ConnectivityObserver


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()

        var uiState: MainActivityUiState by mutableStateOf(MainActivityUiState.Loading)

        // Update the uiState
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .onEach {
                        uiState = it
                    }
                    .collect()
            }
        }

        // Keep the splash screen on-screen until the UI state is loaded. This condition is
        // evaluated each time the app needs to be redrawn so it should be fast to avoid blocking
        // the UI.
        splashScreen.setKeepOnScreenCondition {
            when (uiState) {
                MainActivityUiState.Loading -> true
                is MainActivityUiState.Success -> false
            }
        }

        setContent {
            val darkTheme = shouldUseDarkTheme(uiState)
            val useDynamicColor = useDynamicColor(uiState = uiState)
            val snackbarHostState = remember { SnackbarHostState() }


            val status by connectivityObserver.observe().collectAsState(
                initial = ConnectivityObserver.Status.Unavailable
            )
            val isOffline = status != ConnectivityObserver.Status.Available


            // Update the dark content of the system bars to match the theme
            DisposableEffect(darkTheme) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
                onDispose {}
            }

            MediaHunterTheme(
                darkTheme = darkTheme,
                dynamicColor = useDynamicColor,
                pureBlack = (uiState as? MainActivityUiState.Success)?.userSettings?.pureBlack == true,
            ) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination?.route

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                    ) {

                        NavSetUp(
                            navController = navController,
                            paddingValues = it)

                    }

                    // If user is not connected to the internet show a snack bar to inform them.
                    val notConnectedMessage = stringResource(R.string.not_connected)
                    Log.d(TAG, "onCreate isOffline: $isOffline")
                    LaunchedEffect(isOffline) {
                        if (isOffline) {
                            delay(1000)
                            snackbarHostState.showSnackbar(
                                message = notConnectedMessage,
                                duration = SnackbarDuration.Indefinite,
                            )
                        }
                    }

                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun NavSetUp(
        navController: NavHostController,
        paddingValues: PaddingValues,
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.MovieScreen.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = paddingValues),
        ) {
            composable(Screen.MovieScreen.route) {
                val viewModel = hiltViewModel<MovieViewModel>()
                MovieScreen(
                    navController = navController,
                    viewModel = viewModel,
                )
            }
            composable("movie/{type}/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                val type = backStackEntry.arguments?.getString("type")
                Log.d(TAG, "NavSetUp: Movie ID -  $id")
                val viewModel = hiltViewModel<MovieDetailsViewModel>()
                MovieDetailsScreen(
                    id = id ?: "",
                    type = type ?: "",
                    viewModel = viewModel,
                    navigateBack = { navController.popBackStack() },
                )
            }
            composable(Screen.SettingsScreen.route) {
                SettingsScreen(
                    viewModel = hiltViewModel<SettingsViewModel>(),
                    navigateBack = { navController.popBackStack() },
                )
            }
            composable(Screen.SearchScreen.route) {
                val viewModel = hiltViewModel<SearchViewModel>()
                SearchScreen(
                    viewModel = viewModel,
                    navController = navController,
                )
            }
        }
    }


    /**
     * Returns `true` if the dynamic color is disabled, as a function of the [uiState].
     */
    @Composable
    private fun useDynamicColor(
        uiState: MainActivityUiState,
    ): Boolean = when (uiState) {
        MainActivityUiState.Loading -> false
        is MainActivityUiState.Success -> uiState.userSettings.useDynamicColor
    }

    /**
     * Returns `true` if dark theme should be used, as a function of the [uiState] and the
     * current system context.
     */
    @Composable
    private fun shouldUseDarkTheme(
        uiState: MainActivityUiState,
    ): Boolean = when (uiState) {
        MainActivityUiState.Loading -> isSystemInDarkTheme()
        is MainActivityUiState.Success -> when (uiState.userSettings.darkThemeConfig) {
            DarkThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
            DarkThemeConfig.LIGHT -> false
            DarkThemeConfig.DARK -> true
            else -> {
                // Should never happen, but if it does, default to following the system
                // dark theme setting.
                isSystemInDarkTheme()
            }
        }
    }
}
