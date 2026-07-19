plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

dependencies {
    implementation(project(":core-model"))
    testImplementation(project(":core-testing"))
    testImplementation(libs.junit)
}
