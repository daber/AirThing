package de.neofonie.airthing.sensor

import android.util.Log
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import com.google.android.things.pio.UartDevice
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*


@ExperimentalUnsignedTypes
class PMSensor {
    data class Data(
        val pm1_0: Int,
        val pm2_5: Int,
        val pm10: Int,
        val pm1_0atm: Int,
        val pm2_5atm: Int,
        val pm10atm: Int
    )

    private lateinit var uartDevice: UartDevice
    private val buffer = ByteBuffer.allocate(32)
    private lateinit var gpio: Gpio
    var isEnabled: Boolean
        get() = gpio.value
        set(value) {
            gpio.value = value
        }

    fun setup() {
        val pm = PeripheralManager.getInstance()

        val uart = pm.uartDeviceList.first()
        gpio = pm.openGpio("GPIO6_IO13")
        gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

        uartDevice = pm.openUartDevice(uart)
        uartDevice.apply {
            setBaudrate(9600)
            setParity(UartDevice.PARITY_NONE)
            setStopBits(1)
            setHardwareFlowControl(UartDevice.HW_FLOW_CONTROL_NONE)
        }
    }

    fun startReading(): Flow<Data> {
        return flow {
            while (currentCoroutineContext().isActive) {
                buffer.clear()
                val read = uartDevice.read(buffer.array(), buffer.limit())
                if (read == 32) {
                    Log.d("UART", "UART$read =  ${buffer.array().map { it.toUByte() }}")
                    buffer.rewind()
                    decodeData()?.let { emit(it) }
                }
            }
        }.flowOn(Dispatchers.IO).conflate()
    }

    private fun decodeData(): Data? {
        @ExperimentalUnsignedTypes
        val expectedFrameLength = 28
        val expectedControlSum = 0x424d
        val buff = buffer.order(ByteOrder.BIG_ENDIAN).asShortBuffer()

        val controlSum = buff[0].toUShort().toInt()
        val frameLength = buff[1].toUShort().toInt()
        val pm1_0 = buff[2].toUShort().toInt()
        val pm2_5 = buff[3].toUShort().toInt()
        val pm10 = buff[4].toUShort().toInt()
        val pm1_0atm = buff[5].toUShort().toInt()
        val pm2_5atm = buff[6].toUShort().toInt()
        val pm10atm = buff[7].toUShort().toInt()
        val checkSum = buff[15].toUShort()

        val sublist = buffer.array().map { it.toUByte() }.subList(0, 30)
        val sum = sublist.map { it.toUShort() }.reduce { acc, uShort -> (acc + uShort).toUShort() }
        Log.d("CHECKSUM", "$checkSum == $sum")
        if (checkSum == sum && controlSum == expectedControlSum && expectedFrameLength == frameLength) {
            return Data(pm1_0, pm2_5, pm10, pm1_0atm, pm2_5atm, pm10atm)
        }
        return null
    }

}