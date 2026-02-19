plugins {
    kotlin("jvm")
}
dependencies {
    implementation(kotlin("stdlib"))
}
repositories {
    mavenCentral()
}
kotlin {
    jvmToolchain(21)
}