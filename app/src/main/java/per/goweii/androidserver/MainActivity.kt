package per.goweii.androidserver

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import per.goweii.androidserver.http.SimpleHttpService
import per.goweii.androidserver.runtime.service.HttpService
import per.goweii.androidserver.ui.theme.AndroidServerTheme
import per.goweii.androidserver.ws.SimpleWsService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate -> $intent")

        setContent {
            AndroidServerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Greeting("Android Server")
                    }
                }
            }
        }

        HttpService.start<SimpleHttpService>(application, "SimpleHttpService")
        HttpService.start<SimpleWsService>(application, "SimpleWsService")
}

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        Log.d("MainActivity", "onNewIntent -> $intent")
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidServerTheme {
        Greeting("AndroidServer")
    }
}