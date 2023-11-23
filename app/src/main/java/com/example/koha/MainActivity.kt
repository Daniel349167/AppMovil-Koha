package com.example.koha

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.koha.ui.theme.KohaTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject
import java.io.IOException
import android.media.MediaPlayer
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat

data class LoanRequest(
    val request_id: String,
    val title: String,
    val barcode: String,
    val itemcallnumber: String,
    val author: String,
    val firstname: String,
    val surname: String,
    val borrowername: String,
    val timestamp: String,
    val branchname: String,
    val device_id: String,
    val tipo: String,
    val tiempo: String,
    val location: String
)

class MainActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = true // Establece íconos de la barra de estado claros u oscuros

        window.statusBarColor = Color(0xFF64B5F6).toArgb() // Establece el color de la barra de estado
        val loanRequests = mutableStateOf(mutableListOf<LoanRequest>())

        // Actualizar la lista al inicio y en intervalos
        lifecycleScope.launch {
            while (true) {
                updateLoanRequests(loanRequests, lifecycleScope, this@MainActivity)
                delay(6000)
            }
        }

        setContent {
            KohaTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("Solicitudes de Préstamo", color = Color.White, fontWeight = FontWeight.Bold)
                            },
                            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFF64B5F6))
                        )
                    }
                ) {
                    Column(modifier = Modifier.padding(top = 56.dp)) {
                        Spacer(modifier = Modifier.height(45.dp))

                        // Contenido principal
                        ItemList(requests = loanRequests.value, updateList = { updateLoanRequests(loanRequests, lifecycleScope, this@MainActivity) })
                    }
                }
            }
        }

    }



}


@Composable
fun ItemList(requests: MutableList<LoanRequest>, updateList: () -> Unit) {  // Añadir parámetro updateList

    LazyColumn {
        items(requests) { request ->
            LoanRequestItem(request = request, loanRequests = requests, updateList = updateList)  // Pasar updateList
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun LoanRequestItem(request: LoanRequest, loanRequests: MutableList<LoanRequest>, updateList: () -> Unit) { // Añadir un callback para actualizar la lista
    val offsetX = remember { mutableStateOf(0f) }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            title = { Text("Eliminar Solicitud") },
            text = { Text("Está seguro de eliminar?") },
            onDismissRequest = {
                showDialog = false
            },
            confirmButton = {
                TextButton(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        val success = deleteRequest(request.request_id)
                        withContext(Dispatchers.Main) {
                            if (success) {
                                updateList() // Llama al callback para actualizar la lista
                                offsetX.value = 0f
                                showDialog = false
                            } else {
                                // Mostrar algún mensaje de error o hacer algo si la eliminación falla
                            }
                        }
                    }
                }) {
                    Text("Si")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(8.dp)
            .graphicsLayer(
                translationX = offsetX.value
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    if (abs(dragAmount.x) > abs(dragAmount.y)) {  // Detectar movimientos horizontales
                        change.consume()
                        offsetX.value += dragAmount.x
                    }
                }
            },
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Image(
                painter = painterResource(id = R.drawable.imagen_lista),
                contentDescription = null,
                modifier = Modifier
                    .size(95.dp)
                    .align(Alignment.CenterVertically)
                    .offset(y = (-5).dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Código: ${request.barcode}",
                    style = TextStyle(fontWeight = FontWeight.Bold, color = Color.Red)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Título: ${request.title}")
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Autor: ${request.author}")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tipo: ${request.tipo}",
                    color = Color(0xFF0000FF)
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${request.firstname} ${request.surname}",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Código para calcular días y horas
                val horas = extractHours(request.tiempo)
                val days = horas / 24
                val remainingHours = horas % 24

                val tiempoTexto = if (days > 0) {
                    "$days día${if (days > 1) "s" else ""} y $remainingHours hora${if (remainingHours > 1) "s" else ""}"
                } else {
                    "$horas hora${if (horas > 1) "s" else ""}"
                }

                Text(
                    text = "Solicitado hace: $tiempoTexto",
                    color = Color(0xFF006400),
                    textAlign = TextAlign.End
                )
            }
        }
    }

    LaunchedEffect(offsetX.value) {
        if (abs(offsetX.value) > 200f) {
            showDialog = true  // Mostrar el cuadro de diálogo
            offsetX.value = 0f  // Reiniciar el desplazamiento
        }
    }
}
fun extractHours(timeString: String): Int {
    return timeString.split(" ")[0].toInt()
}

suspend fun fetchLoanRequests(): List<LoanRequest>? {
    val client = OkHttpClient()
    val request = Request.Builder()
        //.url("http://procesos.uni.edu.pe/request.php?action=getList")
        .url("http://172.16.28.51:802/request.php?action=getList")
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                println("Error HTTP: ${response.code}")
                return null
            }

            val jsonResult = response.body?.string()
            println("Resultado JSON: $jsonResult")

            val listType = object : TypeToken<ResponseWrapper>() {}.type
            val wrapper = Gson().fromJson<ResponseWrapper>(jsonResult, listType)
            wrapper?.requests
        }
    } catch (e: IOException) {
        println("Error al realizar la solicitud: ${e.message}")
        null
    } catch (e: JsonSyntaxException) {
        println("Error al deserializar el JSON: ${e.message}")
        null
    }
}



suspend fun deleteRequest(requestId: String): Boolean {
    val client = OkHttpClient()
    val request = Request.Builder()
        //.url("http://procesos.uni.edu.pe/request.php?action=delete&id=$requestId")
        .url("http://172.16.28.51:802/request.php?action=delete&id=$requestId")
        .build()

    try {
        return client.newCall(request).execute().use { response ->
            val jsonResult = response.body?.string()

            // Imprime la respuesta completa del servidor
            println("Respuesta del servidor: $jsonResult")

            if (!response.isSuccessful) {
                println("Error en la solicitud: ${response.code}")
                return false
            }

            val jsonObject = JSONObject(jsonResult)
            return jsonObject.getString("result") == "OK"
        }
    } catch (e: Exception) {
        // Imprime cualquier excepción que pueda ocurrir
        println("Excepción capturada: ${e.message}")
        return false
    }
}



data class ResponseWrapper(
    val requests: List<LoanRequest>
)


fun updateLoanRequests(loanRequests: MutableState<MutableList<LoanRequest>>, scope: CoroutineScope, context: Context) {
    scope.launch {
        val previousSize = loanRequests.value.size
        val newLoanRequests = withContext(Dispatchers.IO) { fetchLoanRequests() }
        loanRequests.value = newLoanRequests?.toMutableList() ?: mutableListOf()

        // Reproducir sonido si hay nuevos pedidos
        if (loanRequests.value.size > previousSize) {
            playNotificationSound(context)
        }
    }
}

private fun playNotificationSound(context: Context) {
    val mediaPlayer: MediaPlayer = MediaPlayer.create(context, R.raw.sound2)
    mediaPlayer.start()
    println("suena")
    mediaPlayer.setOnCompletionListener {
        it.release()
    }
}
