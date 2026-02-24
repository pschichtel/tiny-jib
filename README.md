# Tiny Jib

A tiny version of [Google's Jib plugin for Gradle](https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin).

The primary focus of this project is to provide a small and modern version of the upstream Jib Gradle plugin.
While it is currently almost a drop-in replacement for many common use-cases, there is no need for it to stay this way.
This project explicitly does _not_ try to reach feature parity with upstream.

If you are lacking features from upstream, feel free to request them in the [issues](https://github.com/pschichtel/tiny-jib/issues),
including a justification why you think it is reasonable to include that particular functionality.

This plugin has roughly an order of magnitude less code than Jib's official Gradle plugin (~2200 vs ~30000 LoC),
primarily due to the removal of many niche features and the switch to kotlin. That's why this plugin is called
"tiny jib".

## Features

* No support for build systems other than Gradle
* No support for old Gradle versions
* No support for PACKAGED containerization
* No support for plugins/extensions
* No support for Skaffold
* No support for web archives
* No support for inferred auth
* No support for property-based configuration (see below for a work-around)
* No support for Docker image format
* No support for Main class detection
* No support for Gradle versions older than 8
* Limited support for Java versions older than 9
* ...

## Actual Features

* Support for Gradle's Configuration Caching
* Support for Gradle's Task Caching
* Support for source sets other than `main` (e.g., for Kotlin Multiplatform projects)
* Support for parallel builds of multi-module projects

## Usage

```kotlin
plugins {
  // Checkout https://plugins.gradle.org/plugin/tel.schich.tinyjib for the latest version!
  id("tel.schich.tinyjib") version "<use latest>"
}
```

Then run `gradle tinyJibTar`.

### Migration

Here are some hints about how things need to change when migrating from Jib to TinyJib:

* The extension name changes from `jib` to `tinyJib`.
* Task names change as follows:
  * `jib` -> `tinyJibPublish`
  * `jibBuildTar` -> `tinyJibTar`
  * `jibDockerBuild` -> `tinyJibDocker`

To resemble Jib's property-based configuration, you might want to use code similar to this:

```kotlin
tinyJib {
  System.getProperty("jib.container.labels")?.also {
    container.labels = it.split(',').associate { label ->
      label.substringBefore('=') to label.substringAfter('=')
    }
  }

  System.getProperty("jib.from.platforms")?.also {
    from.platforms {
      it.split(',').map { platform ->
        platform {
          os = platform.substringBefore('/')
          architecture = platform.substringAfter('/')
        }
      }
    }
  }
}
```

## Documentation

You can either refer [Jib's upstream documentation](https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin),
most of its documentation still applies to this plugin.

Additionally, there is a derived version of its documentation [available here](docs.md).
