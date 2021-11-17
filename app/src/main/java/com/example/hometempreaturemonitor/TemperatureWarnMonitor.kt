package com.example.hometempreaturemonitor

import android.util.Log
import java.util.*
import android.content.Context.MODE_PRIVATE

import android.content.SharedPreferences




class TemperatureWarnMonitor {
    private var warnSentTime = Date(0)
    private var warnWasDetected = false
    companion object {
        const val CRITICAL_TEMPERATURE = 5.0
        var WARN_TEMPERATURE = 10.0
        const val TOLERANCE = 1.0
        const val POLL_INTERVAL_MINUTES = 1
        const val WARN_SENT_DELTA_MINUTES = 60

        fun getWarnTemp(): Float {
            //val sp: SharedPreferences = MainActivity.activity getSharedPreferences("FILE_NAME", MODE_PRIVATE)
            try {
                return MainActivity.sp!!.getFloat("WARN_TEMPERATURE", WARN_TEMPERATURE.toFloat())
            } catch (ex: Exception) {
                return WARN_TEMPERATURE.toFloat()
            }
        }

        fun setWarnTemp(warnTemp: Float) {
            with(MainActivity.sp!!.edit()) {
                putFloat("WARN_TEMPERATURE", warnTemp)
                apply()
            }
        }
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
                if (tempRecord.temp.toFloat() < getWarnTemp()) {
                    warnWasDetected = true
                    if (!isSentRecently()) {
                        warnSentTime = Calendar.getInstance().time
                        sendWarnMessage(tempRecord.temp.toFloat())
                    }
                } else if (tempRecord.temp.toFloat() > getWarnTemp() + TOLERANCE) {
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