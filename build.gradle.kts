import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(21)
}

group = "net.greybeardedgeek"
version = "1.1.1"

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

    testImplementation(platform("org.junit:junit-bom:5.10.5"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

val generateBuildInfo by tasks.registering {
    val outputFile = layout.buildDirectory.file("generated/buildInfo/build-info.properties")
    outputs.file(outputFile)
    // Always refresh so buildDate reflects the current build.
    outputs.upToDateWhen { false }

    doLast {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        val buildDate = SimpleDateFormat("yyyy-MM-dd").apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        file.writeText(
            """
            version=${project.version}
            buildDate=$buildDate
            """.trimIndent() + "\n"
        )
    }
}

sourceSets {
    named("main") {
        resources.srcDir(layout.buildDirectory.dir("generated/buildInfo"))
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateBuildInfo)
}

val resourceDirPath = rootDir.toPath().toString() + "/src/main/resources";

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            packageName = "DVRA-Treasurers-Report-Generator"
            packageVersion = version.toString()

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
