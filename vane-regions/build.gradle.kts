// The shadowJar contents rule for this plugin is applied centrally from the root build
// script: it bundles nothing and relocates onto the copies vane-core ships.
plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":vane-portals"))
    compileOnly(project(":vane-core"))
    compileOnly(libs.serviceIo)
    compileOnly(libs.json)
}
