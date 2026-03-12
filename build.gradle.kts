import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.compose") version "1.10.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
    @Suppress("DEPRECATION")
    implementation(compose.material3)
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.7.1")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}

tasks.withType<Test> { useJUnitPlatform() }

compose.desktop {
    application {
        mainClass = "com.timewgui.MainKt"

        // macOS: set app name and dock icon so notifications show "TimewGUI" instead of "java"
        if (System.getProperty("os.name").lowercase().contains("mac")) {
            jvmArgs += "-Xdock:name=TimewGUI"
            jvmArgs += "-Xdock:icon=${project.file("src/main/resources/icon.png").absolutePath}"
            jvmArgs += "-Dapple.awt.application.name=TimewGUI"
        }

        val instanceId = project.findProperty("instanceId") as? String
        if (instanceId != null) {
            jvmArgs += "-Dtimewgui.instanceId=$instanceId"
        }

        nativeDistributions {
            modules("java.net.http")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TimewGUI"
            packageVersion = project.findProperty("appVersion") as String
            description = "A modern desktop GUI for Timewarrior"

            macOS {
                iconFile.set(project.file("src/main/resources/icon.icns"))
                bundleID = "com.timewgui"
            }

            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
                debMaintainer = "abuhamza@users.noreply.github.com"
                menuGroup = "Utility"
                appCategory = "utils"
                shortcut = true
            }

            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
        }
    }
}
