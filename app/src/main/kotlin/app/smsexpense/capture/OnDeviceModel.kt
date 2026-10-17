package app.smsexpense.capture

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation

/**
 * Gemini Nano through Android's AICore. The model runs inside the phone's operating system;
 * the message is never sent anywhere. On phones without it, [read] returns null and nothing changes.
 */
object OnDeviceModel {
    private val model by lazy { Generation.getClient() }

    suspend fun read(prompt: String): String? = runCatching {
        if (model.checkStatus() != FeatureStatus.AVAILABLE) return null
        model.generateContent(prompt).candidates.firstOrNull()?.text
    }.getOrNull()
}
