package com.example.koha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class LoanRequestViewModel : ViewModel() {
    val loanRequests = MutableLiveData<MutableList<LoanRequest>>()

    init {
        loanRequests.value = mutableListOf()
    }

    fun updateLoanRequests(context: Context) {
        viewModelScope.launch {
            Log.d("LoanRequestViewModel", "Iniciando actualización de solicitudes de préstamo")

            val previousSize = loanRequests.value?.size ?: 0
            val newLoanRequests = withContext(Dispatchers.IO) { fetchLoanRequests() }

            Log.d("LoanRequestViewModel", "Solicitudes obtenidas: ${newLoanRequests?.size ?: 0}")

            loanRequests.postValue(newLoanRequests?.toMutableList() ?: mutableListOf())

            // Agrega un pequeño retraso para asegurar la actualización de postValue
            delay(100)

            val newSize = loanRequests.value?.size ?: 0
            Log.d("LoanRequestViewModel", "Tamaño actualizado: $newSize")

            if (newSize > previousSize) {
                Log.d("LoanRequestViewModel", "Hay nuevas solicitudes, intentando reproducir sonido")
                playNotificationSound(context)
            } else {
                Log.d("LoanRequestViewModel", "No hay nuevas solicitudes")
            }
        }
    }



    private fun playNotificationSound(context: Context) {
        Log.d("LoanRequestViewModel", "Intentando reproducir el sonido de notificación")

        try {
            val mediaPlayer: MediaPlayer = MediaPlayer.create(context, R.raw.sound2)
            if (mediaPlayer == null) {
                Log.e("LoanRequestViewModel", "Error al crear MediaPlayer, mediaPlayer es null")
                return
            }

            mediaPlayer.start()
            Log.d("LoanRequestViewModel", "Reproducción de sonido iniciada")

            mediaPlayer.setOnCompletionListener {
                it.release()
                Log.d("LoanRequestViewModel", "Sonido de notificación completado y recurso liberado")
            }
        } catch (e: Exception) {
            Log.e("LoanRequestViewModel", "Excepción al reproducir el sonido: ${e.message}")
        }
    }


    // Aquí incluye cualquier otra función necesaria, como fetchLoanRequests
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



}
