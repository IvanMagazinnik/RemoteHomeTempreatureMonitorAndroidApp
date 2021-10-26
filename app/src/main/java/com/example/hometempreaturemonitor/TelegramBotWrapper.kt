package com.example.hometempreaturemonitor

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.logging.LogLevel


class TelegramBotWrapper {
    fun main() {

        val bot = bot {

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
                    bot.sendMessage(ChatId.fromId(message.chat.id), text = text)
                }

                telegramError {
                    println(error.getErrorMessage())
                }
            }
        }
        bot.startPolling()
    }

    private fun generateUsersButton(): List<List<KeyboardButton>> {
        return listOf(
            listOf(KeyboardButton("Request location (not supported on desktop)", requestLocation = true)),
            listOf(KeyboardButton("Request contact", requestContact = true))
        )
    }
}