plugins {
	id("com.gradleup.shadow") version "9.2.1"
}

repositories {
	maven("https://repo.codemc.io/repository/maven-releases/")
	maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
	compileOnly("com.github.retrooper:packetevents-spigot:2.9.5")
	compileOnly(group = "org.json", name = "json", version = "20250517")
}

tasks {
	shadowJar {
		configurations = listOf()
		relocate("org.json", "org.oddlama.vane.external.json")
	}
}
