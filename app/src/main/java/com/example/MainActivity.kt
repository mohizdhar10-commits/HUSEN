package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.i18n.stringsFor
import com.example.model.MainTab
import com.example.model.UiState
import com.example.ui.components.StudioBottomBar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.VidGenAITheme
import com.example.ui.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        VidGenAITheme(darkTheme = true) {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            MainStudioApp(currentUserId = user.uid)
        }
    }
}

@Composable
fun MainStudioApp(currentUserId: String) {
    val context = LocalContext.current
    val viewModel: StudioViewModel = viewModel(
        key = currentUserId,
        factory = StudioViewModel.provideFactory(context, currentUserId)
    )

    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val lang = profile?.preferredLanguage ?: "id"
    val darkTheme = profile?.darkTheme ?: true
    val strings = stringsFor(lang)

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(ui.snackbarMessage) {
        ui.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    if (ui.activeTab != MainTab.HOME) {
        BackHandler {
            viewModel.selectTab(MainTab.HOME)
        }
    }

    VidGenAITheme(darkTheme = darkTheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                StudioBottomBar(
                    activeTab = ui.activeTab,
                    strings = strings,
                    onSelectTab = { viewModel.selectTab(it) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (ui.activeTab) {
                    MainTab.HOME -> HomeScreen(
                        profile = profile,
                        lang = lang,
                        darkTheme = darkTheme,
                        strings = strings,
                        templates = viewModel.templates,
                        onToggleTheme = { viewModel.toggleTheme(it) },
                        onToggleLanguage = { viewModel.toggleLanguage(it) },
                        onNavigateTab = { viewModel.selectTab(it) },
                        onOpenQuickTool = { mode, toolTab -> viewModel.openQuickTool(mode, toolTab) },
                        onSelectTemplate = { viewModel.applyTemplate(it) }
                    )
                    MainTab.CREATE -> CreateScreen(
                        ui = ui,
                        lang = lang,
                        strings = strings,
                        viewModel = viewModel
                    )
                    MainTab.TOOLS -> ToolsScreen(
                        ui = ui,
                        lang = lang,
                        strings = strings,
                        viewModel = viewModel
                    )
                    MainTab.LIBRARY -> LibraryScreen(
                        ui = ui,
                        projectsState = projectsState,
                        lang = lang,
                        strings = strings,
                        viewModel = viewModel
                    )
                    MainTab.PROFILE -> ProfileScreen(
                        profile = profile,
                        projectsState = projectsState,
                        lang = lang,
                        darkTheme = darkTheme,
                        strings = strings,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
