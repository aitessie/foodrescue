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
        languageVersion.set(JavaLanguageVersion.of(21))
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

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

val springBootVersion = "4.1.0"
val hibernateVersion = "7.4.1.Final"
val jacksonVersion = "3.1.4"
val kotlinVersion = "2.3.21"
val mockitoVersion = "5.23.0"
val postgresqlVersion = "42.7.11"
val springSecurityVersion = "7.1.0"
val testcontainersVersion = "2.0.5"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-validation:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-security:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-restclient:$springBootVersion")
    implementation(
        "org.springframework.boot:spring-boot-starter-oauth2-resource-server:$springBootVersion"
    )
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-liquibase:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-jackson:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-kafka:4.1.0")
    implementation("org.hibernate.orm:hibernate-spatial:$hibernateVersion")
    implementation("tools.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.springframework.kafka:spring-kafka:4.1.0")
    implementation("org.apache.kafka:kafka-clients:4.3.1")

    runtimeOnly("org.postgresql:postgresql:$postgresqlVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation("org.springframework.security:spring-security-test:$springSecurityVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-kafka-test:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test:4.1.0")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers-postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers-kafka:$testcontainersVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.4.0")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
