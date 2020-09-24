import com.google.protobuf.gradle.*

plugins {
    `java-library`
    idea
    id("com.google.protobuf") version "0.8.12"
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.12.0"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.29.0"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}

dependencies {
    api("io.grpc:grpc-stub")
    api("io.grpc:grpc-protobuf")
    compileOnly("jakarta.annotation:jakarta.annotation-api:1.3.5")
}
