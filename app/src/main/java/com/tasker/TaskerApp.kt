package com.tasker

import android.app.Application
import com.google.firebase.FirebaseApp
import com.tasker.data.di.appModule
import com.tasker.data.di.repositoryModule
import com.tasker.data.di.syncModule
import com.tasker.data.di.useCaseModule
import com.tasker.data.sync.WorkManagerProvider
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TaskerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        startKoin {
            androidLogger()
            androidContext(this@TaskerApp)
            modules(listOf(appModule, repositoryModule,useCaseModule, syncModule))
        }
    }
}