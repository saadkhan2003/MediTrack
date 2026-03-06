package com.meditrack.app.domain.model

enum class DoseStatus {
    TAKEN,
    MISSED,
    PENDING;

    companion object {
        fun fromString(value: String): DoseStatus {
            return entries.firstOrNull { it.name == value } ?: PENDING
        }
    }
}
