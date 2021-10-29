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
                "Время запуска: ${currDate.toString()}")
        bot.startPolling()
    }

    fun sendMessage(message: String) {
        bot.sendMessage(ChatId.fromId(BuildConfig.TELEGRAM_CHAT_ID.toLong()), text = message)
    }

    fun getLastDate(): Date {
        return Date(lastDate)
    }
}