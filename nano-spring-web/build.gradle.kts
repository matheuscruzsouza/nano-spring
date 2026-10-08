plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "com.github.matheuscruzsouza.nanospring.web"
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
    api(libs.nanohttpd)
    api(libs.nanohttpd.nanolets)
    api(libs.gson)
    api(libs.jmustache)
    
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
}
