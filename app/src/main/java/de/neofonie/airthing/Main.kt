package de.neofonie.airthing

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Window
import com.ekn.gruzer.gaugelibrary.ArcGauge
import com.ekn.gruzer.gaugelibrary.HalfGauge
import com.ekn.gruzer.gaugelibrary.Range
import com.google.android.things.pio.PeripheralManager
import com.google.android.things.pio.UartDevice
import kotlinx.android.synthetic.main.main.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread


class Main : Activity() {

    private lateinit var gaugePm10: ArcGauge
    private lateinit var gaugePm2_5: ArcGauge
    private lateinit var gaugePm1_0: ArcGauge
    private lateinit var gaugePm10Atm: ArcGauge
    private lateinit var gaugePm2_5Atm: ArcGauge
    private lateinit var gaugePm1_0Atm: ArcGauge
    private lateinit var uartDevice: UartDevice

    private val handler = Handler()
    private val dimHandler = Handler()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.attributes.screenBrightness = 0.0f
        window.attributes = window.attributes
        val pm = PeripheralManager.getInstance()
        val uart = pm.uartDeviceList.first()
        uartDevice = pm.openUartDevice(uart)
        uartDevice.apply {
            setBaudrate(9600)
            setParity(UartDevice.PARITY_NONE)
            setStopBits(1)
            setHardwareFlowControl(UartDevice.HW_FLOW_CONTROL_NONE)
        }
        dimScreenAfterWhile()

        thread(isDaemon = true, start = true) {
            while (true) {
                read()
            }
        }
        setContentView(R.layout.main)
        root.setOnClickListener {
            window.attributes.screenBrightness = 1f
            window.attributes = window.attributes
            dimScreenAfterWhile()
        }
        gaugePm1_0 = setChart(R.id.pm1_0, "PM 1.0")
        gaugePm2_5 = setChart(R.id.pm2_5, "PM 2.5")
        gaugePm10 = setChart(R.id.pm10, "PM 10")
        gaugePm1_0Atm = setChart(R.id.pm1_0atm, "PM 1.0")
        gaugePm2_5Atm = setChart(R.id.pm2_5atm, "PM 2.5")
        gaugePm10Atm = setChart(R.id.pm10atm, "PM 10")

    }

    private fun dimScreenAfterWhile() {
        dimHandler.removeCallbacksAndMessages(null)
        dimHandler.postDelayed({
            window.attributes.screenBrightness = 0f
            window.attributes = window.attributes
        }, 10 * 1000)
    }


    @ExperimentalUnsignedTypes
    private fun updateData(byteArray: ByteArray) {
        val expectedFrameLength = 28
        val expectedControlSum = 0x424d
        val buffer = ByteBuffer.wrap(byteArray).order(ByteOrder.BIG_ENDIAN).asShortBuffer()

        val controlSum = buffer[0].toUShort().toInt()
        val frameLength = buffer[1].toUShort().toInt()
        val pm1_0 = buffer[2].toUShort().toInt()
        val pm2_5 = buffer[3].toUShort().toInt()
        val pm10 = buffer[4].toUShort().toInt()
        val pm1_0atm = buffer[5].toUShort().toInt()
        val pm2_5atm = buffer[6].toUShort().toInt()
        val pm10atm = buffer[7].toUShort().toInt()
        val checkSum = buffer[15].toUShort()

        val sublist = byteArray.map { it.toUByte() }.subList(0, 30)
        val sum = sublist.map { it.toUShort() }.reduce { acc, uShort -> (acc + uShort).toUShort() }
        Log.d("CHECKSUM", "$checkSum == $sum")
        if (controlSum == expectedControlSum && frameLength == expectedFrameLength && checkSum == sum) {
            gaugePm2_5.value = pm2_5.toDouble()
            gaugePm1_0.value = pm1_0.toDouble()
            gaugePm10.value = pm10.toDouble()

            gaugePm2_5Atm.value = pm2_5atm.toDouble()
            gaugePm1_0Atm.value = pm1_0atm.toDouble()
            gaugePm10Atm.value = pm10atm.toDouble()
        }
    }


    private fun read() {
        val buffer = ByteArray(32)
        val read = uartDevice.read(buffer, buffer.size)
        if (read == 32) {
            Log.d("UART", "UART$read =  ${buffer.toList().map { it.toUByte() }}")
            handler.post { updateData(buffer) }
        }
    }

    private fun setChart(id: Int, name: String): ArcGauge {
        val halfGauge: ArcGauge = findViewById(id)
        val range = Range()
        range.color = Color.parseColor("#00b20b")
        range.from = 0.0
        range.to = 50.0

        val range2 = Range()
        range2.color = Color.parseColor("#E3E500")
        range2.from = 50.0
        range2.to = 100.0

        val range3 = Range()
        range3.color = Color.parseColor("#ce0000")
        range3.from = 100.0
        range3.to = 150.0
        //add color ranges to gauge
        halfGauge.addRange(range)
        halfGauge.addRange(range2)
        halfGauge.addRange(range3)

        //set min max and current value
        halfGauge.minValue = 0.0
        halfGauge.maxValue = 150.0
        halfGauge.value = 0.0
        return halfGauge
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacksAndMessages(null)
        dimHandler.removeCallbacksAndMessages(null)
    }

}