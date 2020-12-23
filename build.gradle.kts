import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.3.5.RELEASE"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    kotlin("jvm")  version "1.4.10"
    kotlin("plugin.spring") version "1.4.10"
    kotlin("kapt") version "1.4.10"
    kotlin("plugin.allopen") version "1.4.10"
    kotlin("plugin.jpa") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"

}

group = "top.netapps.kvppeteer"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}
configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

val seleniumDriverVersion = "3.141.59"
val htmlunitDriverVersion = "2.41.0"
val httpclientVersion = "4.5.11"
val druidVersion = "1.1.23"
val jsoupVersion = "1.13.1"
val cdtversion = "2.1.0"
val javaWebSocketVersion="1.5.0"
val commonsCompressVersion="1.19"
dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    //    集成springboot
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    implementation("org.apache.commons:commons-compress:$commonsCompressVersion")
    implementation("org.apache.httpcomponents:httpclient:$httpclientVersion")

    implementation("org.jsoup:jsoup:${jsoupVersion}")
    implementation("com.github.kklisura.cdt:cdt-java-client:2.1.0")
    implementation("net.sourceforge.htmlunit:htmlunit:$htmlunitDriverVersion")
     implementation("com.zaxxer:nuprocess:2.0.0")
    implementation("org.htmlparser:htmlparser:2.1")
}
tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict","-Xinline-classes")
        jvmTarget = "1.8"
    }
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.suppressWarnings = true
}
kapt {
    useBuildCache = false
}
allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.Embeddable")
    annotation("javax.persistence.MappedSuperclass")
}
