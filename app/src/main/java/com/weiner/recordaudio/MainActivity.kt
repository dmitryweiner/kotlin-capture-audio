package com.weiner.recordaudio

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    var isRunning = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        val buttonStart = findViewById<Button>(R.id.buttonStart)
        buttonStart.setOnClickListener {
            isRunning = true
            recordAudioWithPermissions()
        }

        val buttonStop = findViewById<Button>(R.id.buttonStop)
        buttonStop.setOnClickListener {
            isRunning = false
        }

    }

    @SuppressLint("MissingPermission")
    fun recordAudio() {
        // check permissions first
        val RECORDER_SAMPLERATE = 8000
        val RECORDER_CHANNELS: Int = AudioFormat.CHANNEL_IN_MONO
        val RECORDER_AUDIO_ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLERATE,
            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING
        )
        val BufferElements2Rec = 1024 // want to play 2048 (2K) since 2 bytes we use only 1024

        val BytesPerElement = 2 // 2 bytes in 16bit format

        val sData = ShortArray(BufferElements2Rec)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            RECORDER_SAMPLERATE, RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement
        )

        val textView = findViewById<TextView>(R.id.textView)
        thread {
            recorder.startRecording()
            while (isRunning) {
                // gets the voice output from microphone to byte format
                recorder.read(sData, 0, BufferElements2Rec)
                val max = Math.round(1.0 * sData.max() / Short.MAX_VALUE * 100).toInt()
                runOnUiThread {
                    println(max)
                    textView.text = "#".repeat(max)
                }
            }
            recorder.stop()
        }
    }

    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Разрешение дали 😊
                // можно делать что собирались
                recordAudio()
            } else {
                // Разрешение не дали 😭
                // Покажем тост с объяснениями, зачем разрешение
                Toast.makeText(
                    applicationContext,
                    "Приложению нужно разрешение для записи звука",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    @RequiresApi(Build.VERSION_CODES.M)
    fun recordAudioWithPermissions() {

        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                recordAudio()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.RECORD_AUDIO) -> {
                Toast.makeText(
                    applicationContext,
                    "Приложению нужно разрешение для записи звука",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }


}