plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(project(":vane-core"))
}

tasks {
    shadowJar {
        // Unlike the other vane plugins, this one cannot borrow vane-core's shaded Kotlin runtime.
        // Its `bootstrapper` runs during Bootstrap.bootStrap, before plugins are loaded and before
        // Paper wires a class loader group for declared dependencies, so at that point the jar can
        // only resolve classes it carries itself. Dropping this include makes the bootstrapper die
        // with NoClassDefFoundError: org/oddlama/vane/external/kotlin/jvm/internal/Intrinsics, and
        // the whole plugin then fails to load.
        dependencies {
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        }
        relocate("kotlin", "org.oddlama.vane.external.kotlin")
    }
}
