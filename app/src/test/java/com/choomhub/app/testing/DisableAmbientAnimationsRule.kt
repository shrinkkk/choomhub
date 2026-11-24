package com.choomhub.app.testing

import android.app.Application
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Mirrors the system "remove animations" setting so infinite ambient effects stay off in tests. */
class DisableAmbientAnimationsRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                val resolver = ApplicationProvider.getApplicationContext<Application>().contentResolver
                Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
                base.evaluate()
            }
        }
}
