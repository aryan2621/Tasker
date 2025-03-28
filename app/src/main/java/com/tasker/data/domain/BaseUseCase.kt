// BaseUseCase.kt
package com.tasker.data.domain

import kotlinx.coroutines.flow.Flow

interface UseCase<in Params, out Type> {
    suspend operator fun invoke(params: Params): Type
}

interface FlowUseCase<in Params, out Type> {
    operator fun invoke(params: Params): Flow<Type>
}

abstract class NoParamsUseCase<out Type> : UseCase<Unit, Type> {
    abstract suspend fun execute(): Type

    override suspend operator fun invoke(params: Unit): Type = execute()
}

abstract class NoParamsFlowUseCase<out Type> : FlowUseCase<Unit, Type> {
    abstract fun execute(): Flow<Type>

    override operator fun invoke(params: Unit): Flow<Type> = execute()
}