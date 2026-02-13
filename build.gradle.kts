plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "4.4.1.3373"
    `maven-publish`
}

group = "com.example"
version = project.findProperty("version") as String? ?: "0.0.1-SNAPSHOT"
description = "toss-backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.3")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Nexus 배포용 (Jenkins에서 -PnexusUrl=... -PnexusUsername=... -PnexusPassword=... 로 전달)
publishing {
    publications {
        create<MavenPublication>("bootJava") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            artifact(tasks.bootJar.get())
        }
    }
    repositories {
        maven {
            val nexusUrl = project.findProperty("nexusUrl") as String? ?: "http://localhost:8081/repository/maven-snapshots/"
            url = uri(nexusUrl)
            isAllowInsecureProtocol = true
            credentials {
                username = project.findProperty("nexusUsername") as String? ?: ""
                password = project.findProperty("nexusPassword") as String? ?: ""
            }
        }
    }
}

// SonarQube 정적 분석 (Jenkins에서 sonar.host.url, sonar.token 주입)
// skipCompile: 이미 Build & Test 단계에서 컴파일되므로 sonar 태스크가 컴파일을 다시 하지 않도록 함 (5.x deprecation 대응)
sonar {
    properties {
        property("sonar.projectKey", "income-backend")
        property("sonar.projectName", "income-backend")
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.gradle.skipCompile", "true")
    }
}
