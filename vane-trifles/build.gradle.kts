plugins {
	alias(libs.plugins.shadow)
    kotlin("jvm")
}

repositories {
	maven("https://repo.codemc.io/repository/maven-releases/")
	maven("https://repo.codemc.io/repository/maven-snapshots/")
    mavenCentral()
}

dependencies {
	compileOnly(libs.packetEvents)
	compileOnly(libs.json)
    implementation(kotlin("stdlib"))
}

tasks {
	shadowJar {
		configurations = listOf()
		relocate("org.json", "org.oddlama.vane.external.json")
	}
}
kotlin {
    jvmToolchain(21)
}