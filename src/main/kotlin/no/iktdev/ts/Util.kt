package no.iktdev.ts

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

fun File.toClassName(rootPath: String): String {
    val full = this.path.replace(File.separatorChar, '/')
    val idx = full.indexOf(rootPath)
    val relative = full.substring(idx)
    return relative
        .removeSuffix(".class")
        .replace('/', '.')
}

fun KClass<*>.hasProperties(): Boolean =
    this.memberProperties.isNotEmpty()

// sealed subtype = har en sealed supertype
fun KClass<*>.isSealedSubtype(): Boolean =
    this.supertypes
        .mapNotNull { it.classifier as? KClass<*> }
        .any { it.isSealed }