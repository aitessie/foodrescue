plugins {
    base
}

group = "com.example.foodrescue"
version = "0.1.0-SNAPSHOT"

subprojects {
    apply(plugin = "jacoco")

    group = rootProject.group
    version = rootProject.version

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
