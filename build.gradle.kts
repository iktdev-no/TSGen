plugins {
    kotlin("jvm") version "2.2.21"
    id("maven-publish")
    `java-gradle-plugin`
    `kotlin-dsl`
}

val dynamicVersion: String by lazy {
    val githubRef = System.getenv("GITHUB_REF") ?: ""
    val releaseTag = System.getenv("GITHUB_REF_NAME") ?: ""
    val eventName = System.getenv("GITHUB_EVENT_NAME") ?: ""

    // Hvis det er en release eller tag-push i GitHub Actions, bruk taggen direkte
    if ((eventName == "release" || githubRef.startsWith("refs/tags/")) && releaseTag.isNotBlank()) {
        releaseTag.removePrefix("v")
    } else {
        // Lokalt eller ved vanlig push til main/master: Beregn dynamisk SNAPSHOT via git
        try {
            val isSnapshot = githubRef.endsWith("/master") || githubRef.endsWith("/main") || githubRef.isEmpty()
            val latestTag = providers.exec {
                commandLine("git", "describe", "--tags", "--abbrev=0")
            }.standardOutput.asText.get().trim().removePrefix("v")

            if (isSnapshot) {
                val parts = latestTag.split(".")
                val patch = parts.lastOrNull()?.toIntOrNull()?.plus(1) ?: 1
                val base = if (parts.size >= 2) "${parts.first()}.${parts[1]}" else latestTag
                val buildNumber = providers.exec {
                    commandLine("git", "rev-list", "v$latestTag..HEAD", "--count")
                }.standardOutput.asText.get().trim().toIntOrNull() ?: 0

                "$base.$patch-SNAPSHOT-$buildNumber"
            } else {
                latestTag
            }
        } catch (e: Exception) {
            "0.0.1-SNAPSHOT"
        }
    }
}

group = "no.iktdev"
version = dynamicVersion
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

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
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
    // 1. Vi konfigurerer den eksisterende 'pluginMaven' publikasjonen i stedet for å lage en ny
    publications {
        withType<MavenPublication>().configureEach {
            if (name == "pluginMaven") {
                groupId = "no.iktdev"
                artifactId = "ts-gen"
                version = project.version.toString()
            }
        }
    }

    // 2. Repositories er uendret
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
