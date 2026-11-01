plugins {
    alias(libs.plugins.android.library) apply false
}

subprojects {
    afterEvaluate {
        if (plugins.hasPlugin("com.android.library")) {
            extensions.configure<com.android.build.gradle.LibraryExtension>("android") {
                compileSdk = 36
                defaultConfig {
                    minSdk = 25
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }
            }
        }

        if (plugins.hasPlugin("maven-publish")) {
            val refVersion = System.getenv("GITHUB_REF_NAME")?.removePrefix("v")
                ?.takeIf { it.matches(Regex("^[0-9].*")) }
            extensions.configure<PublishingExtension>("publishing") {
                components.matching { it.name == "release" }.all {
                    val component = this
                    publications.create<MavenPublication>("release") {
                        from(component)
                        groupId = "com.github.matheuscruzsouza"
                        artifactId = project.name
                        version = System.getenv("VERSION") ?: refVersion ?: "2.4.1"
                    }
                }
                repositories {
                    maven {
                        name = "GitHubPackages"
                        url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "matheuscruzsouza/nano-spring"}")
                        credentials {
                            username = System.getenv("GITHUB_ACTOR") ?: System.getenv("USERNAME")
                            password = System.getenv("GITHUB_TOKEN") ?: System.getenv("TOKEN")
                        }
                    }
                }
            }
        }
    }
}


subprojects {
    apply(plugin = "checkstyle")

    configure<org.gradle.api.plugins.quality.CheckstyleExtension> {
        toolVersion = "10.12.5"
        configFile = rootProject.file("checkstyle.xml")
        isShowViolations = true
        isIgnoreFailures = false
    }

    tasks.register<org.gradle.api.plugins.quality.Checkstyle>("checkstyle") {
        source("src/main/java")
        exclude("**/R.java", "**/BuildConfig.java", "**/internal/**")
        classpath = files()
    }
}
