package io.github.kowx712.mmuautoqr.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val userId: String,
    val password: String,
    var isActive: Boolean = true
)
