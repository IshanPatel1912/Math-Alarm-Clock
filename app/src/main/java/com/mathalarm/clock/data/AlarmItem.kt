package com.mathalarm.clock.data

data class AlarmItem(
    val id: Long = System.currentTimeMillis(),
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val questionsCount: Int = 3,
    val isEnabled: Boolean = true,
    val daysOfWeek: Set<Int> = emptySet() // 1 = Sunday, 2 = Monday ... Calendar.DAY_OF_WEEK
) {
    val formattedTime: String
        get() {
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            return String.format("%02d:%02d", h, minute)
        }

    val amPm: String
        get() = if (hour < 12) "AM" else "PM"

    val daysSummary: String
        get() {
            if (daysOfWeek.isEmpty()) return "Once"
            if (daysOfWeek.size == 7) return "Every day"
            if (daysOfWeek == setOf(2, 3, 4, 5, 6)) return "Weekdays"
            if (daysOfWeek == setOf(1, 7)) return "Weekends"
            val names = listOf("", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            return daysOfWeek.sorted().joinToString(" ") { names[it] }
        }
}
