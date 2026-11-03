package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

class AiDistractionPingService {

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    suspend fun generateDistractionPing(
        appName: String,
        currentStreak: Int,
        timesOpenedToday: Int,
        minutesSpentToday: Int
    ): String {
        return try {
            withTimeout(30_000L) {
                val prompt = """
                    You are the strict, no-nonsense AI accountability coach for FocusFlow, an anti-smartphone addiction app.
                    The user has a $currentStreak-day clean streak.
                    They just attempted to scroll on $appName.
                    Stats today: Opened $timesOpenedToday times, $minutesSpentToday minutes consumed.
                    
                    Write a single punchy, strict, challenging intervention message (maximum 2 sentences, under 30 words).
                    Directly challenge their discipline, remind them their $currentStreak-day streak is at stake, and tell them to close the app now.
                    Do not use greetings or pleasantries. Keep it sharp and impactful.
                """.trimIndent()

                val response = model.generateContent(prompt)
                val text = response.text?.trim()
                if (!text.isNullOrBlank()) {
                    text
                } else {
                    fallbackPing(appName, currentStreak)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini distraction ping generation failed", e)
            fallbackPing(appName, currentStreak)
        }
    }

    private fun fallbackPing(appName: String, currentStreak: Int): String {
        return "You're opening $appName again. Your $currentStreak-day streak is on the line. Put the phone down now."
    }

    companion object {
        private const val TAG = "AiDistractionPing"
    }
}
