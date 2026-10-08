plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "com.github.matheuscruzsouza.nanospring.discovery"
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
    implementation(project(":nano-spring-web"))
    
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
}
