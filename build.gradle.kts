plugins {
    kotlin("jvm") version "2.3.0"
    application
    jacoco
}

group = "sopra"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("tools.aqua:bgw-gui:0.11")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("MainKt")
    // Umlaute und Kartensymbole im Konsolen-Log korrekt ausgeben
    applicationDefaultJvmArgs = listOf("-Dstdout.encoding=UTF-8", "-Dfile.encoding=UTF-8")
}

jacoco {
    toolVersion = "0.8.14" // unterstützt Java 25
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport) // nach den Tests automatisch Coverage-Report erzeugen
}

tasks.jacocoTestReport {
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}
