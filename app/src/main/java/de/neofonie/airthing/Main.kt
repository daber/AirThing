package de.neofonie.airthing

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.widget.CompoundButton
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.ekn.gruzer.gaugelibrary.ArcGauge
import com.ekn.gruzer.gaugelibrary.Range
import de.neofonie.airthing.mqtt.MqttPublisher
import de.neofonie.airthing.sensor.PMSensor
import kotlinx.android.synthetic.main.main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map

@ExperimentalUnsignedTypes

class Main : AppCompatActivity() {

    private var sensorJob: Job? = null
    private lateinit var gaugePm10: ArcGauge
    private lateinit var gaugePm2_5: ArcGauge
    private lateinit var gaugePm1_0: ArcGauge
    private lateinit var gaugePm10Atm: ArcGauge
    private lateinit var gaugePm2_5Atm: ArcGauge
    private lateinit var gaugePm1_0Atm: ArcGauge
    private lateinit var switch: SwitchCompat
    private val gauges: Set<ArcGauge>
        get() = setOf(gaugePm10, gaugePm1_0, gaugePm2_5, gaugePm10Atm, gaugePm2_5Atm, gaugePm1_0Atm)


    private val sensor = PMSensor()

    private lateinit var publisher: MqttPublisher
    private val handler = Handler()
    private val dimHandler = Handler()

    private fun startObserving() {
        sensorJob?.cancel()
        sensorJob = lifecycleScope.launch {
            publisher = MqttPublisher(this@Main, "tcp://192.168.1.157:1883")

            val flow = sensor.startReading()
            val topicFlow = flow.map { TopicSplitter.split(it) }
            publisher.publish(topicFlow)

            sensor.isEnabled = true
            syncUI()
            delay(10_000)
            flow.collect {
                updateData(it)
            }

        }
    }

    private fun stopObserving() {
        sensor.isEnabled = false
        sensorJob?.cancel()
        publisher.stop()
        syncUI()
    }

    private val onCheckChanged =
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                startObserving()
            } else stopObserving()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.attributes.screenBrightness = 0.0f
        window.attributes = window.attributes
        sensor.setup()
        dimScreenAfterWhile()

        setContentView(R.layout.main)
        root.setOnClickListener {
            window.attributes.screenBrightness = 1f
            window.attributes = window.attributes
            dimScreenAfterWhile()
        }
        switch = findViewById(R.id.uiOnOff)
        gaugePm1_0 = setChart(R.id.pm1_0, "PM 1.0")
        gaugePm2_5 = setChart(R.id.pm2_5, "PM 2.5")
        gaugePm10 = setChart(R.id.pm10, "PM 10")
        gaugePm1_0Atm = setChart(R.id.pm1_0atm, "PM 1.0")
        gaugePm2_5Atm = setChart(R.id.pm2_5atm, "PM 2.5")
        gaugePm10Atm = setChart(R.id.pm10atm, "PM 10")
        switch.setOnCheckedChangeListener(onCheckChanged)
    }


    override fun onStart() {
        super.onStart()
        startObserving()
    }

    private fun dimScreenAfterWhile() {
        dimHandler.removeCallbacksAndMessages(null)
        dimHandler.postDelayed({
            window.attributes.screenBrightness = 0f
            window.attributes = window.attributes
        }, 10 * 1000)
    }


    private fun updateData(data: PMSensor.Data) {
        data.apply {
            gaugePm2_5.value = pm2_5.toDouble()
            gaugePm1_0.value = pm1_0.toDouble()
            gaugePm10.value = pm10.toDouble()

            gaugePm2_5Atm.value = pm2_5atm.toDouble()
            gaugePm1_0Atm.value = pm1_0atm.toDouble()
            gaugePm10Atm.value = pm10atm.toDouble()
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

    private fun syncUI() {
        switch.setOnCheckedChangeListener(null)
        switch.isChecked = sensor.isEnabled
        switch.setOnCheckedChangeListener(onCheckChanged)
        gauges.forEach {
            it.isEnabled = sensor.isEnabled
            if(!sensor.isEnabled) {
                it.value = 0.0
            }
        }
    }

    override fun onStop() {
        super.onStop()
        stopObserving()

        handler.removeCallbacksAndMessages(null)
        dimHandler.removeCallbacksAndMessages(null)
    }


}