package de.neofonie.airthing.mqtt

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MqttPublisher(ctx: Context, uri: String) : MqttCallback {
    companion object {
        const val TAG = "MQTT"
    }

    val connectOptions = MqttConnectOptions().apply {
        isCleanSession = true
    }
    val client = MqttAndroidClient(ctx, uri, "PM Sensor", null as MqttClientPersistence?).apply {
        setCallback(this@MqttPublisher)
    }
    val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun publish(aFlow: Flow<Map<String, String>>) {

        coroutineScope.launch {
            aFlow.collect {
                it.forEach { (k, v) ->
                    if (!client.isConnected) {
                        connect()
                    }
                    client.publish(k, MqttMessage(v.toByteArray()))
                }
            }
        }
    }


    private suspend fun connect() {
        while (true) {
            try {
                client.connect(connectOptions).waitForCompletion()
            } catch (e: MqttException) {
                Log.e(TAG, "Error while connecting ", e)
                delay(1000)
                continue
            }
            break
        }

    }

    fun stop() {
        coroutineScope.coroutineContext.cancel()
    }

    override fun connectionLost(cause: Throwable?) {
        Log.w(TAG, "Connection lost")
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {
        Log.i(TAG, "Message arrived for topic $topic")
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        Log.i(TAG, "Delivery completed")
    }

}

