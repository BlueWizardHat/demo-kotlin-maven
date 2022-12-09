package net.bluewizardhat.common.statemachine

enum class Thing {
    A, B, C
}

val stateDefinition: StateDefinition<Thing> = StateDefinition
    .initialState(Thing.A)
    .allow(Thing.A).to(Thing.B, Thing.C)
    .allow(Thing.B).to(Thing.C)
    .terminateAt(Thing.C)

data class Entity(
    var state: Thing? = null
)

data class EntityEvent(private val s: String)

val stateMachine = StateMachine.of(stateDefinition, Entity::state, Entity::state.setter) { entity, prevState ->
    EntityEvent("state changed from $prevState to ${entity.state}")
}
