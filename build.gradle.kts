plugins {
    kotlin("jvm") version "2.2.0"
}

group = "com.jvanev"
version = "0.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
