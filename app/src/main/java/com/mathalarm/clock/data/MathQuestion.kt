package com.mathalarm.clock.data

import com.google.gson.annotations.SerializedName

data class MathQuestion(
    @SerializedName("id") val id: Int,
    @SerializedName("category") val category: String,
    @SerializedName("question") val question: String,
    @SerializedName("answer") val answer: Int
)
