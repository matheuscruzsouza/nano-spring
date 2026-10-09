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
