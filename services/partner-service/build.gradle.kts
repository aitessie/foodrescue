plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    kotlin("plugin.jpa") version "2.3.21"
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.9.0"
}

group = "com.example.foodrescue"

version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

spotless {
    kotlin {
        target("src/**/*.kt")
        targetExclude("**/build/**")

        ktfmt().kotlinlangStyle()
    }

    kotlinGradle {
        target("*.gradle.kts")

        ktfmt().kotlinlangStyle()
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation(
        "org.springframework.boot:" + "spring-boot-starter-security-oauth2-resource-server"
    )
    implementation("org.springframework.data:spring-data-jpa:4.1.0")
    implementation("org.springframework:spring-tx:7.0.8")

    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.modelmapper:modelmapper:3.2.6")
    implementation("org.hibernate.orm:hibernate-core:7.4.5.Final")
    implementation("jakarta.validation:jakarta.validation-api:4.0.0-M1")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test:4.1.0")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test:4.1.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.3.21")
    testImplementation("org.junit.platform:junit-platform-launcher:6.1.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test:4.1.0")
    testImplementation("org.springframework.security:spring-security-test:7.1.0")
    testImplementation("org.springframework.boot:spring-boot-testcontainers:4.1.0")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
    testImplementation("org.assertj:assertj-core:3.27.7")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
