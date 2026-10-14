dependencyResolutionManagement {
    versionCatalogs {
        create("config") {
            from(files("gradle/config.versions.toml"))
        }
    }
}
