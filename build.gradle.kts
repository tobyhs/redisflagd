import com.google.protobuf.gradle.id

val grpcKotlinVersion = project.properties["grpcKotlinVersion"] as String
val kotlinCoroutinesVersion = project.properties["kotlinCoroutinesVersion"] as String
val kotlinVersion = project.properties["kotlinVersion"] as String

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25"
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.micronaut.application") version "4.4.2"
    id("com.google.protobuf") version "0.9.2"
}

version = "0.1"
group = "io.github.tobyhs.redisflagd"

dependencyLocking {
    lockAllConfigurations()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$kotlinCoroutinesVersion")

    implementation("io.micronaut.grpc:micronaut-grpc-runtime:4.+")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime:4.+")
    implementation("io.micronaut.redis:micronaut-redis-lettuce:6.+")

    implementation("jakarta.annotation:jakarta.annotation-api:3.+")

    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")

    runtimeOnly("ch.qos.logback:logback-classic:1.+")

    testImplementation("io.kotest:kotest-assertions-json:5.+")

    testImplementation("com.redis.testcontainers:testcontainers-redis:1.6.4")
    testImplementation("org.testcontainers:testcontainers:1.18.0")
}


application {
    mainClass = "io.github.tobyhs.redisflagd.ApplicationKt"
}

val javaVersion = rootProject.file(".java-version").readText().trim()

java {
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
}

sourceSets {
    main {
        java {
            srcDirs("build/generated/source/proto/main/grpc")
            srcDirs("build/generated/source/proto/main/grpckt")
            srcDirs("build/generated/source/proto/main/java")
        }
    }
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:3.25.4" }
    plugins {
        id("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:1.66.0" }
        id("grpckt") { artifact = "io.grpc:protoc-gen-grpc-kotlin:${grpcKotlinVersion}:jdk8@jar" }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}

micronaut {
    testRuntime("kotest5")
    processing {
        incremental(true)
        annotations("io.github.tobyhs.redisflagd.*")
    }
}

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    jdkVersion = javaVersion
}
