package net.bluewizardhat.demoapp.template.mapping

import net.bluewizardhat.demoapp.template.api.Page
import org.springframework.data.domain.Page as SpringPage

object PageMapper {
    fun <T, R> SpringPage<T>.toApiPage(elementMapper: (T) -> R): Page<R> =
        Page(
            content = content.map(elementMapper),
            page = pageable.pageNumber,
            pageSize = pageable.pageSize,
            totalPages = totalPages,
            totalElements = totalElements
        )
}
