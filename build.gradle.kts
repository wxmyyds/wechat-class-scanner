plugins {
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.wechat.scanner"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("org.jf.dexlib2:dexlib2:2.5.2")
    implementation("info.picocli:picocli:4.7.6")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

tasks.shadowJar {
    manifest.attributes["Main-Class"] = "com.wechat.scanner.Main"
    relocate("org.jf.dexlib2", "com.wechat.scanner.dexlib2")
    relocate("picocli", "com.wechat.scanner.picocli")
    archiveClassifier.set("all")
}

tasks.test {
    useJUnitPlatform()
}