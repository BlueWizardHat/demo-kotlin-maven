package net.bluewizardhat.common.statemachine

import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function
import kotlin.reflect.KClass

/**
 * Defines the allowed transitions in a state machine.
 *
 * Example:
 * ```kotlin
 * enum class States {
 *     A, B, C
 * }
 * val stateDefinition: StateDefinition<States> = StateDefinition
 *     .initialState(States.A) // If the state is null we assume the state is A
 *     .allow(States.A).to(States.B, States.C) // Allows A->B and A->C
 *     .allow(States.B).to(States.C) // Allows B->C
 *     .terminateAt(States.C) // C is a terminal state
 * ```
 */
class StateDefinition<E : Enum<E>> private constructor(
    val initialState: E,
    val transitions: Map<E, Set<E>>,
    val terminalStates: Set<E>
) {
    companion object {
        fun <E : Enum<E>> initialState(enumClass: KClass<E>, initialState: E): Allow<E> = Builder(enumClass, initialState)

        /**
         * Starts defining the allowed states.
         */
        inline fun <reified E : Enum<E>> initialState(initialState: E): Allow<E> = initialState(E::class, initialState)
    }
    interface Allow<E : Enum<E>> {
        /**
         * Defines the start of a transition.
         */
        fun allow(from: E): To<E>

        /**
         * Defines the terminal states and ends the definition.
         */
        fun terminateAt(vararg terminalStates: E): StateDefinition<E>

        /**
         * Builds the definition with no terminal states.
         */
        fun build(): StateDefinition<E>
    }
    interface To<E : Enum<E>> {
        /**
         * Defines which states are allowed to transition to from the allow condition.
         */
        fun to(vararg to: E): Allow<E>
    }

    private class Builder<E : Enum<E>>(val enumClass: KClass<E>, var initialState: E) : Allow<E>, To<E> {
        private lateinit var fromState: E
        private var transitions: MutableMap<E, Set<E>> = HashMap()
        private var terminalStates: Set<E> = emptySet()

        override fun allow(from: E): To<E> {
            if (transitions.containsKey(from)) {
                throw IllegalStateException("")
            }
            fromState = from
            return this
        }
        override fun to(vararg to: E): Allow<E> {
            if (to.isEmpty()) {
                throw IllegalStateException("")
            }
            transitions[fromState] = setOf(*to)
            return this
        }
        override fun terminateAt(vararg end: E): StateDefinition<E> {
            if (end.isEmpty()) {
                throw IllegalStateException("")
            }
            terminalStates = setOf(*end)
            return build()
        }
        override fun build(): StateDefinition<E> {
            // TODO validate consistency
            if (terminalStates.contains(initialState)) {
                throw IllegalStateException("")
            }
            if (transitions.keys.containsAny(terminalStates)) {
                throw IllegalStateException("")
            }

            return StateDefinition(initialState, transitions, terminalStates)
        }

        private fun <E> Set<E>.containsAny(other: Set<E>): Boolean {
            return other.find { this.contains(it) } != null
        }
    }
}

class StateMachine<E : Enum<E>, S, R> private constructor(
    private val stateDefinition: StateDefinition<E>,
    private val getState: Function<S, E?>,
    private val setState: BiConsumer<S, E>,
    private val stateChangeHandler: BiFunction<S, StateTransition<E>, R>
) {
    companion object {
        fun <E : Enum<E>, S> of(
            stateDefinition: StateDefinition<E>,
            getState: Function<S, E?>,
            setState: BiConsumer<S, E>
        ): StateMachine<E, S, Unit> = StateMachine(stateDefinition, getState, setState) { _, _ -> }

        fun <E : Enum<E>, S, R> of(
            stateDefinition: StateDefinition<E>,
            getState: Function<S, E?>,
            setState: BiConsumer<S, E>,
            stateChangeHandler: BiFunction<S, StateTransition<E>, R>
        ): StateMachine<E, S, R> = StateMachine(stateDefinition, getState, setState, stateChangeHandler)
    }
    data class StateTransition<E : Enum<E>>(
        val from: E?,
        val to: E
    )

    /**
     * Transitions to the initial state if current state is null.
     */
    fun initialize(entity: S, callStateChangeHandler: Boolean = true): R? {
        if (getState.apply(entity) == null) {
            setState.accept(entity, stateDefinition.initialState)
            if (callStateChangeHandler) {
                return stateChangeHandler.apply(entity, StateTransition(null, stateDefinition.initialState))
            }
        }
        return null
    }

    /**
     * Transition an entity to a new state. Will throw an exception if the transition is not allowed.
     *
     * Explicitly allows the transition from 'null' to the initial state and from 'null' to any state
     * allowed from the initial state.
     */
    fun transition(entity: S, newState: E): R {
        val prevState = getState.apply(entity)
        val currentState = prevState ?: stateDefinition.initialState
        if (stateDefinition.terminalStates.contains(currentState)) {
            throw IllegalStateException("$currentState is a terminal state")
        }
        if ((prevState != null || newState != stateDefinition.initialState) &&
            (stateDefinition.transitions[currentState]?.contains(newState) != true)
        ) {
            throw IllegalStateException("Changing from $prevState to $newState is not allowed")
        }
        setState.accept(entity, newState)
        return stateChangeHandler.apply(entity, StateTransition(prevState, newState))
    }

    fun isInitial(entity: S): Boolean {
        val state = getState.apply(entity)
        return state == null || state == stateDefinition.initialState
    }

    fun isTerminal(entity: S): Boolean {
        val state = getState.apply(entity)
        return state != null && stateDefinition.terminalStates.contains(state)
    }
}
