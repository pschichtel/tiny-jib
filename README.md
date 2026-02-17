# Tiny Jib

A tiny version of [Google's Jib plugin for Gradle](https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin).

The primary focus of this project is to provide a small and modern version of the upstream Jib Gradle plugin.
While it is currently almost a drop-in replacement for many common use-cases, there is no need for it to stay this way.
This project explicitly does _not_ try to reach feature parity with upstream.

If you are lacking features from upstream, feel free to request them in the [issues](https://github.com/pschichtel/tiny-jib/issues),
including a justification why you think it is reasonable to include that particular functionality.

## Usage

```kotlin
plugins {
  id("tel.schich.tinyjib") version "0.2.0"
}
```

Then run `gradle tinyJibTar`.

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

## Documentation

You can either refer [Jib's upstream documentation](https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin),
most of its documentation still applies to this plugin.

Additionally, there is a derived version of its documentation [available here](docs.md).