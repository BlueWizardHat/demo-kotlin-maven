package net.bluewizardhat.demoapp.template.api

data class Page<T>(
    val content: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalElements: Long
)
