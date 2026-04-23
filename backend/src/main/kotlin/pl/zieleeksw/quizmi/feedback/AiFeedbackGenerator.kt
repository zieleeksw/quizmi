package pl.zieleeksw.quizmi.feedback

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class AiFeedbackGenerator(
    private val properties: AiFeedbackProperties,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(AiFeedbackGenerator::class.java)

    fun generate(context: AiFeedbackContext): AiFeedbackDto {
        if (!properties.enabled || properties.apiKey.isBlank()) {
            logger.info("[AI-FEEDBACK] External prompt skipped. AI feedback is disabled or API key is missing.")
            return fallbackFeedback(context)
        }

        return runCatching { requestFeedback(context) }
            .getOrElse { exception ->
                logger.warn("[AI-FEEDBACK] External prompt failed. Falling back to guided feedback.", exception)
                fallbackFeedback(context)
            }
    }

    private fun requestFeedback(context: AiFeedbackContext): AiFeedbackDto {
        val targetUrl = "${properties.baseUrl.trimEnd('/')}/chat/completions"
        val prompt = buildPrompt(context)
        val payload = mapOf(
            "model" to properties.model,
            "temperature" to 0.2,
            "max_tokens" to properties.maxTokens.coerceIn(80, 600),
            "messages" to listOf(
                mapOf(
                    "role" to "user",
                    "content" to buildUserMessage(prompt)
                )
            )
        )
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(targetUrl))
            .header("Authorization", "Bearer ${properties.apiKey}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))

        if (properties.siteUrl.isNotBlank()) {
            requestBuilder.header("HTTP-Referer", properties.siteUrl)
        }

        if (properties.appName.isNotBlank()) {
            requestBuilder.header("X-Title", properties.appName)
        }

        logger.info(
            "[AI-FEEDBACK] Sending external prompt. providerUrl={}, model={}, selectedAnswers={}, promptChars={}",
            properties.baseUrl,
            properties.model,
            context.selectedAnswerIds.joinToString(","),
            prompt.length
        )

        val response = HttpClient.newHttpClient()
            .send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())

        logger.info(
            "[AI-FEEDBACK] External prompt response received. status={}, bodyChars={}",
            response.statusCode(),
            response.body().length
        )

        if (response.statusCode() !in 200..299) {
            logger.warn("[AI-FEEDBACK] External provider returned non-success status {}.", response.statusCode())
            return fallbackFeedback(context)
        }

        val root = objectMapper.readTree(response.body())
        val choices = root.path("choices")
        val content = if (choices.isArray && choices.size() > 0) {
            choices[0].path("message").path("content").asText().trim()
        } else {
            ""
        }

        if (content.isBlank()) {
            logger.warn("[AI-FEEDBACK] External provider returned an empty feedback message.")
            return fallbackFeedback(context)
        }

        return AiFeedbackDto(
            feedback = content.take(1200),
            generatedByAi = true
        )
    }

    private fun buildPrompt(context: AiFeedbackContext): String {
        val answerLines = context.answers.joinToString("\n") { answer ->
            val selected = if (answer.selected) "selected" else "not selected"
            val correctness = if (answer.correct) "correct" else "incorrect"
            "- [${answer.id}] ${plainText(answer.content)} ($correctness, $selected)"
        }
        val categories = context.categories.joinToString(", ").ifBlank { "none" }
        val explanation = context.explanation?.let { plainText(it) }?.takeIf { it.isNotBlank() } ?: "none"

        return """
            Question:
            ${plainText(context.prompt)}

            Categories: $categories

            Existing author explanation:
            $explanation

            Answers:
            $answerLines

            Selected answer ids: ${context.selectedAnswerIds.joinToString(", ")}
        """.trimIndent()
    }

    private fun buildUserMessage(prompt: String): String {
        return """
            Act as a concise educational tutor for technical exam preparation.

            Your task is to provide formative feedback for a learner's incorrect answer.
            Diagnose the misunderstanding visible in the selected and missed answers.
            Do not reveal hidden chain-of-thought.
            Do not invent facts beyond the question, answer options, and explanation provided.
            If the context is ambiguous, stay conservative and explain only what is clearly supported.

            Output rules:
            - Use the same language as the question. If unsure, answer in English.
            - Use exactly 3 short parts:
              1. What was misunderstood
              2. Why the correct reasoning is different
              3. One small hint for the next attempt
            - Keep the whole response brief and easy to scan.
            - Do not write a long lecture.
            - Prefer guiding the learner over giving a full ready-made solution.

            $prompt
        """.trimIndent()
    }

    private fun fallbackFeedback(context: AiFeedbackContext): AiFeedbackDto {
        val selectedWrong = context.answers.filter { it.selected && !it.correct }
        val missedCorrect = context.answers.filter { !it.selected && it.correct }
        val parts = mutableListOf<String>()

        if (selectedWrong.isNotEmpty()) {
            parts += "Review the option you selected: it conflicts with the rule tested by this question."
        }

        if (missedCorrect.isNotEmpty()) {
            parts += "Compare your choice with the correct option and focus on the keyword that changes the meaning."
        }

        if (context.explanation.isNullOrBlank()) {
            parts += "Try to explain the correct answer in your own words before moving on."
        } else {
            parts += plainText(context.explanation).take(500)
        }

        return AiFeedbackDto(
            feedback = parts.joinToString(" "),
            generatedByAi = false
        )
    }

    private fun plainText(value: String): String {
        return value
            .replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
