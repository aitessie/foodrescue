plugins {
	base
//    id("org.jlleitschuh.gradle.ktlint") version "12.3.0" apply false
}

group = "com.example.foodrescue"
version = "0.1.0-SNAPSHOT"

subprojects {
	apply(plugin = "jacoco")

	group = rootProject.group
	version = rootProject.version

//    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
//        apply(plugin = "org.jlleitschuh.gradle.ktlint")
//    }

	tasks.withType<Test>().configureEach {
		useJUnitPlatform()
	}
}
