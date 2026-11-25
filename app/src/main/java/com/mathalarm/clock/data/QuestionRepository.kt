package com.mathalarm.clock.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import kotlin.random.Random

class QuestionRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("math_questions_prefs", Context.MODE_PRIVATE)
    private val allQuestions: MutableList<MathQuestion> = mutableListOf()

    init {
        loadQuestionsFromAssets()
    }

    private fun loadQuestionsFromAssets() {
        try {
            context.assets.open("questions.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val type = object : TypeToken<List<MathQuestion>>() {}.type
                    val list: List<MathQuestion> = Gson().fromJson(reader, type)
                    allQuestions.clear()
                    allQuestions.addAll(list)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: populate with some dynamic questions if assets fail
            for (i in 1..20) {
                allQuestions.add(generateAlgorithmicQuestion(i))
            }
        }
    }

    private fun getUsedQuestionIds(): MutableSet<String> {
        val set = prefs.getStringSet("used_question_ids", emptySet()) ?: emptySet()
        return set.toMutableSet()
    }

    private fun saveUsedQuestionIds(ids: Set<String>) {
        prefs.edit().putStringSet("used_question_ids", ids).apply()
    }

    fun getQuestions(count: Int): List<MathQuestion> {
        if (allQuestions.isEmpty()) {
            return (1..count).map { generateAlgorithmicQuestion(it) }
        }

        val usedIds = getUsedQuestionIds()
        var available = allQuestions.filter { it.id.toString() !in usedIds }

        // If we ran out of unused questions, reset used pool so we recycle without repeating within a session
        if (available.size < count) {
            usedIds.clear()
            saveUsedQuestionIds(usedIds)
            available = allQuestions
        }

        val selected = available.shuffled().take(count)
        val updatedUsed = usedIds.apply {
            addAll(selected.map { it.id.toString() })
        }
        saveUsedQuestionIds(updatedUsed)

        return selected
    }

    private fun generateAlgorithmicQuestion(id: Int): MathQuestion {
        return when (Random.nextInt(4)) {
            0 -> {
                // Difference of squares mental shortcut: a + b = 100 -> (a - b) * 100
                val b = Random.nextInt(10, 48)
                val a = 100 - b
                val ans = (a - b) * 100
                MathQuestion(id, "Shortcut", "Mental shortcut: $a² - $b²", ans)
            }
            1 -> {
                // Mental multiplication by 11
                val n = Random.nextInt(21, 85)
                val ans = n * 11
                MathQuestion(id, "Shortcut", "Mental multiplication: $n × 11", ans)
            }
            2 -> {
                // Mental balance: x + y = S, x - y = D -> x = (S + D) / 2
                val x = Random.nextInt(15, 45)
                val y = Random.nextInt(5, x - 2)
                val s = x + y
                val d = x - y
                MathQuestion(id, "Mental Algebra", "If x + y = $s and x - y = $d, what is x?", x)
            }
            else -> {
                // Square of number ending in 5
                val tens = Random.nextInt(1, 10)
                val n = tens * 10 + 5
                val ans = n * n
                MathQuestion(id, "Shortcut", "Compute mental square: $n²", ans)
            }
        }
    }
}
