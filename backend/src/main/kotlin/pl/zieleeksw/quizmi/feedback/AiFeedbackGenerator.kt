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

        return parseStructuredFeedback(content)
            ?: fallbackFeedback(context)
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
            - Return ONLY valid JSON.
            - Do not use markdown.
            - Do not number sections.
            - Use exactly this schema:
              {"misunderstanding":"...","reasoning":"...","hint":"..."}
            - `misunderstanding` should explain what the learner mixed up.
            - `reasoning` should explain why the correct reasoning is different.
            - `hint` should give one small next-step hint, not a full ready-made solution.
            - Keep the whole response brief and easy to scan.
            - Do not write a long lecture.
            - Prefer guiding the learner over giving a full ready-made solution.

            $prompt
        """.trimIndent()
    }

    private fun fallbackFeedback(context: AiFeedbackContext): AiFeedbackDto {
        val selectedWrong = context.answers.filter { it.selected && !it.correct }
        val missedCorrect = context.answers.filter { !it.selected && it.correct }
        val misunderstanding = when {
            selectedWrong.isNotEmpty() && missedCorrect.isNotEmpty() ->
                "You focused on an option that sounded plausible, but you also missed a key correct idea."
            selectedWrong.isNotEmpty() ->
                "You selected an option that conflicts with the rule tested in this question."
            missedCorrect.isNotEmpty() ->
                "You missed an important correct element of the answer."
            else ->
                "Your answer needs another look at the core concept tested here."
        }

        val reasoning = if (context.explanation.isNullOrBlank()) {
            "The correct reasoning depends on the exact wording of the question and the role of each answer option."
        } else {
            plainText(context.explanation).take(500)
        }

        return AiFeedbackDto(
            misunderstanding = misunderstanding,
            reasoning = reasoning,
            hint = "Compare the correct and incorrect options and look for the one keyword that changes the meaning.",
            generatedByAi = false
        )
    }

    private fun parseStructuredFeedback(content: String): AiFeedbackDto? {
        val parsedJson = parseJsonFeedback(content)
        if (parsedJson != null) {
            return parsedJson
        }

        return parseLabeledFeedback(content)
    }

    private fun parseJsonFeedback(content: String): AiFeedbackDto? {
        val direct = runCatching { objectMapper.readTree(content) }.getOrNull()
        val node = direct ?: extractJsonObject(content)?.let { runCatching { objectMapper.readTree(it) }.getOrNull() }
        val misunderstanding = node?.path("misunderstanding")?.asText()?.trim().orEmpty()
        val reasoning = node?.path("reasoning")?.asText()?.trim().orEmpty()
        val hint = node?.path("hint")?.asText()?.trim().orEmpty()

        if (misunderstanding.isBlank() || reasoning.isBlank() || hint.isBlank()) {
            return null
        }

        return AiFeedbackDto(
            misunderstanding = misunderstanding.take(500),
            reasoning = reasoning.take(700),
            hint = hint.take(300),
            generatedByAi = true
        )
    }

    private fun parseLabeledFeedback(content: String): AiFeedbackDto? {
        val normalized = content.replace("\r", " ").replace("\n", " ")
        val misunderstanding = extractSection(
            normalized,
            "What was misunderstood:",
            "Why the reasoning is different:"
        )
        val reasoning = extractSection(
            normalized,
            "Why the reasoning is different:",
            "Hint:"
        )
        val hint = extractSection(normalized, "Hint:", null)

        if (misunderstanding.isBlank() || reasoning.isBlank() || hint.isBlank()) {
            return null
        }

        return AiFeedbackDto(
            misunderstanding = misunderstanding.take(500),
            reasoning = reasoning.take(700),
            hint = hint.take(300),
            generatedByAi = true
        )
    }

    private fun extractJsonObject(content: String): String? {
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')

        if (start < 0 || end <= start) {
            return null
        }

        return content.substring(start, end + 1)
    }

    private fun extractSection(content: String, startLabel: String, endLabel: String?): String {
        val startIndex = content.indexOf(startLabel, ignoreCase = true)
        if (startIndex < 0) {
            return ""
        }

        val valueStart = startIndex + startLabel.length
        val valueEnd = if (endLabel == null) {
            content.length
        } else {
            content.indexOf(endLabel, startIndex = valueStart, ignoreCase = true).takeIf { it >= 0 } ?: content.length
        }

        return content.substring(valueStart, valueEnd)
            .replace(Regex("""^\s*[\d.)\-:]*\s*"""), "")
            .replace(Regex("\\s+"), " ")
            .trim()
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
