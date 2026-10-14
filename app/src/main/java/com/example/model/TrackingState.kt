package com.example.model

enum class TrackingState {
    STOPPED,
    STARTING,
    ACTIVE,
    WAITING_FOR_PERMISSION,
    LOCATION_DISABLED,
    TEMPORARILY_UNAVAILABLE,
    ERROR
}
