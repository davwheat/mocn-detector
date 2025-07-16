package dev.davwheat.mocndetector.di

import android.app.Application
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object TelephonyModule {

    @Provides
    fun providesTelephonyManager(
        application: Application
    ): TelephonyManager = application.getSystemService<TelephonyManager>()!!

    @Provides
    fun providesSubscriptionManager(
        application: Application
    ): SubscriptionManager = application.getSystemService<SubscriptionManager>()!!
}
