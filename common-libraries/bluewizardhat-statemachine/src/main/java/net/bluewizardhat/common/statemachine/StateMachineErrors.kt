package net.bluewizardhat.common.statemachine

sealed class StateException(message: String) : RuntimeException(message)
class InvalidTransitionException(message: String) : StateException(message)
