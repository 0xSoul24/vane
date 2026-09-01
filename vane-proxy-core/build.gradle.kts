plugins {
    alias(libs.plugins.shadow)
}

// The version constant used to be produced by blossom's `javaSources`, but this project has no
// Java sources at all, so nothing was ever templated and `VERSION` compiled to the literal
// string "$VERSION". Generating the file directly is both correct and one plugin lighter.
val generateVersionSource = tasks.register("generateVersionSource") {
    description = "Generates the build-time VERSION constant"
    val versionString = project.version.toString()
    val outputDir = layout.buildDirectory.dir("generated/sources/version/kotlin/main")
    inputs.property("version", versionString)
    outputs.dir(outputDir)
    doLast {
        val packageDir = outputDir.get().asFile.resolve("org/oddlama/vane/proxycore/util")
        packageDir.mkdirs()
        packageDir.resolve("Version.kt").writeText(
            """
            package org.oddlama.vane.proxycore.util

            /** Build-time injected plugin version string. */
            const val VERSION: String = "$versionString"
            """.trimIndent() + "\n"
        )
    }
}

kotlin.sourceSets.named("main") {
    kotlin.srcDir(generateVersionSource)
}

dependencies {
    implementation(libs.nightConfig)
    implementation(libs.slf4j)
    compileOnly(libs.json)
}

tasks {
    shadowJar {
        dependencies {
            include(dependency("com.electronwill.night-config:toml"))
        }

        relocate("com.electronwill.night-config", "org.oddlama.vane.vane_velocity.external.night-config")
        relocate("org.json", "org.oddlama.vane.external.json")
    }
}
