plugins {
	java
	id("org.springframework.boot") version "4.0.2"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "be.kdg.tracktogether"
version = "0.0.1-SNAPSHOT"
description = "TrackTogether Spring Boot application"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
	testCompileOnly {
		extendsFrom(compileOnly.get())
	}
	testAnnotationProcessor {
		extendsFrom(annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework:spring-websocket")
	implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
	implementation("com.fasterxml.jackson.core:jackson-databind")

	runtimeOnly("org.postgresql:postgresql")

	developmentOnly("org.springframework.boot:spring-boot-devtools")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("com.h2database:h2")

	implementation("org.webjars:bootstrap:5.3.3")
	implementation("org.webjars.npm:bootstrap-icons:1.13.1")
	implementation("org.webjars.npm:leaflet:1.9.4")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
	val envFile = rootProject.file(".env")
	if (envFile.exists()) {
		envFile.readLines()
			.filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
			.forEach { line ->
				val (key, value) = line.split("=", limit = 2)
				environment(key.trim(), value.trim())
			}
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.named<JavaCompile>("compileTestJava") {
	// Required with the current project layout so tests can resolve main package types.
	source(sourceSets.main.get().allJava)
}