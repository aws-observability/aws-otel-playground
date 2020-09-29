import com.github.jk1.license.LicenseReportExtension

plugins {
    `java-platform`
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val DEPENDENCY_BOMS = listOf(
        "com.amazonaws:aws-xray-recorder-sdk-bom:2.6.1",
        "com.fasterxml.jackson:jackson-bom:2.11.0",
        "io.grpc:grpc-bom:1.29.0",
        "io.zipkin.brave:brave-bom:5.12.3",
        "io.zipkin.reporter2:zipkin-reporter-bom:2.15.0",
        "org.apache.logging.log4j:log4j-bom:2.13.3",
        "org.springframework.boot:spring-boot-dependencies:2.2.7.RELEASE",
        "software.amazon.awssdk:bom:2.13.17"
)

val DEPENDENCY_SETS = listOf(
        DependencySet(
                "com.amazonaws",
                "1.2.1",
                listOf("aws-lambda-java-core")
        ),
        DependencySet(
                "com.amazonaws",
                "3.2.0",
                listOf("aws-lambda-java-events")
        ),
        DependencySet(
                "com.sparkjava",
                "2.9.2",
                listOf("spark-core")
        ),
        DependencySet(
                "com.squareup.okhttp3",
                "4.7.2",
                listOf("okhttp")
        ),
        DependencySet(
                "io.lettuce",
                "5.3.1.RELEASE",
                listOf("lettuce-core")
        ),
        DependencySet(
                "io.opentelemetry",
                "0.9.0-20200925.025016-49",
                listOf(
                        "opentelemetry-api",
                        "opentelemetry-exporters-logging",
                        "opentelemetry-exporters-otlp"
                )
        ),
        DependencySet(
                "io.opentelemetry",
                "0.9.0-20200925.025016-49",
                listOf(
                        "opentelemetry-context-prop",
                        "opentelemetry-extension-trace-propagators",
                        "opentelemetry-proto",
                        "opentelemetry-sdk",
                        "opentelemetry-sdk-extension-aws-v1-support"
                )
        ),
        DependencySet(
                "io.opentelemetry",
                "0.9.0-20200925.025016-49",
                listOf(
                        "opentelemetry-sdk-common",
                        "opentelemetry-sdk-correlation-context",
                        "opentelemetry-sdk-metrics",
                        "opentelemetry-sdk-tracing"
                )
        ),
        DependencySet(
                "jakarta.annotation",
                "1.3.5",
                listOf("jakarta-annotation-api")
        ),
        DependencySet(
                "org.apache.httpcomponents",
                "4.5.12",
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
                "2.6.0",
                listOf("mariadb-java-client")
        ),
        DependencySet(
                "mysql",
                "8.0.20",
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
