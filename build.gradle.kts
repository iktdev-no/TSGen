plugins {
    kotlin("jvm") version "2.3.21"
    id("maven-publish")
    `java-gradle-plugin`
    `kotlin-dsl`
}

group = "no.iktdev"
version = "1.0-SNAPSHOT"
val named = "ts-gen"

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("tsGenerator") {
            id = "no.iktdev.ts-gen"
            implementationClass = "no.iktdev.tsgen.TsGeneratorPlugin"
            // Denne er viktig for Gradle-menyen:
            displayName = "TS Generator"
            description = "Genererer TypeScript definisjoner fra Kotlin DTO-er"
        }
    }
}


dependencies {
    implementation(gradleApi())
    implementation("com.google.code.gson:gson:2.8.9")
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib"))

    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    // Bruk BOM for å styre alle JUnit-versjoner automatisk
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

val reposiliteUrl = if (version.toString().endsWith("SNAPSHOT")) {
    "https://reposilite.iktdev.no/snapshots"
} else {
    "https://reposilite.iktdev.no/releases"
}

publishing {
    publications {
        // 'pluginMaven' er den magiske tingen du trenger når du bruker java-gradle-plugin
        create<MavenPublication>("plugin") {
            from(components["java"])
            groupId = "no.iktdev"
            artifactId = "ts-gen" // Dette er navnet du refererer til
            version = project.version.toString()
        }
    }
    repositories {
        mavenLocal()
        maven {
            name = "reposilite"
            url = uri(reposiliteUrl)
            credentials {
                username = System.getenv("reposiliteUsername")
                password = System.getenv("reposilitePassword")
            }
        }
    }
}

// Versjons-logikk som er "Lazy" (kjøres kun når versjonen faktisk trengs)
version = providers.provider {
    val ref = System.getenv("GITHUB_REF") ?: ""
    val isSnapshot = ref.endsWith("/master") || ref.endsWith("/main")

    // Hent tag via providers (Gradle 9 sikker måte)
    val latestTag = providers.exec {
        commandLine("git", "describe", "--tags", "--abbrev=0")
    }.standardOutput.asText.getOrElse("0.0.0").trim().removePrefix("v")

    if (isSnapshot) {
        val parts = latestTag.split(".")
        val patch = parts.lastOrNull()?.toIntOrNull()?.plus(1) ?: 1
        val base = if (parts.size >= 2) "${parts[0]}.${parts[1]}" else latestTag

        // Hent commits via providers
        val buildNumber = providers.exec {
            commandLine("git", "rev-list", "v$latestTag..HEAD", "--count")
        }.standardOutput.asText.getOrElse("0").trim().toIntOrNull() ?: 0

        "$base.$patch-SNAPSHOT-$buildNumber"
    } else {
        latestTag
    }
}