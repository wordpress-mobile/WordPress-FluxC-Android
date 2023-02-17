package org.wordpress.android.fluxc.network.rest.wpcom.wc.onboarding

data class OnboardingTasksResponse(
    val data: List<TaskGroupDto>
)

data class TaskGroupDto(
    val id: String,
    val title: String?,
    val tasks: List<TaskDto>
)

data class TaskDto(
    val id: String,
    val isComplete: Boolean = false,
    val isVisited: Boolean = false,
    val isActioned: Boolean = false,
    val actionUrl: String?
)

