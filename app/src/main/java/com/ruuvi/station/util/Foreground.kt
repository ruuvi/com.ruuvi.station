package com.ruuvi.station.util

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import android.os.Handler
import timber.log.Timber

class Foreground : ActivityLifecycleCallbacks {
    private var paused = false
    private val listeners = arrayListOf<ForegroundListener>()
    private val handler = Handler()
    private var check: Runnable? = null

    var isForeground = false
    val isBackground: Boolean
        get() = !isForeground

    fun addListener(listener: ForegroundListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: ForegroundListener) {
        listeners.remove(listener)
    }

    override fun onActivityResumed(activity: Activity) {
        paused = false
        val wasBackground = !isForeground
        isForeground = true

        check?.let { handler.removeCallbacks(it) }

        if (wasBackground) {
            Timber.d("went foreground")
            listeners.forEach {
                try {
                    it.onBecameForeground()
                } catch (exception: Exception) {
                    Timber.e(exception, "Listener threw exception!")
                }
            }
        } else {
            Timber.i("still foreground")
        }
    }

    override fun onActivityPaused(activity: Activity) {
        paused = true
        check?.let { handler.removeCallbacks(it) }
        val runnable = Runnable {
            if (isForeground && paused) {
                isForeground = false
                Timber.d("went background")
                listeners
                    .forEach {
                        try {
                            it.onBecameBackground()
                        } catch (exception: Exception) {
                            Timber.e(exception, "Listener threw exception!")
                        }
                    }
            } else {
                Timber.i("still foreground")
            }
        }
        check = runnable
        handler.postDelayed(runnable, CHECK_DELAY)
    }

    override fun onActivityDestroyed(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    companion object {
        private const val CHECK_DELAY: Long = 500

        @JvmStatic
        private var INSTANCE: Foreground? = null

        /***
         * Initialize like java static
         */
        private fun init(context: Context): Foreground {
            return INSTANCE ?: createInstance(context).also { INSTANCE = it }
        }

        /***
         * Create instance for dependency injection
         */
        fun createInstance(context: Context): Foreground =
            (context.applicationContext as? Application)
                ?.let {
                    val foreground = Foreground()
                    it.registerActivityLifecycleCallbacks(foreground)
                    foreground
                }
                ?: throw IllegalStateException("ForegroundKt is not initialised and cannot obtain the Application object")

        @JvmStatic
        fun get(context: Context): Foreground = INSTANCE ?: init(context)

        @JvmStatic
        fun get(application: Application): Foreground = INSTANCE ?: init(application)

        @JvmStatic
        fun get() = INSTANCE ?: throw IllegalStateException("ForegroundKt is not initialised - invoke at least once with parameterised init/get")
    }
}

interface ForegroundListener {
    fun onBecameForeground()

    fun onBecameBackground()
}