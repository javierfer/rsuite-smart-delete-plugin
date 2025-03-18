# rsuite-smart-delete-plugin

## Requirements

- **RSE** 6.11.x or higher
- **JDK** 11

## Gradle Setup

Create a gradle.properties file at root level, set the "rsuite_plugins" property to the RSuite installation plugins directory - see gradle.properties.template - and run:

```
./gradlew wrapper -> only once, to download the wrapper
./gradlew clean -> cleans target folder
./gradlew build -> optional, to build the plugin
./gradlew deployLocally -> build and deploy to the RSuite plugins directory.
```

## Purpose

Delete completely folders and content, including references, without leaving orphans.
