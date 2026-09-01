plugins {
    alias(libs.plugins.shadow)
}

repositories {
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    mavenCentral()
}

dependencies {
    compileOnly(libs.packetEvents) {
        // PacketEvents ships a hard dependency on the ancient netty-all 4.1.72.Final, which is
        // flagged for several CVEs and would shadow the much newer netty provided by Paper.
        // Netty is supplied by the server at runtime, so it is not needed on our classpath.
        exclude(group = "io.netty")
    }
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
