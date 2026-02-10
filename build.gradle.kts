plugins {
	`java-library`
	id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.belikhun.lunamanhunt"
version = "0.1.0"

tasks.withType<JavaCompile>().configureEach {
	options.release.set(21)
}

repositories {
	mavenCentral()
	maven("https://repo.papermc.io/repository/maven-public/")
	maven("https://repo.onarandombox.com/content/groups/public/")
}

dependencies {
	compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
	compileOnly("org.mvplugins.multiverse.core:multiverse-core:5.5.2")
	compileOnly("org.mvplugins.multiverse.netherportals:multiverse-netherportals:5.0.4")
}

tasks.processResources {
	filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
		expand("version" to project.version)
	}
}

tasks.shadowJar {
	archiveClassifier.set("shadow")
	dependsOn(tasks.build)
}
