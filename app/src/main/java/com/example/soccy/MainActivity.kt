package com.example.soccy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.soccy.ui.MainNavigation
import com.example.soccy.ui.theme.SoccyTheme
import com.google.firebase.FirebaseApp


class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            SoccyTheme {
                MainNavigation()
            }
        }
    }

}
