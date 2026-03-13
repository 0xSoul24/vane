plugins {
    alias(libs.plugins.shadow)
    kotlin("jvm")
    kotlin("kapt")
}

kapt {
    // Explicitly disable annotation processor discovery from the compile classpath for this module
    includeCompileClasspath = false
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "external", "include" to listOf("*.jar"))))
    compileOnly(libs.spotbugsAnnotations)
    implementation(libs.velocity)
    kapt(libs.velocity)
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
    shadowJar {
        dependencies {
            include(dependency("org.bstats:bstats-velocity"))
            include(dependency("org.bstats:bstats-base"))
            include(dependency("org.json:json"))
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
}
repositories {
    mavenCentral()
}