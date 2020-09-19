package de.neofonie.airthing.mqtt

import android.content.Context
import android.util.Log
import de.neofonie.airthing.sensor.PMSensor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence

class MqttPublisher(ctx: Context, uri: String) {
    companion object {
        const val TAG = "MQTT"
    }

    val connectOptions = MqttConnectOptions().apply {
        isCleanSession = true
    }
    val client = MqttAndroidClient(ctx, uri, "PM Sensor",null as MqttClientPersistence?)
    val coroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    fun publish(aFlow: Flow<Map<String, String>>) {
        coroutineScope.launch {
            var attempts =10
            while(true){
                try {
                    delay(1000)
                    client.co { connect(connectOptions) }
                    break
                }catch (e: MqttException){
                    Log.e(TAG, "Exception while attempting to connect", e)
                    attempts--
                    if(attempts == 0){
                        throw e
                    }
                }
            }
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

