plugins {
    alias(libs.plugins.shadow)
}
val id = project.property("id") as String
val extensionName = project.property("name") as String
val author = project.property("author") as String
val version = project.version as String

// Resolve the Geyser version now and strip any qualifier like "-SNAPSHOT"
val geyserApiVersion: String = rootProject.libs.versions.geyser.get().substringBefore("-")

repositories {
    // Add other repositories here (main repo moved to root build.gradle.kts)
    mavenCentral()
}

dependencies {
    // Geyser API - requested via the version catalog
    compileOnly(rootProject.libs.geyserApi)

    // Include other dependencies here - e.g. configuration libraries.
}

// These are plain constants from gradle.properties, so they can be validated right away
// instead of deferring to afterEvaluate.
require(Regex("[a-z][a-z0-9-_]{0,63}").matches(id)) {
    "Invalid extension id $id! Must only contain lowercase letters, and cannot start with a number."
}
require(Regex("^[A-Za-z_.-]+$").matches(extensionName)) {
    "Invalid extension name $extensionName! Must only contain letters, dots, dashes and underscores."
}

tasks {
    // This automatically fills in the extension.yml file.
    // The values are copied into locals first: capturing the script-level properties directly
    // makes the task hold a reference to the build script, which the configuration cache
    // cannot serialize.
    val extensionId = id
    val displayName = extensionName
    val extensionAuthor = author
    val extensionVersion = version
    val apiVersion = geyserApiVersion
    processResources {
        inputs.property("id", extensionId)
        inputs.property("name", displayName)
        inputs.property("api", apiVersion)
        inputs.property("version", extensionVersion)
        inputs.property("author", extensionAuthor)
        filesMatching("extension.yml") {
            expand(
                "id" to extensionId,
                "name" to displayName,
                "api" to apiVersion,
                "version" to extensionVersion,
                "author" to extensionAuthor
            )
        }
    }

    // Disable the default plain jar so we only output a single jar file
    // and configure the shadowJar (fat jar) to not use the "-all" classifier
    // so the produced artifact matches the normal jar naming.
    named("jar") {
        enabled = false
    }

    // Configure the shadow/fat jar: include kotlin stdlib and relocate it
    // so the extension doesn't conflict with other plugins at runtime.
    shadowJar {
        // Ensure the shadow/fat jar has no "-all" classifier so the artifact
        // matches the standard jar name (single output file).
        archiveClassifier.set("")

        dependencies {
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        }

        relocate("kotlin", "org.oddlama.vane.vane_geyser_extension.external.kotlin")
    }

    // Make assemble produce the shadow jar
    named("assemble") {
        dependsOn(named("shadowJar"))
    }

    // Copy produced shadow jar to the repository target directory like other modules
    // (registered at top-level below to avoid task container receiver issues)
}
// Register copyJar at top-level so we can reference the shadowJar task provider
tasks.register<Copy>("copyJar") {
    description = "Copy the produced shadow jar to the repository target directory"
    from(tasks.named("shadowJar"))
    into(rootProject.layout.projectDirectory.dir("target"))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    rename("(.*)-all.jar", "$1.jar")
}

// Ensure build depends on copyJar so the artifact is placed in /target
tasks.named("build") {
    dependsOn("copyJar")
}
