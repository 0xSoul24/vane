plugins {
    alias(libs.plugins.shadow)
    kotlin("jvm")
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "external", "include" to listOf("*.jar"))))
    compileOnly(libs.spotbugsAnnotations)
    implementation(libs.velocity)
    implementation(libs.bstatsVelocity)
    implementation(libs.bstatsBase)
    implementation(libs.json)
    implementation(rootProject.project(":vane-core"))
    implementation(rootProject.project(":vane-proxy-core"))
    implementation(kotlin("stdlib"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.register<Copy>("copyJar") {
    from(tasks.shadowJar)
    into("${project.rootProject.projectDir}/target")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    rename("(.*)-all.jar", "$1.jar")
}

tasks {
    val velocityPluginVersion = project.version.toString()

    shadowJar {
        dependencies {
            include(dependency("org.bstats:bstats-velocity"))
            include(dependency("org.bstats:bstats-base"))
            include(dependency("org.json:json"))
            include(dependency(rootProject.project(":vane-core")))
            include(dependency(rootProject.project(":vane-proxy-core")))
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        }

        relocate("org.json", "org.oddlama.vane.vane_velocity.external.json")
        relocate("org.bstats", "org.oddlama.vane.vane_velocity.external.bstats")
        relocate("kotlin", "org.oddlama.vane.vane_velocity.external.kotlin")
    }

    build {
        dependsOn("copyJar")
    }

    processResources {
        // Keep this config-cache friendly by passing explicit values instead of project.properties.
        inputs.property("version", velocityPluginVersion)
        filesMatching("velocity-plugin.json") {
            expand(mapOf("version" to velocityPluginVersion))
        }
    }
}
repositories {
    mavenCentral()
}