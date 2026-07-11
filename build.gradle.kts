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



dependencies {
    implementation(gradleApi())
    implementation("com.google.code.gson:gson:2.8.9")
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib"))
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
        create<MavenPublication>("reposilite") {
            artifactId = named
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set(named)
                version = project.version.toString()
                url.set(reposiliteUrl)
            }
            from(components["kotlin"])
        }
    }
    repositories {
        mavenLocal()
        maven {
            name = named
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