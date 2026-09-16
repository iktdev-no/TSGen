package no.iktdev.ts.anno

import no.iktdev.ts.MapStrategy

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class TsMapStrategy(
    val value: MapStrategy
)