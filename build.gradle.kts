import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
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
    implementation("org.apache.commons:commons-text:1.10.0")

    // implementation("com.opencsv:opencsv:5.5.2") -- opencsv has packaging issues - a dependency beanutils, which has a dependency on commons logging
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.2")

    // to display instructions written in markdown
    implementation("com.mikepenz:multiplatform-markdown-renderer-jvm:0.7.0")
}

val resourceDirPath = rootDir.toPath().toString() + "/src/main/resources";

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            packageName = "DVRA Treasurers Report Generator"
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
    }
}
