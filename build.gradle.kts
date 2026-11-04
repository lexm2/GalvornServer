// GalvornServer — standalone server-side Java that handles packets
// in the infected Forge JAR and returns responses to the client.
//
// This is separate from the agent module (Galvorn/agent/) which runs
// inside the Minecraft JVM for introspection. The server code lives
// in com.galvorn.server.* and compiles to its own galvorn-server.jar.

plugins {
    java
    `java-library`
    id("com.gradleup.shadow") version "8.3.5"
}

val serverJavaTarget: Int = providers.gradleProperty("server.javaTarget")
    .getOrElse("8").toInt()
val mcVersion: String = providers.gradleProperty("mc.version").get()

group = "com.galvorn"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(serverJavaTarget))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // MC 1.7.10 ships with Netty 4.x. Compile-only so the server
    // can type-check netty.Channel references without bundling
    // the full Netty JAR (it's already present in the Forge runtime).
    compileOnly("io.netty:netty-all:4.1.68.Final")

    // MC reference for compile-time checking (reflective at runtime).
    compileOnly(files("libs/minecraft-${mcVersion}.jar"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveBaseName.set("galvorn-server")
    archiveClassifier.set("")
    archiveVersion.set("")
    exclude("META-INF/maven/**", "META-INF/*.kotlin_module", "META-INF/versions/**")
    
    manifest {
        attributes(
            "Premain-Class" to "com.galvorn.server.GalvornStartupHook",
            "Implementation-Title" to "galvorn-server",
            "Implementation-Version" to project.version.toString(),
        )
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
