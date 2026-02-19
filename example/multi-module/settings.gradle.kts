rootProject.name = "tiny-jib-example-multi-module"

include(
    ":mod-a",
    ":mod-b",
)

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }

    includeBuild("../..")
}
