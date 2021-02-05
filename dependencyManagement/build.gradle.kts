import com.github.jk1.license.LicenseReportExtension

plugins {
    `java-platform`
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val DEPENDENCY_BOMS = listOf(
        "com.amazonaws:aws-xray-recorder-sdk-bom:2.8.0",
        "com.fasterxml.jackson:jackson-bom:2.12.1",
        "io.grpc:grpc-bom:1.35.0",
        "io.opentelemetry:opentelemetry-bom:0.15.0",
        "io.zipkin.brave:brave-bom:5.13.3",
        "io.zipkin.reporter2:zipkin-reporter-bom:2.16.3",
        "org.apache.logging.log4j:log4j-bom:2.14.0",
        "org.springframework.boot:spring-boot-dependencies:2.4.2",
        "software.amazon.awssdk:bom:2.15.67"
)

val DEPENDENCY_SETS = listOf(
        DependencySet(
                "com.amazonaws",
                "1.2.1",
                listOf("aws-lambda-java-core")
        ),
        DependencySet(
                "com.amazonaws",
                "3.7.0",
                listOf("aws-lambda-java-events")
        ),
        DependencySet(
                "com.sparkjava",
                "2.9.3",
                listOf("spark-core")
        ),
        DependencySet(
                "com.squareup.okhttp3",
                "4.9.0",
                listOf("okhttp")
        ),
        DependencySet(
                "io.lettuce",
                "6.0.2.RELEASE",
                listOf("lettuce-core")
        ),
        DependencySet(
                "jakarta.annotation",
                "1.3.5",
                listOf("jakarta-annotation-api")
        ),
        DependencySet(
                "org.apache.httpcomponents",
                "4.5.13",
                listOf("httpclient")
        ),
        DependencySet(
                "org.apache.httpcomponents",
                "4.1.4",
                listOf("httpasyncclient")
        ),
        DependencySet(
                "org.jooq",
                "3.12.3",
                listOf("jooq")
        ),
        DependencySet(
                "org.mariadb.jdbc",
                "2.7.1",
                listOf("mariadb-java-client")
        ),
        DependencySet(
                "mysql",
                "8.0.23",
                listOf("mysql-connector-java")
        )
)

javaPlatform {
    allowDependencies()
}

dependencies {
    for (bom in DEPENDENCY_BOMS) {
        api(platform(bom))
    }
    constraints {
        for (set in DEPENDENCY_SETS) {
            for (module in set.modules) {
                api("${set.group}:${module}:${set.version}")
            }
        }
    }
}

rootProject.allprojects {
    plugins.withId("com.github.jk1.dependency-license-report") {
        configure<LicenseReportExtension> {
            val bomExcludes = DEPENDENCY_BOMS.stream()
                    .map { it.substring(0, it.lastIndexOf(':')) }
                    .toArray { length -> arrayOfNulls<String>(length) }
            excludes = bomExcludes
        }
    }
}
