package de.neofonie.airthing.mqtt

import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun MqttAndroidClient.co( f :MqttAndroidClient.()->IMqttToken ) {
    return suspendCoroutine {
        val token = f.invoke(this)
        token.actionCallback = object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                it.resume(Unit)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable) {
                it.resumeWithException(exception)
            }
        }
    }
}
