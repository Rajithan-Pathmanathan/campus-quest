package com.campusquest.domain.model

data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val isAnonymous: Boolean = false
)
