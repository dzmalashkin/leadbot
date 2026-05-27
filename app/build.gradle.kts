plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")

    // Apply the Application plugin to add support for building an executable JVM application.
    application

    alias(libs.plugins.kotlinPluginSpring)
    alias(libs.plugins.springBoot)
}

dependencies {
    // Project "app" depends on project "utils". (Project paths are separated with ":", so ":utils" refers to the top-level "utils" project.)
    implementation(project(":utils"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}"))
    implementation(libs.springBootStarter)
    implementation(libs.telegramBotsSpringBootLongPollingStarter)
    implementation(libs.telegramBotsClient)
    implementation(kotlin("reflect"))
}

application {
    // Define the Fully Qualified Name for the application main class
    // (Note that Kotlin compiles `App.kt` to a class with FQN `com.example.app.AppKt`.)
    mainClass = "com.ipdim.app.LeadBotApplicationKt"
}

tasks.bootJar {
    archiveFileName = "leadbot.jar"
}
