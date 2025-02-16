package org.octavius.novels.domain

data class Novel(
    val id: Int,
    val titles: List<String>,
    val novelType: String,
    val originalLanguage: String,
    val status: String
)

enum class NovelStatus {
    notReading,
    reading,
    completed,
    planToRead;

    fun toDisplayString(status: NovelStatus): String {
        return when (status) {
            notReading -> "Not Reading"
            reading -> "Reading"
            completed -> "Completed"
            planToRead -> "Plan to Read"
        }
    }
}


