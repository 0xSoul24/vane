plugins {
    `java-library`
    alias(libs.plugins.paperweightUserdev)
    alias(libs.plugins.runPaper) // Adds runServer and runMojangMappedServer tasks for testing
    alias(libs.plugins.dokka)
    kotlin("jvm")
}

dependencies {
    paperweight.paperDevBundle(rootProject.libs.versions.paper)
    implementation(kotlin("stdlib"))

    // Dokka multi-module aggregation
    dokka(project(":vane-admin"))
    dokka(project(":vane-annotations"))
    dokka(project(":vane-bedtime"))
    dokka(project(":vane-core"))
    dokka(project(":vane-enchantments"))
    dokka(project(":vane-permissions"))
    dokka(project(":vane-portals"))
    dokka(project(":vane-proxy-core"))
    dokka(project(":vane-regions"))
    dokka(project(":vane-trifles"))
    dokka(project(":vane-velocity"))
    dokka(project(":vane-geyser-extension"))
}

dokka {
    moduleName.set("Vane")
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
    }
}

kotlin {
    jvmToolchain(25)
}

// We don't need to generate an empty `vane.jar`
tasks.withType<Jar> {
    enabled = false
}


tasks {
    runServer {
        pluginJars(vanePlugins.map { it.tasks.findByName("copyJar")?.inputs?.files })
        jvmArgs(
            "-Xms2G",
            "-Xmx2G",
            "-XX:+AlwaysPreTouch",
            "-XX:+DisableExplicitGC",
            "-XX:+ParallelRefProcEnabled",
            "-XX:+PerfDisableSharedMem",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UseG1GC",
            "-XX:G1HeapRegionSize=8M",
            "-XX:G1HeapWastePercent=5",
            "-XX:G1MaxNewSizePercent=40",
            "-XX:G1MixedGCCountTarget=4",
            "-XX:G1MixedGCLiveThresholdPercent=90",
            "-XX:G1NewSizePercent=30",
            "-XX:G1RSetUpdatingPauseTimePercent=5",
            "-XX:G1ReservePercent=20",
            "-XX:InitiatingHeapOccupancyPercent=15",
            "-XX:MaxGCPauseMillis=200",
            "-XX:MaxTenuringThreshold=1",
            "-XX:SurvivorRatio=32",
            "-Dusing.aikars.flags=https://mcflags.emc.gs",
            "-Daikars.new.flags=true",
            "-Dcom.mojang.eula.agree=true"
        )
    }

    register("runVelocity") {
        description = "Runs Velocity using the :vane-velocity project configuration"
        group = "run velocity"
        dependsOn(":vane-velocity:runVelocity")
    }
}

// Common settings to all subprojects.
subprojects {
    pluginManager.apply("java-library")
    pluginManager.apply("java")
    pluginManager.apply("org.jetbrains.dokka")

    group = "org.oddlama.vane"
    version = "1.21.1"

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://repo.opencollab.dev/main/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.mikeprimm.com/")
        maven("https://repo.codemc.org/repository/maven-public/")
        maven("https://jitpack.io")
        maven("https://api.modrinth.com/maven")
        maven("https://repo.bluecolored.de/releases")
        maven("https://repo.thenextlvl.net/releases")
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(arrayOf("-Xlint:all", "-Xlint:-processing", "-Xdiags:verbose"))
        options.encoding = "UTF-8"
    }

    dependencies {
        compileOnly(rootProject.libs.annotations)
        annotationProcessor(rootProject.libs.annotations)
    }

    configure<org.jetbrains.dokka.gradle.DokkaExtension> {
        moduleName.set(project.name)
        dokkaSourceSets.configureEach {
            // Module-level overview and per-package docs, if the module provides them.
            // The `# Module <name>` heading inside must match moduleName above.
            val moduleDoc = project.file("Module.md")
            if (moduleDoc.exists()) {
                includes.from(moduleDoc)
            }

            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl("https://github.com/oddlama/vane/blob/main/${project.name}/src/main/kotlin")
                remoteLineSuffix.set("#L")
            }
        }
    }
}

// All Paper Plugins + Annotations.
configure(subprojects.filter {
    !listOf("vane-velocity", "vane-proxy-core").contains(it.name)
}) {
    pluginManager.apply("io.papermc.paperweight.userdev")

    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(arrayOf("-Xlint:-this-escape"))
    }

    tasks {
        reobfJar {
            enabled = false
        }
    }
    dependencies {
        paperweight.paperDevBundle(rootProject.libs.versions.paper)
    }
}

// All Projects with jar shadow
configure(subprojects.filter {
    listOf(
        "vane-regions",
        "vane-core",
        "vane-portals",
        "vane-regions",
        "vane-trifles",
        "vane-enchantments",
        "vane-permissions",
        "vane-admin",
        "vane-bedtime"
    ).contains(it.name)
}) {
    tasks.register<Copy>("copyJar") {
        description = "Copies the shadow jar to the target directory"
        evaluationDependsOn(project.path)
        from(tasks.findByPath("shadowJar"))
        into("${project.rootProject.projectDir}/target")
        rename("(.+)-all.jar", "$1.jar")
    }
}


// All Projects except proxies, annotations and Geyser extension.
val vanePlugins = subprojects.filter {
    !listOf("vane-annotations", "vane-velocity", "vane-proxy-core", "vane-geyser-extension").contains(it.name)
}
configure(vanePlugins) {
    tasks {
        named("build") {
            dependsOn("copyJar")
        }

        val projectName = project.name
        val projectVersion = project.version.toString()
        named<ProcessResources>("processResources") {
            inputs.property("name", projectName)
            inputs.property("version", projectVersion)
            filesMatching("**/*plugin.yml") {
                expand(mapOf("name" to projectName, "version" to projectVersion))
            }
        }
    }

    dependencies {
        compileOnly(project(":vane-annotations"))
        annotationProcessor(project(path = ":vane-annotations"))
    }
}

// All paper plugins except core.
configure(subprojects.filter {
    !listOf("vane-annotations", "vane-core", "vane-velocity", "vane-proxy-core").contains(it.name)
}) {
    dependencies {
        // https://imperceptiblethoughts.com/shadow/multi-project/#depending-on-the-shadow-jar-from-another-project
        // In a multi-project build, there may be one project that applies Shadow and another that requires the shadowed
        // JAR as a dependency. In this case, use Gradle's normal dependency declaration mechanism to depend on the
        // shadow configuration of the shadowed project.
        implementation(project(path = ":vane-core", configuration = "shadow"))
        // But also depend on core itself.
        implementation(project(path = ":vane-core"))
    }
}

// All plugins with map integration
configure(subprojects.filter {
    listOf("vane-core", "vane-bedtime", "vane-portals", "vane-regions").contains(it.name)
}) {
    dependencies {
        implementation(rootProject.libs.dynmap)
        implementation(rootProject.libs.bluemap)
    }
}

runPaper {
    disablePluginJarDetection()
}

tasks.register<Delete>("cleanVaneRuntimeTranslations") {
    group = "run paper"
    description = "Deletes generated runtime translation files"
    delete(fileTree("run").matching {
        include("plugins/vane-*/lang-*.yml")
    })
}

tasks.register<Delete>("cleanVaneConfigurations") {
    group = "run paper"
    description = "Deletes generated configuration files"
    delete(fileTree("run").matching {
        include("plugins/vane-*/config.yml")
    })
}

tasks.register<Delete>("cleanVaneStorage") {
    group = "run paper"
    description = "Deletes runtime storage files"
    delete(fileTree("run").matching {
        include("plugins/vane-*/storage.json")
    })
}

tasks.register<Delete>("cleanVane") {
    group = "run paper"
    description = "Deletes all runtime plugin files"
    delete(fileTree("run").matching {
        include("plugins/vane-*/")
    })
}

tasks.register<Delete>("cleanWorld") {
    group = "run paper"
    description = "Deletes generated world folders"
    delete(fileTree("run").matching {
        include(
            "world",
            "world_nether",
            "world_the_end"
        )
    })
}
repositories {
    mavenCentral()
}

subprojects {
    tasks.withType<Jar>().configureEach {
        if (name == "shadowJar") {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            exclude("META-INF/*.kotlin_module")
        }
    }
}
