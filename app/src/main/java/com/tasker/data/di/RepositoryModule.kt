package com.tasker.data.di

import com.tasker.data.db.StreakManager
import com.tasker.data.repository.AchievementRepository
import com.tasker.data.repository.AchievementRepositoryImpl
import com.tasker.data.repository.AuthRepository
import com.tasker.data.repository.AuthRepositoryImpl
import com.tasker.data.repository.FirebaseRepository
import com.tasker.data.repository.FirebaseRepositoryImpl
import com.tasker.data.repository.StreakRepository
import com.tasker.data.repository.StreakRepositoryImpl
import com.tasker.data.repository.TaskRepository
import com.tasker.data.repository.TaskRepositoryImpl
import org.koin.dsl.module

val repositoryModule = module {
    single { StreakManager(get(), get(), get(), get(), get()) }
    single<FirebaseRepository> { FirebaseRepositoryImpl() }
    single<TaskRepository> { TaskRepositoryImpl(get(), get(), get(),get(),get() ) }
    single<AuthRepository> { AuthRepositoryImpl() }
    single<AchievementRepository> { AchievementRepositoryImpl(get(), get(), get()) }
    single<StreakRepository> { StreakRepositoryImpl(get(), get()) }
}