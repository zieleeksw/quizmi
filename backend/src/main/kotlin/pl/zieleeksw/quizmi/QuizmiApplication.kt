package pl.zieleeksw.quizmi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class QuizmiApplication

fun main(args: Array<String>) {
	runApplication<QuizmiApplication>(*args)
}
