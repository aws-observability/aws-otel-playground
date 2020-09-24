import de.undercouch.gradle.tasks.download.Download

plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("de.undercouch.download") version "4.1.1"
}

tasks {

    val downloadAgent by registering(Download::class) {
        src("https://github.com/anuraaga/aws-opentelemetry-java-instrumentation/releases/download/v0.9.0-alpha.1/aws-opentelemetry-agent.jar")
        dest("$buildDir/layers/javaagent/aws-opentelemetry-agent.jar")
        onlyIfModified(true)
    }

    val createAgentLayer by registering(Zip::class) {
        dependsOn(downloadAgent)

        archiveFileName.set("aws-opentelemetry-agent-layer.zip")
        destinationDirectory.set(file("$buildDir/distributions"))

        from("build/layers/javaagent")
    }

    val assemble by existing {
        dependsOn("shadowJar")
        dependsOn(createAgentLayer)
    }
}

dependencies {
    implementation("com.amazonaws:aws-lambda-java-core")
    implementation("com.amazonaws:aws-lambda-java-events")
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.httpcomponents:httpclient")
    implementation("commons-logging:commons-logging:1.1.1")

    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl")
}
