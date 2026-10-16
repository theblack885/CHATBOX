package com.example.insta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InstaTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { App() }
            }
        }
    }
}

@Composable
fun App() {
    val uid by Repo.authState().collectAsState(initial = Repo.uid)
    if (uid == null) AuthScreen() else MainNav()
}

@Composable
fun MainNav() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onComments = { nav.navigate("post/$it") },
                onUser = { nav.navigate("user/$it") }
            )
        }
        composable("post/{id}") { e ->
            CommentsScreen(
                postId = e.arguments!!.getString("id")!!,
                onBack = { nav.popBackStack() },
                onUser = { nav.navigate("user/$it") }
            )
        }
        composable("user/{uid}") { e ->
            ProfileScreen(
                uid = e.arguments!!.getString("uid")!!,
                onBack = { nav.popBackStack() },
                onOpenPost = { nav.navigate("post/$it") }
            )
        }
    }
}
