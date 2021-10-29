package com.example.hometempreaturemonitor

import android.util.Log
import java.util.*

class TemperatureWarnMonitor {
    private var warnSentTime = Date(0)
    private var warnWasDetected = false
    companion object {
        const val CRITICAL_TEMPERATURE = 5.0
        const val WARN_TEMPERATURE = 10.0
        const val TOLERANCE = 1.0
        const val POLL_INTERVAL_MINUTES = 1
        const val WARN_SENT_DELTA_MINUTES = 60
    }

    private fun isSentRecently(): Boolean {
        val currDate = Calendar.getInstance().time
        if (currDate.time - warnSentTime.time > 1000L * 60 * WARN_SENT_DELTA_MINUTES) {
            return false
        }
        return true
    }

    private fun sendWarnMessage(temp: Float) {
        TelegramBotWrapper.instance.sendMessage("Внимание! Низкая температура в доме: $temp C")
    }
    private fun sendTempIsOkMessage(temp: Float) {
        TelegramBotWrapper.instance.sendMessage("Температура Вернулась к нормальному значению: $temp C")
    }

    fun main() {
        while (true) {
            try {
                val tempRecord = TemperatureDataStorage.instance.getLastRecord()
                if (tempRecord.temp.toFloat() < WARN_TEMPERATURE) {
                    warnWasDetected = true
                    if (!isSentRecently()) {
                        warnSentTime = Calendar.getInstance().time
                        sendWarnMessage(tempRecord.temp.toFloat())
                    }
                } else if (tempRecord.temp.toFloat() > WARN_TEMPERATURE + TOLERANCE) {
                    if (warnWasDetected) {
                        warnSentTime = Date(0)
                        warnWasDetected = false
                        sendTempIsOkMessage(tempRecord.temp.toFloat())
                    }
                }
            } catch (ex: Exception) {
                Log.e("TemperatureWarnMonitor", "Failed to get last record")
            }
            Thread.sleep(1000L * 60 * POLL_INTERVAL_MINUTES )
        }
    }
}