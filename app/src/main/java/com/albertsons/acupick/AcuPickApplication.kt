package com.albertsons.acupick

import android.app.Application
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.ProcessLifecycleOwner
import com.albertsons.acupick.buildconfig.BuildConfigProviderImpl
import com.albertsons.acupick.data.di.ConfigObject
import com.albertsons.acupick.data.di.Data
import com.albertsons.acupick.data.di.NetworkObject
import com.albertsons.acupick.data.di.TokenAuth
import com.albertsons.acupick.data.environment.EnvironmentRepository
import com.albertsons.acupick.di.AppModule
import com.albertsons.acupick.ui.bindingadapters.AlbertsonsDataBindingComponent
import com.jakewharton.processphoenix.ProcessPhoenix
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.logger.Level
import timber.log.Timber

@Suppress("unused")
class AcuPickApplication : Application(), LifecycleObserver {

    private val albertsonsDataBindingComponent: AlbertsonsDataBindingComponent by inject()
    private val environmentRepository: EnvironmentRepository by inject()

    override fun onCreate() {
        super.onCreate()

        if (ProcessPhoenix.isPhoenixProcess(this)) {
            // skip initialization when in the Phoenix process (from environment switcher)
            return
        }
        // Can't use Koin to create this due to necessary logic needed in startKoin for androidLogger. Just create/use an instance here for this special case.
        val buildConfigProvider = BuildConfigProviderImpl()
        if (buildConfigProvider.isDebugOrInternalBuild) {
            Timber.plant(Timber.DebugTree())
        } else {
            // FIXME only for testing
            Timber.plant(ReleaseTree())
        }

        Timber.v("[onCreate]")
        startKoin {
            if (buildConfigProvider.isDebugOrInternalBuild) {
                androidLogger(Level.ERROR) // TODO: Change back to Level.DEBUG/Level.INFO once koin bug is fixed: https://github.com/InsertKoinIO/koin/issues/847#issuecomment-665226544
            }

            androidContext(this@AcuPickApplication)
            modules(
                listOf(
                    AppModule.appModule,
                    Data.dataModule,
                    TokenAuth.tokenAuthModule,
                    NetworkObject.networkModule,
                    ConfigObject.configModule
                )
            )
        }
        DataBindingUtil.setDefaultComponent(albertsonsDataBindingComponent)

        AcuPickMessagingService.createNotificationChannels(this, true)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        stopKoin()
    }

    fun isAppOnForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState
            .isAtLeast(State.STARTED)
    }
}
