import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(21)
}

group = "net.greybeardedgeek"
version = "1.0.2"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.components:components-resources-desktop:${property("compose.version")}")
    implementation("org.apache.commons:commons-text:1.10.0")

    // implementation("com.opencsv:opencsv:5.5.2") -- opencsv has packaging issues - a dependency beanutils, which has a dependency on commons logging
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.2")

    // to display instructions written in markdown
    implementation("com.mikepenz:multiplatform-markdown-renderer-jvm:0.43.0")
    implementation("com.mikepenz:multiplatform-markdown-renderer-m2:0.43.0")
}

val resourceDirPath = rootDir.toPath().toString() + "/src/main/resources";

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            packageName = "DVRA-Treasurers-Report-Generator"
            packageVersion = "1.0.2"

            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            macOS {
                iconFile.set(project.file(resourceDirPath + "/icon.icns"))
            }
            windows {
                iconFile.set(project.file(resourceDirPath + "/icon.ico"))
            }
            linux {
                iconFile.set(project.file(resourceDirPath + "/icon.png"))
            }

        }

        buildTypes.release.proguard {
            // JetBrains markdown (via multiplatform-markdown-renderer) references
            // ArrayList.removeLast() (Java 21+). Keep packaging resilient if ProGuard
            // analyzes against an older JDK bootstrap classpath.
            configurationFiles.from(project.file("compose-desktop.pro"))
        }
    }
}
