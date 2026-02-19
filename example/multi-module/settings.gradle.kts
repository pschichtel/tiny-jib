pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }

    includeBuild("../..")
}


include("mod-a")
include("mod-b")