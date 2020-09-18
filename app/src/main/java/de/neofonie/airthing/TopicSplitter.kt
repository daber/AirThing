package de.neofonie.airthing

import de.neofonie.airthing.sensor.PMSensor

@ExperimentalUnsignedTypes
object TopicSplitter {

    fun split(data: PMSensor.Data): Map<String, String> {
        data.apply {
            return mapOf(
                "pm1.0" to pm1_0.toString(),
                "pm2.5" to pm2_5.toString(),
                "pm10" to pm10.toString(),
                "pm1.0atm" to pm1_0atm.toString(),
                "pm2.5atm" to pm2_5atm.toString(),
                "pm10atm" to pm10atm.toString()
            ).mapKeys { "air/${it.key}" }
        }
    }
}