package de.neofonie.airthing.mqtt

import android.content.Context
import de.neofonie.airthing.sensor.PMSensor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.MqttMessage

class MqttPublisher(ctx: Context, val uri: String) {
    companion object {
        const val TAG = "MQTT"
    }

    val clientId = "test"
    val client = MqttAndroidClient(ctx, uri, clientId)
    val coroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    fun publish(aFlow: Flow<Map<String, String>>) {
        coroutineScope.launch {
            client.co { connect() }
            while (isActive) {
                aFlow.collect {
                    it.forEach { (k, v) ->
                        client.publish(k, MqttMessage(v.toByteArray()))
                    }
                }
            }
        }
    }

    fun stop() {
        coroutineScope.coroutineContext.cancel()
    }

}

