plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.json)
    compileOnly(project(":vane-core"))
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
