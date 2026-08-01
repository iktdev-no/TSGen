package no.iktdev.ts

enum class SealedStrategy {
    AS_INTERFACE,          // Lager kun interfacet (og eventuelt union) uten automatisk type-felt
    AS_INTERFACE_WITH_TYPE,// Lager interface + union + automatisk type-felt på subtypene
    ONLY_TYPED             // Lager kun unionen/typene uten base-interface
}