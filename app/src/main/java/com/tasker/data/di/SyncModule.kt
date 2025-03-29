package com.tasker.data.di

import com.tasker.data.sync.SyncManager
import com.tasker.data.sync.WorkManagerProvider
import com.tasker.util.NetworkConnectivityObserver
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val syncModule = module {
    single { NetworkConnectivityObserver(androidContext()) }

    single {
        SyncManager(
            taskRepository = get(),
            achievementRepository = get(),
            streakRepository = get(),
            authRepository = get(),
            networkConnectivityObserver = get()
        )
    }

    single { WorkManagerProvider(androidContext()) }
}