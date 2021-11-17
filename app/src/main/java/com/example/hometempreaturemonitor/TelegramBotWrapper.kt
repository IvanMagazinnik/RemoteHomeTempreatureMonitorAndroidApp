package com.example.hometempreaturemonitor

import android.util.Log
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.logging.LogLevel
import java.util.*


class TelegramBotWrapper {
    private var lastDate = 0L
    private val bot = bot {

        token = BuildConfig.TELEGRAM_API_KEY
        timeout = 30
        logLevel = LogLevel.Network.Body

        dispatch {
            text {
                var text = ""
                if (message.text != null) {
                    val regex = """.*set_min_temp\s*(\d+\.?\d*)""".toRegex()
                    val matchResult = regex.find(message.text.toString())
                    if (matchResult != null) {
                        try {
                            TemperatureWarnMonitor.setWarnTemp(matchResult.groups[1]!!.value.toString().toFloat())
                            text += "Установлена новая минимальная температура: ${TemperatureWarnMonitor.getWarnTemp()}\n"
                            bot.sendMessage(ChatId.fromId(message.chat.id), text = text)
                        } catch (ex: Exception) {
                            bot.sendMessage(ChatId.fromId(message.chat.id), text = "Неверный формат сообщения. Ожидается например: set_min_temp 8.0")
                        }

                    }
                }

                try {
                    val record = TemperatureDataStorage.instance.getLastRecord()
                    text = "Дата: ${record.date}\nТемпература: ${record.temp}\nВлажность: ${record.humidity}"
                } catch (ex: Exception) {
                    text = "Произошла ошибка при получении результатов из базы данных. Текст ошибки: ${ex.message}"
                }
                Log.i("TelegramBotWrapper",
                    "Отправленно сообщение: $text в чат: ${message.chat.id}")
                bot.sendMessage(ChatId.fromId(message.chat.id), text = text)
                lastDate = message.date
            }

            telegramError {
                println(error.getErrorMessage())
            }
        }
    }

    companion object {
        val instance = TelegramBotWrapper()
    }

    fun main() {
        val currDate = Calendar.getInstance().time
        sendMessage("Система была перезагружена. " +
                "Время запуска: ${currDate.toString()}\n" +
                "Минимальная температура для отправки предупреждения: ${TemperatureWarnMonitor.getWarnTemp()}")
        bot.startPolling()
    }

    fun sendMessage(message: String) {
        bot.sendMessage(ChatId.fromId(BuildConfig.TELEGRAM_CHAT_ID.toLong()), text = message)
    }

    fun getLastDate(): Date {
        return Date(lastDate)
    }
}