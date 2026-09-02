// The shadowJar contents rule for this plugin is applied centrally from the root build
// script: it bundles nothing and relocates onto the copies vane-core ships.
plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.json)
    compileOnly(project(":vane-core"))
}
