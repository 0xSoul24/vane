// The rest of this module's configuration is applied centrally from the root build script.
//
// This module is deliberately kept off `paperweight`: the only Paper type it names is
// `org.bukkit.Material`, used as an annotation member type. The plain `paper-api` artifact
// provides it, which avoids downloading the Paper dev bundle and building a per-module
// Mojang-mapped server jar (~38 MB of cache) just to resolve one enum.
dependencies {
    compileOnly(rootProject.libs.paperApi)
}
