import com.github.jk1.license.render.InventoryMarkdownReportRenderer

plugins {
    java
    id("com.github.jk1.dependency-license-report") version "1.14"
}

allprojects {
    repositories {
        jcenter()
        mavenCentral()
        mavenLocal()

        maven {
            setUrl("https://oss.jfrog.org/libs-snapshot")
        }
    }

    plugins.withId("java") {
        java {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }

        dependencies {
            configurations.configureEach {
                add(name, enforcedPlatform(project(":dependencyManagement")))
            }
        }
    }
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = "6.6.1"
    distributionType = Wrapper.DistributionType.ALL
    distributionSha256Sum = "11657af6356b7587bfb37287b5992e94a9686d5c8a0a1b60b87b9928a2decde5"
}

tasks {
    val cleanLicenses by registering(Delete::class) {
        delete("licenses")
    }

    val copyLicenses by registering(Copy::class) {
        dependsOn(cleanLicenses)

        from("build/reports/dependency-license")
        into("licenses")
    }

    val generateLicenseReport by existing {
        finalizedBy(copyLicenses)
    }
}
