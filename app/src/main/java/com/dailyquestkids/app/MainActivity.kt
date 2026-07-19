package com.dailyquestkids.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = DailyQuestContainer(applicationContext)
        setContent {
            DailyQuestApp(container = container)
        }
    }
}
