plugins {
    alias(libs.plugins.shadow)
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly(project(":vane-core"))
}

tasks {
    shadowJar {
        dependencies {
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        }
        relocate("kotlin", "org.oddlama.vane.external.kotlin")
    }
}

kotlin {
    jvmToolchain(21)
}
