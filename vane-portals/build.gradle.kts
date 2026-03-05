plugins {
    alias(libs.plugins.shadow)
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.json)
    implementation(kotlin("stdlib"))
}

tasks {
    shadowJar {
        dependencies {
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        }
        relocate("org.json", "org.oddlama.vane.external.json")
        relocate("kotlin", "org.oddlama.vane.external.kotlin")
    }
}
repositories {
    mavenCentral()
}
kotlin {
    jvmToolchain(21)
}