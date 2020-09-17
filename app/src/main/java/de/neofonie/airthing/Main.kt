package de.neofonie.airthing

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.ekn.gruzer.gaugelibrary.ArcGauge
import com.ekn.gruzer.gaugelibrary.HalfGauge
import com.ekn.gruzer.gaugelibrary.Range
import com.google.android.things.pio.PeripheralManager
import com.google.android.things.pio.UartDevice
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread


class Main : Activity() {

    private lateinit var gaugePm10: ArcGauge
    private lateinit var gaugePm2_5: ArcGauge
    private lateinit var gaugePm1_0: ArcGauge
    private lateinit var uartDevice: UartDevice

    private val handler = Handler()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pm = PeripheralManager.getInstance()
        val uart = pm.uartDeviceList.first()
        uartDevice = pm.openUartDevice(uart)
        uartDevice.apply {
            setBaudrate(9600)
            setParity(UartDevice.PARITY_NONE)
            setStopBits(1)
            setHardwareFlowControl(UartDevice.HW_FLOW_CONTROL_NONE)
        }

        thread(isDaemon = true, start = true) {
            while (true) {
                read()
            }
        }
        setContentView(R.layout.main)
        gaugePm1_0 = setChart(R.id.pm1_0, "PM 1.0")
        gaugePm2_5 = setChart(R.id.pm2_5, "PM 2.5")
        gaugePm10 = setChart(R.id.pm10, "PM 10")
    }


    @ExperimentalUnsignedTypes
    private fun updateData(byteArray: ByteArray) {
        val buffer = ByteBuffer.wrap(byteArray).order(ByteOrder.BIG_ENDIAN).asShortBuffer()

        val controlSum = buffer[0].toUShort().toInt()
        val frameLength = buffer[1].toUShort().toInt()
        val pm1_0 = buffer[2].toUShort().toInt()
        val pm2_5 = buffer[3].toUShort().toInt()
        val pm10 = buffer[4].toUShort().toInt()
        gaugePm2_5.value = pm2_5.toDouble()
        gaugePm1_0.value = pm1_0.toDouble()
        gaugePm10.value = pm10.toDouble()

    }


    private fun read() {
        val buffer = ByteArray(32)
        val read = uartDevice.read(buffer, buffer.size)
        if (read == 32) {
            Log.d("UART", "UART$read =  ${buffer.toList()}")
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

}