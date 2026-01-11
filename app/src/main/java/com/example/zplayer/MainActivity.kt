package com.example.zplayer

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.zplayer.ui.screens.LibraryScreen
import com.example.zplayer.ui.screens.MainPlayerScreen
import com.example.zplayer.ui.theme.ZPlayerTheme
import com.example.zplayer.ui.viewmodel.PlayerViewModel
import com.example.zplayer.ui.viewmodel.PlayerViewModelFactory
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels {
        PlayerViewModelFactory(applicationContext)
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        android.Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    
                    val permissionState = rememberPermissionState(permission)

                    if (permissionState.status.isGranted) {
                        LaunchedEffect(Unit) {
                            viewModel.loadAudioFiles()
                        }
                        
                        val navController = rememberNavController()
                        
                        NavHost(navController = navController, startDestination = "library") {
                            composable("library") {
                                LibraryScreen(
                                    viewModel = viewModel,
                                    onSongSelected = {
                                        navController.navigate("player")
                                    },
                                    onMiniPlayerClick = {
                                        navController.navigate("player")
                                    }
                                )
                            }
                            composable("player") {
                                MainPlayerScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                        
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Button(onClick = { permissionState.launchPermissionRequest() }) {
                                Text("Grant Music Permission")
                            }
                        }
                    }
                }
            }
        }
    }
}
