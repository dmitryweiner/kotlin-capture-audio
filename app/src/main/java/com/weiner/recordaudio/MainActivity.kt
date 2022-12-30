package com.weiner.recordaudio

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

    companion object {
        const val MAX_ARRAY_SIZE = 200
    }
    private var isRunning = false
    private lateinit var data: Array<DoubleArray>
    private var currentIndex = 0
    private var frequenciesCount by Delegates.notNull<Int>()

    private lateinit var imageView: ImageView
    private lateinit var bmp: Bitmap

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val buttonStart = findViewById<Button>(R.id.buttonStart)
        val buttonStop = findViewById<Button>(R.id.buttonStop)

        buttonStart.setOnClickListener {
            isRunning = true
            buttonStart.isEnabled = false
            buttonStop.isEnabled = true
            recordAudioWithPermissions()
        }

        buttonStop.setOnClickListener {
            isRunning = false
            buttonStart.isEnabled = true
            buttonStop.isEnabled = false
        }

        imageView = findViewById(R.id.imageView)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        bmp = Bitmap.createBitmap(imageView.width, imageView.height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.BLACK)
    }

    @SuppressLint("MissingPermission")
    fun recordAudio() {
        // check permissions first
        val RECORDER_SAMPLERATE = 8000
        val RECORDER_CHANNELS: Int = AudioFormat.CHANNEL_IN_MONO
        val RECORDER_AUDIO_ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT
        val butterSize = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLERATE,
            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING
        )
        val buffer = ShortArray(butterSize)
        frequenciesCount = getMinimalPowerOf2(butterSize)
        data = Array(MAX_ARRAY_SIZE) {
            DoubleArray(frequenciesCount) { 0.0 }
        }
        val x = DoubleArray(frequenciesCount)
        val y = DoubleArray(frequenciesCount)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            RECORDER_SAMPLERATE, RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING, frequenciesCount
        )

        val fft = FFT(frequenciesCount)

        thread {
            recorder.startRecording()
            while (isRunning) {
                // gets the voice output from microphone to byte format
                recorder.read(buffer, 0, frequenciesCount)

                for (fr in 0 until frequenciesCount) {
                    x[fr] = buffer[fr] * 1.0
                    y[fr] = 0.0
                }
                fft.process(x, y)
                for (fr in 0 until frequenciesCount) {
                    data[currentIndex][fr] = Math.abs(y[fr])
                }

                render()
                currentIndex = if (currentIndex == (MAX_ARRAY_SIZE - 1)) 0 else currentIndex + 1
            }
            recorder.stop()
        }
    }

    private fun render() {
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectHeight = 1.0f * bmp.height / MAX_ARRAY_SIZE
        val rectWidth = 1.0f * bmp.width / (frequenciesCount / 2.0f)
        repeat(MAX_ARRAY_SIZE) { y ->
            val index = if (currentIndex + y + 1 < MAX_ARRAY_SIZE) currentIndex + y + 1 else currentIndex + y + 1 - MAX_ARRAY_SIZE
            val min = data[index].min()
            val max = data[index].max()
            for (x in 0 until frequenciesCount / 2) {
                paint.color = getColorByValue(data[index][x], min, max)
                c.drawRect(
                    x * rectWidth,
                    y * rectHeight,
                    (x + 1) * rectWidth,
                    (y + 1) * rectHeight,
                    paint
                )
            }
        }
        runOnUiThread {
            imageView.setImageBitmap(bmp)
            imageView.invalidate()
        }
    }

    private val requestPermissionLauncher =
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

    private fun getMinimalPowerOf2(n: Int): Int {
        return Math.pow(2.0, Math.floor(log2(n.toDouble()))).toInt()
    }
}

fun getColorByValue(value: Double, min: Double, max: Double): Int {
    if (min == 0.0 && max == 0.0) {
        return Color.rgb(0, 0, 0)
    }
    val normalizedValue = (value - min) / (max - min)
    val color = (normalizedValue * 255).roundToInt()
    return Color.rgb(color, color, color)
}