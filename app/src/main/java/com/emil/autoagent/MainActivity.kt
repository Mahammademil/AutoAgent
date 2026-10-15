package com.emil.autoagent

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var commandInput: EditText
    private lateinit var statusText: TextView
    private val SPEECH_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
            setBackgroundColor(Color.parseColor("#121212"))
        }

        val title = TextView(this).apply {
            text = "🤖 Emil AutoAgent"
            textSize = 26f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 24)
        }

        statusText = TextView(this).apply {
            text = "Status: Yoxlanılır..."
            textSize = 15f
            setTextColor(Color.parseColor("#00E676"))
            setPadding(0, 0, 0, 32)
        }

        val permBtn = Button(this).apply {
            text = "⚙️ 1. Əlçatanlıq İcazəsini Ver"
            setBackgroundColor(Color.parseColor("#1E88E5"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        commandInput = EditText(this).apply {
            hint = "Əmri yazın (məs: Suno | List | Kitabxana)"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(20, 40, 20, 40)
        }

        val voiceBtn = Button(this).apply {
            text = "🎤 Səslə Əmr Ver"
            setBackgroundColor(Color.parseColor("#D81B60"))
            setTextColor(Color.WHITE)
            setOnClickListener { startVoiceRecognition() }
        }

        val runBtn = Button(this).apply {
            text = "🚀 TAPŞIRIĞI İCRA ET"
            setBackgroundColor(Color.parseColor("#43A047"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val cmd = commandInput.text.toString()
                parseAndExecute(cmd)
            }
        }

        val helpText = TextView(this).apply {
            text = "\n📌 Necə istifadə etməli?\n" +
                    "• Tətbiq açıb düyməyə basmaq üçün:\n   Suno | List\n" +
                    "• Ardıcıl bir neçə əməliyyat üçün:\n   WhatsApp | Muraz | Salam necəsən | Göndər\n" +
                    "• Koordinata toxunmaq üçün:\n   Suno | tap:500,1200\n" +
                    "• Ekranı sürüşdürmək üçün:\n   Suno | aşağı"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 40, 0, 0)
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(permBtn)
        layout.addView(commandInput)
        layout.addView(voiceBtn)
        layout.addView(runBtn)
        layout.addView(helpText)

        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        if (AgentAccessibilityService.instance != null) {
            statusText.text = "✅ Status: Agent Aktivdir və Tam Səlahiyyətlidir!"
            statusText.setTextColor(Color.parseColor("#00E676"))
        } else {
            statusText.text = "⚠️ Status: İcazə verilməyib! 1-ci düyməyə basıb aktiv edin."
            statusText.setTextColor(Color.parseColor("#FF5252"))
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "az-AZ")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Tapşırığı deyin (Məs: Suno aç List kliklə)")
        }
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "Səs tanıma dəstəklənmir", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            val spokenText = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            // Danışıq dilini avtomatik komanda formatına çevirir
            val formatted = spokenText
                .replace("aç", "|", ignoreCase = true)
                .replace("gir", "|", ignoreCase = true)
                .replace("kliklə", "", ignoreCase = true)
                .replace("bas", "", ignoreCase = true)
            commandInput.setText(formatted)
            parseAndExecute(formatted)
        }
    }

    private fun parseAndExecute(rawCommand: String) {
        val service = AgentAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, "Əvvəlcə Əlçatanlıq icazəsini aktiv edin!", Toast.LENGTH_LONG).show()
            return
        }

        val steps = rawCommand.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        if (steps.isEmpty()) return

        // 1-ci addım: Tətbiqi tap və aç
        val appName = steps[0]
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
        val targetApp = apps.firstOrNull {
            pm.getApplicationLabel(it).toString().contains(appName, ignoreCase = true) ||
            it.packageName.contains(appName, ignoreCase = true)
        }

        var delay = 1000L
        if (targetApp != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetApp.packageName)
            startActivity(launchIntent)
            delay = 4000L // Tətbiqin tam açılmasını gözlə
        } else {
            // Tətbiq deyilsə, birbaşa ekranda axtar
            executeStep(service, appName)
        }

        // Sonrakı addımları növbə ilə icra et
        for (i in 1 until steps.size) {
            val step = steps[i]
            Handler(Looper.getMainLooper()).postDelayed({
                executeStep(service, step)
            }, delay)
            delay += 2500L
        }
    }

    private fun executeStep(service: AgentAccessibilityService, step: String) {
        when {
            step.startsWith("tap:", true) -> {
                val coords = step.substringAfter(":").split(",")
                val x = coords.getOrNull(0)?.toFloatOrNull() ?: 500f
                val y = coords.getOrNull(1)?.toFloatOrNull() ?: 500f
                service.tapCoordinate(x, y)
            }
            step.startsWith("yaz:", true) -> {
                service.inputText(step.substringAfter(":"))
            }
            step.equals("aşağı", true) || step.equals("yuxarı", true) -> {
                service.swipeScreen(step)
            }
            step.equals("geri", true) || step.equals("ana ekran", true) -> {
                service.systemAction(step)
            }
            else -> {
                val clicked = service.clickByText(step)
                if (!clicked) {
                    // Əgər kliklənə bilmədisə, bəlkə mətn sahəsidir, yazı yazmağı yoxla
                    service.inputText(step)
                }
            }
        }
    }
}
