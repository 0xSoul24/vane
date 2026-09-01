plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":vane-portals"))
    compileOnly(project(":vane-core"))
    compileOnly(libs.serviceIo)
    compileOnly(libs.json)
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
