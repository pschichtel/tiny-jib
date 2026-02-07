# Tiny Jib

A tiny version of [Google's Jib plugin for Gradle](https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin).

## Features

* No support for build systems other than Gradle
* No support for old Gradle versions
* No support for PACKAGED containerization
* No support for plugins/extensions
* No support for Skaffold
* No support for web archives
* No support for inferred auth
* No support for property-based configuration
* No support for Docker image format
* No support for Main class detection
* Limited support for Java versions older than 9
* ...

## Actual Features

* Support for Gradle's Configuration Caching
* Support for Gradle's Task Caching
* Support for source sets other than `main` (e.g., for Kotlin Multiplatform projects)