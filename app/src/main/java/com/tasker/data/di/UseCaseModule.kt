package com.tasker.data.di

import com.tasker.data.domain.*
import org.koin.dsl.module

val useCaseModule = module {
    // Existing use cases
    factory { DeleteTaskUseCase(get(), get()) }
    factory { SyncTasksUseCase(get()) }
    factory { GetTaskDetailUseCase(get(), get()) }
    factory { UpdateTaskStatusUseCase(get()) }
    factory { GetProgressDataUseCase(get()) }
    factory { GetTaskForEditUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { SignInWithGoogleUseCase(get()) }
    factory { SignOutUseCase(get()) }
    factory { ResetPasswordUseCase(get()) }
    factory { GetCurrentUserUseCase(get()) }
    factory { UpdateUserProfileUseCase(get()) }
    factory { DeleteAccountUseCase(get()) }
    factory { CheckAuthStateUseCase(get()) }
    factory { GetCurrentUserIdUseCase(get()) }
    factory { SignInUseCase(get()) }
    factory { SaveTaskUseCase(get(), get()) }
    factory { UpdateStreakUseCase(get(), get(), get(), get()) }
    factory { GetAchievementsUseCase(get(), get()) }

    factory { GetTasksUseCase(get()) }
    factory { GetTaskStatsUseCase(get()) }
    factory { SyncTasksUseCase(get()) }
    factory { ActiveTaskUseCase(get()) }
    factory { RunTaskUseCase(get()) }
}