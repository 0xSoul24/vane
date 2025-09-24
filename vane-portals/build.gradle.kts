plugins {
	id("com.gradleup.shadow") version "9.2.1"
}

dependencies {
	compileOnly(group = "org.json", name = "json", version = "20250517")
}

tasks {
	shadowJar {
		configurations = listOf()
		relocate("org.json", "org.oddlama.vane.external.json")
	}
}
