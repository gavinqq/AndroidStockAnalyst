package com.autostockplan.app.data

import java.io.Serializable

data class ExecutionPlan(
    val id: String, // datetime string used as filename
    val content: String,
    val imagePath: String? = null,
    val countries: String? = null,
    val language: String? = null,
    val model: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
