//package com.example.myapplication
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.AlertDialog
//import android.bluetooth.BluetoothAdapter
//import android.bluetooth.BluetoothDevice
//import android.bluetooth.BluetoothManager
//import android.bluetooth.BluetoothSocket
//import android.content.Context
//import android.content.pm.PackageManager
//import android.os.Build
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.speech.tts.TextToSpeech
//import android.widget.Button
//import android.widget.ImageButton
//import android.widget.TextView
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import java.io.InputStream
//import java.util.*
//
//class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
//
//    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
//
//    private var btSocket: BluetoothSocket? = null
//    private var readThread: Thread? = null
//    private lateinit var textViewData: TextView
//    private lateinit var buttonConnect: Button
//    private lateinit var buttonStart: ImageButton
//    private var tts: TextToSpeech? = null
//
//    private val bluetoothAdapter: BluetoothAdapter? by lazy {
//        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        bluetoothManager.adapter
//    }
//
//    private val requestPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { permissions ->
//        if (permissions.values.all { it }) {
//            showDeviceSelector()
//        } else {
//            textViewData.text = "Bluetooth permissions denied."
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        textViewData = findViewById(R.id.textViewData)
//        buttonConnect = findViewById(R.id.buttonConnect)
//        buttonStart = findViewById(R.id.start)
//
//        tts = TextToSpeech(this, this)
//
//        buttonConnect.setOnClickListener {
//            checkPermissionsAndSelectDevice()
//        }
//
//        buttonStart.setOnClickListener {
//            val text = textViewData.text.toString()
//            if (text.isNotEmpty() && text != "Waiting for data...") {
//                speakText(text)
//            }
//        }
//    }
//
//    private fun checkPermissionsAndSelectDevice() {
//        if (bluetoothAdapter == null) {
//            textViewData.text = "Bluetooth not supported on this device."
//            return
//        }
//
//        val permissionsNeeded = mutableListOf<String>()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
//            }
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN)
//            }
//        } else {
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
//            }
//        }
//
//        if (permissionsNeeded.isNotEmpty()) {
//            requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
//        } else {
//            showDeviceSelector()
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun showDeviceSelector() {
//        val pairedDevices = bluetoothAdapter?.bondedDevices
//        if (pairedDevices.isNullOrEmpty()) {
//            textViewData.text = "No paired devices found. Please pair your device in Bluetooth Settings first."
//            return
//        }
//
//        val deviceList = pairedDevices.toList()
//        val deviceNames = deviceList.map { it.name ?: it.address }.toTypedArray()
//
//        AlertDialog.Builder(this)
//            .setTitle("Select Bluetooth Device")
//            .setItems(deviceNames) { _, which ->
//                connectToDevice(deviceList[which])
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun connectToDevice(device: BluetoothDevice) {
//        textViewData.text = "Connecting to ${device.name ?: device.address}..."
//
//        Thread {
//            try {
//                btSocket = device.createRfcommSocketToServiceRecord(sppUuid)
//                bluetoothAdapter?.cancelDiscovery()
//                btSocket?.connect()
//
//                // Optional: Send a start command to the device
//                try {
//                    btSocket?.outputStream?.write("start\n".toByteArray())
//                } catch (e: Exception) {
//                    // Ignore write errors
//                }
//
//                runOnUiThread {
//                    textViewData.text = "Connected to ${device.name ?: device.address}\nWaiting for data..."
//                }
//
//                btSocket?.inputStream?.let { listenForData(it) }
//
//            } catch (e: Exception) {
//                runOnUiThread {
//                    textViewData.text = "Connection failed: ${e.message}"
//                }
//            }
//        }.start()
//    }
//
//    private fun listenForData(inputStream: InputStream) {
//        readThread = Thread {
//            val buffer = ByteArray(1024)
//            while (!Thread.currentThread().isInterrupted) {
//                try {
//                    val bytes = inputStream.read(buffer)
//                    if (bytes > 0) {
//                        val received = String(buffer, 0, bytes).trim()
//                        if (received.isNotEmpty()) {
//                            Handler(Looper.getMainLooper()).post {
//                                textViewData.append("$received\n")
//                            }
//                        }
//                    }
//                } catch (e: Exception) {
//                    if (!Thread.currentThread().isInterrupted) {
//                        Handler(Looper.getMainLooper()).post {
//                            textViewData.append("Disconnected: ${e.message}\n")
//                        }
//                    }
//                    break
//                }
//            }
//        }
//        readThread?.start()
//    }
//
//    override fun onInit(status: Int) {
//        if (status == TextToSpeech.SUCCESS) {
//            tts?.language = Locale.US
//        }
//    }
//
//    private fun speakText(text: String) {
//        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        readThread?.interrupt()
//        try {
//            btSocket?.close()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        tts?.stop()
//        tts?.shutdown()
//    }
//}

package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.github.anastr.speedviewlib.SpeedView
import java.io.InputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private val sppUuid: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var btSocket: BluetoothSocket? = null
    private var readThread: Thread? = null

    private lateinit var buttonConnect: Button

    private lateinit var gaugeVoltage: SpeedView
    private lateinit var gaugeCurrent: SpeedView
    private lateinit var gaugePower: SpeedView
    private lateinit var tvTemp: SpeedView
    private lateinit var tvHumidity: SpeedView

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.values.all { it }) {
                showDeviceSelector()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonConnect = findViewById(R.id.buttonConnect)

        gaugeVoltage = findViewById(R.id.gaugeVoltage)
        gaugeCurrent = findViewById(R.id.gaugeCurrent)
        gaugePower = findViewById(R.id.gaugePower)
        tvTemp = findViewById(R.id.tvTemp)
        tvHumidity = findViewById(R.id.tvHumidity)

        buttonConnect.setOnClickListener {
            checkPermissionsAndSelectDevice()
        }
    }

    private fun checkPermissionsAndSelectDevice() {
        val permissionsNeeded = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
        } else {
            showDeviceSelector()
        }
    }

    @SuppressLint("MissingPermission")
    private fun showDeviceSelector() {
        val devices = bluetoothAdapter?.bondedDevices?.toList() ?: return

        val names = devices.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Device")
            .setItems(names) { _, which ->
                connectToDevice(devices[which])
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        Thread {
            try {
                btSocket =
                    device.createRfcommSocketToServiceRecord(sppUuid)
                bluetoothAdapter?.cancelDiscovery()
                btSocket?.connect()

                btSocket?.inputStream?.let {
                    listenForData(it)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun listenForData(inputStream: InputStream) {
        readThread = Thread {
            val buffer = ByteArray(1024)

            while (true) {
                try {
                    val bytes = inputStream.read(buffer)

                    if (bytes > 0) {
                        val data =
                            String(buffer, 0, bytes).trim()

                        Handler(Looper.getMainLooper()).post {
                            updateUI(data)
                        }
                    }

                } catch (e: Exception) {
                    break
                }
            }
        }
        readThread?.start()
    }

    // 🔥 IMPORTANT: THIS UPDATES THE METERS
    private fun updateUI(data: String) {
        try {
            val parts = data.split(" ")

            val voltage = parts[1].toFloat()
            val current = parts[4].toFloat()
            val power = parts[7].toFloat()
            val temp = parts[10].toFloat()
            val humidity = parts[13].toFloat()

            gaugeVoltage.speedTo(voltage)
            gaugeCurrent.speedTo(current)
            gaugePower.speedTo(power)
            tvTemp.speedTo(temp)
            tvHumidity.speedTo(humidity)

//            gaugeVoltage.speedTo(voltage)

//            tvTemp.text = "Temp: $temp °C"
//            tvHumidity.text = "Humidity: $humidity %"

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        readThread?.interrupt()
        btSocket?.close()
    }
}
