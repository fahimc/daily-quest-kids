plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

application {
    mainClass = "com.dailyquestkids.content.tools.ContentPipelineKt"
}

dependencies {
    implementation(project(":core-model"))
    implementation(project(":core-testing"))
    implementation(project(":puzzle-validator"))
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
