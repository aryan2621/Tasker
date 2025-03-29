package com.tasker.data.di

import com.tasker.data.db.AppDatabase
import com.tasker.service.GoogleAuthClient
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val appModule = module {
    single { AppDatabase.getDatabase(androidApplication()) }
    single { get<AppDatabase>().taskDao() }
    single { get<AppDatabase>().taskProgressDao() }
    single { get<AppDatabase>().streakDao() }
    single { get<AppDatabase>().achievementDao() }
    single { GoogleAuthClient(androidApplication()) }
}