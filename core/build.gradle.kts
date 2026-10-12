// Plain Kotlin, no Android: parsing and other logic that an iOS app could share later.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(libs.serialization.json)
    testImplementation(libs.junit)
}
