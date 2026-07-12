package no.iktdev.tsgen

import no.iktdev.ts.TsGenerator
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import java.io.File
import java.net.URLClassLoader

interface TsGeneratorExtension {
    val packageName: Property<String>
    val outputFile: Property<File>
}

class TsGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("tsGenerator", TsGeneratorExtension::class.java)

        project.tasks.register("generateTs") {
            group = "typescript"
            dependsOn("compileKotlin")

            doLast {
                TsGenerator.versionInfo = project.version.toString()
                TsGenerator.buildTime = java.time.Instant.now().toString()

                val pkg = extension.packageName.get()
                val out = extension.outputFile.get() // Henter File direkte

                // Hent classpath for å lage ClassLoaderen
                val mainSourceSet = project.extensions.getByType(JavaPluginExtension::class.java)
                    .sourceSets.getByName("main")

                val urls = mainSourceSet.runtimeClasspath.map { it.toURI().toURL() }.toTypedArray()
                val cl = URLClassLoader(urls, TsGenerator::class.java.classLoader)


                // Kall din signatur: generate(packageName, output, classLoader)
                TsGenerator.generate(pkg, out, cl)
            }
        }
    }
}