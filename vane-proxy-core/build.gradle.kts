plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.blossom)
    kotlin("jvm")
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("\$VERSION", project.version.toString())
            }
        }
    }
}

dependencies {
    implementation(libs.nightConfig)
    implementation(libs.slf4j)
    implementation(rootProject.project(":vane-core"))
    compileOnly(libs.json)
    implementation(kotlin("stdlib"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    shadowJar {
        dependencies {
            include(dependency("com.electronwill.night-config:toml"))
        }

        relocate("com.electronwill.night-config", "org.oddlama.vane.vane_velocity.external.night-config")
        relocate("org.json", "org.oddlama.vane.external.json")
    }
}
repositories {
    mavenCentral()
}