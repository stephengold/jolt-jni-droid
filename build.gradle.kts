// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}
configurations.configureEach {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds") // to disable caching of snapshots
}