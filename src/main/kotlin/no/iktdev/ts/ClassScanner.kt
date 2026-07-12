package no.iktdev.ts

import java.io.File
import kotlin.reflect.KClass

class ClassScanner {
    fun scan(packageName: String, classLoader: ClassLoader): List<KClass<*>> {
        val path = packageName.replace('.', '/')
        println("TsGenerator: looking for resources at path: $path")

        val resources = classLoader.getResources(path)

        val classes = mutableListOf<KClass<*>>()

        if (!resources.hasMoreElements()) {
            println("TsGenerator: NO RESOURCES FOUND for path: $path")
        }

        while (resources.hasMoreElements()) {
            val url = resources.nextElement()
            println("TsGenerator: resource URL = $url")

            val file = File(url.toURI())
            println("TsGenerator: resolved file = ${file.absolutePath}, exists=${file.exists()}, isDir=${file.isDirectory}")

            if (file.exists() && file.isDirectory) {
                file.walkTopDown().forEach { f ->
                    if (f.extension == "class") {
                        val className = f.toClassName(path)
                        try {
                            val cls = Class.forName(className, false, classLoader).kotlin
                            classes.add(cls)
                        } catch (_: Throwable) {}
                    }
                }
            }
        }

        return classes
    }
}