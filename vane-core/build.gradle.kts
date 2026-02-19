import java.security.MessageDigest

plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.blossom) // Text replacement for version numbers
}

sourceSets {
    main {
        blossom {
            javaSources {
                property($$"$VERSION", project.version.toString())
            }
        }
    }
}

dependencies {
    implementation(libs.bstatsBase)
    implementation(libs.bstatsBukkit)
    implementation(libs.reflections)
    implementation(libs.commonsLang)
    implementation(libs.commonsText)
    api(libs.json)
    implementation(project(":vane-annotations"))
}

val resourcePackSha1: String by lazy {
    val resourcePack = File("${projectDir}/../docs/resourcepacks/v" + project.version + ".zip")
    if (!resourcePack.exists()) {
        throw GradleException("The resource pack file $resourcePack is missing.")
    }
    val md: MessageDigest = MessageDigest.getInstance("SHA-1")
    val resourcePackBytes: ByteArray = resourcePack.readBytes()
    md.update(resourcePackBytes, 0, resourcePackBytes.size)
    val sha1Bytes: ByteArray = md.digest()
    val sha1HashString: String = String.format("%040x", BigInteger(1, sha1Bytes))
    sha1HashString
}

val projectProperties: MutableMap<String, *> = project.properties

tasks {
    shadowJar {
        dependencies {
            include(dependency("org.bstats:bstats-base"))
            include(dependency("org.bstats:bstats-bukkit"))
            include(dependency("org.reflections:reflections"))
            include(dependency("org.json:json"))
            include(dependency(":vane-annotations"))
            include(dependency("org.apache.commons:commons-lang3"))
            include(dependency("org.apache.commons:commons-text"))
        }
        relocate("org.bstats", "org.oddlama.vane.external.bstats")
        relocate("org.reflections", "org.oddlama.vane.external.reflections")
        relocate("org.json", "org.oddlama.vane.external.json")
        relocate("org.apache.commons.lang3", "org.oddlama.vane.external.apache.commons.lang3")
        relocate("org.apache.commons.text", "org.oddlama.vane.external.apache.commons.text")
    }

    processResources {
        filesMatching("vane-core.properties") {
            expand(projectProperties + mapOf("resourcePackSha1" to resourcePackSha1))
        }
    }
}