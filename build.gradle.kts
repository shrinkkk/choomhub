// Top-level build file. Plugins are declared here once and applied per module.
// Note: AGP 9 compiles Kotlin itself ("built-in Kotlin"), so the
// org.jetbrains.kotlin.android plugin is intentionally absent.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
