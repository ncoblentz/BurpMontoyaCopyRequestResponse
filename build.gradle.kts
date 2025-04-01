plugins {
    //id("java")
    //`maven-publish`
    kotlin("jvm") version "2.1.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.ben-manes.versions") version "0.51.0" //Gradle -> Help -> dependencyUpdates
}

group = "org.nickcoblentz.montoya.utilities"
version = "1.4.5"

repositories {
    mavenLocal()
    mavenCentral()
    maven(url="https://jitpack.io") {
        content {
            includeGroup("com.github.milchreis")
            includeGroup("com.github.ncoblentz")
        }
    }
}

dependencies {
    //testImplementation(platform("org.junit:junit-bom:5.9.1"))
    //testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("net.portswigger.burp.extensions:montoya-api:2025.3")
    implementation("com.github.ncoblentz:BurpMontoyaLibrary:0.1.13")
    implementation("com.github.milchreis:uibooster:1.21.1")
    //implementation(kotlin("stdlib-jdk8"))

}

tasks.test {
    useJUnitPlatform()
}


kotlin {
    jvmToolchain(21)
}