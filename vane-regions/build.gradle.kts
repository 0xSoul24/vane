plugins {
	alias(libs.plugins.shadow)
    kotlin("jvm")
}

dependencies {
	implementation(project(":vane-portals"))
	compileOnly(libs.vault)
	compileOnly(libs.json)
    implementation(kotlin("stdlib"))
}

tasks {
    shadowJar {
        configurations = listOf()
        relocate("org.json", "org.oddlama.vane.external.json")
    }
}
repositories {
    mavenCentral()
}
kotlin {
    jvmToolchain(21)
}