plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "com.github.matheuscruzsouza.nanospring.media"
    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(project(":nano-spring-core"))
    api(project(":nano-spring-web"))

    compileOnly(libs.smbj)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.smbj)
}
