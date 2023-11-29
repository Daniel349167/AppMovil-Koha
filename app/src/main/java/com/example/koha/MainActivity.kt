package com.example.koha

import android.annotation.SuppressLint
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.lifecycleScope
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
import androidx.compose.foundation.layout.Box
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle




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

    private lateinit var viewModel: LoanRequestViewModel

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(LoanRequestViewModel::class.java)

        // Actualizar la lista al inicio y en intervalos
        lifecycleScope.launch {
            while (true) {
                viewModel.updateLoanRequests(this@MainActivity)
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
                    Box(modifier = Modifier.padding(bottom = 1.dp)) {
                        LoanRequestListScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun LoanRequestListScreen(viewModel: LoanRequestViewModel) {
    val loanRequests = viewModel.loanRequests.observeAsState(listOf())
    val context = LocalContext.current

    Column(modifier = Modifier
        .padding(top = 14.dp)
        .padding(bottom = 5.dp)) {
        Spacer(modifier = Modifier.height(50.dp))

        // Contenido principal
        ItemList(requests = loanRequests.value) { viewModel.updateLoanRequests(context) }
    }
}


@Composable
fun ItemList(requests: List<LoanRequest>, updateList: () -> Unit) {  // Añadir parámetro updateList

    LazyColumn {
        items(requests) { request ->
            LoanRequestItem(request = request, loanRequests = requests, updateList = updateList)  // Pasar updateList
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}


@Composable
fun LoanRequestItem(request: LoanRequest, loanRequests: List<LoanRequest>, updateList: () -> Unit) {
    var offsetX = remember { mutableStateOf(0f) }
    var showDialog by remember { mutableStateOf(false) }
    var dragStarted = remember { mutableStateOf(false) }
    var verticalDragInProgress = remember { mutableStateOf(false) }




    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            title = {
                Column {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Alert Icon",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Eliminar Solicitud",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            text = {
                Text(
                    "¿Está seguro de eliminar esta solicitud?",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
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
                    }
                ) {
                    Text("Eliminar", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }



    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(8.dp)
            .graphicsLayer(translationX = offsetX.value)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragStarted.value = true
                        verticalDragInProgress.value = false
                    },
                    onDragEnd = {
                        dragStarted.value = false
                        // Mueve la lógica de verificación aquí
                        if (abs(offsetX.value) > 50f) {
                            showDialog = true
                            offsetX.value = 0f
                        } else {
                            offsetX.value = 0f
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (dragStarted.value && !verticalDragInProgress.value) {
                            if (abs(dragAmount.y) > abs(dragAmount.x)) {
                                verticalDragInProgress.value = true
                            } else {
                                val newOffset = offsetX.value + dragAmount.x
                                if (offsetX.value >= 0f) {
                                        offsetX.value = newOffset
                                }
                                change.consume()
                            }
                        }
                    }
                )
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
                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Título: ")
                        }
                        append("${request.title}")
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Autor: ")
                        }
                        append("${request.author}")
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.width(25.dp))

            Column(
                horizontalAlignment = Alignment.End
            ) {0
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val timestamp = dateFormat.parse(request.timestamp)

                val now = Calendar.getInstance().time
                val duration = now.time - timestamp.time // tiempo en milisegundos

                val totalMinutes = duration / (1000 * 60)
                val days = totalMinutes / (24 * 60)
                val remainingHours = (totalMinutes % (24 * 60)) / 60
                val remainingMinutes = totalMinutes % 60

                val tiempoTexto = when {
                    days > 0 -> "$days día${if (days != 1.toLong()) "s" else ""} y $remainingHours hora${if (remainingHours != 1.toLong()) "s" else ""}"
                    remainingHours > 0 -> "$remainingHours hora${if (remainingHours != 1.toLong()) "s" else ""} y $remainingMinutes minuto${if (remainingMinutes != 1.toLong()) "s" else ""}"
                    remainingMinutes > 0 -> "$remainingMinutes minuto${if (remainingMinutes != 1.toLong()) "s" else ""}"
                    else -> "menos de un minuto"
                }



                Text(
                    text = "${request.firstname} ${request.surname}",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column {

                Text(
                    text = "Solicitado hace:",
                    color = Color(0xFF006400),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = tiempoTexto,
                    color = Color(0xFF006400),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "Tipo: ${request.tipo}",
                    color = Color(0xFF0000FF)
                )
                }
            }

        }
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
fun extractHours(timeString: String): Int {
    return timeString.split(" ")[0].toInt()
}







data class ResponseWrapper(
    val requests: List<LoanRequest>
)

