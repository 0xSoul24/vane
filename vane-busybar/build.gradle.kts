// The shadowJar contents rule for this plugin is applied centrally from the root build
// script: it bundles nothing and relocates onto the copies vane-core ships.
plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(project(":vane-core"))
    compileOnly(libs.json)

    // Optional integrations. Each one lives in its own class under `hooks/`, which is only
    // loaded after the corresponding plugin was found at runtime.
    compileOnly(project(":vane-admin"))
    compileOnly(project(":vane-bedtime"))
    compileOnly(project(":vane-permissions"))
    compileOnly(project(":vane-portals"))
    compileOnly(project(":vane-regions"))
}
