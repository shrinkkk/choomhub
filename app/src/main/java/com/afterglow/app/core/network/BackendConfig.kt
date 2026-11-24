package com.afterglow.app.core.network

import com.afterglow.app.BuildConfig

/**
 * Client-side backend configuration, injected at build time from the
 * git-ignored `local.properties` (see docs/ARCHITECTURE.md -> Configuration).
 *
 * Only public-client values belong here: the Supabase project URL and the
 * publishable (anon) key, whose access is constrained by Row Level Security.
 * Service-role keys and Firebase service-account credentials must never be
 * shipped in the app.
 *
 * Nothing talks to Supabase yet; this exists so later milestones have one
 * place to read configuration from and so Settings can show its status.
 */
object BackendConfig {
    val supabaseUrl: String = BuildConfig.SUPABASE_URL
    val supabasePublishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabasePublishableKey.isNotBlank()
}
