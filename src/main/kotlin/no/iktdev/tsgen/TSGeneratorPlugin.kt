package no.iktdev.tsgen

import no.iktdev.ts.TsGenerator // Importer generatoren din
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import java.io.File

// Interface for brukerkonfigurasjon
interface TsGeneratorExtension {
    val packageName: Property<String>
    val outputDir: Property<File>
}

class TsGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Lag en extension så brukeren kan sette config
        val extension = project.extensions.create("tsGenerator", TsGeneratorExtension::class.java)

        project.tasks.register("generateTs") {
            // Sett avhengighet (valgfritt: gjør at den alltid kjører før compile)
            dependsOn("compileKotlin")

            doLast {
                val pkg = extension.packageName.get()
                val out = extension.outputDir.get()

                // Kall din generator-logikk
                TsGenerator.generate(pkg, out)
            }
        }
    }
}