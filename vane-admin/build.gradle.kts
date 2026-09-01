plugins {
    alias(libs.plugins.shadow)
}

dependencies {
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
