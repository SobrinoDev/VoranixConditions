plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.voranix"
version = "1.0.0-spigot1.20.1"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.aikar.co/content/groups/aikar/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    // Última versión estable de BetterModel compatible con 1.20.1
    compileOnly("io.github.toxicity188:BetterModel:1.4.2") { isTransitive = false }

    // Van dentro del jar (relocalizadas para no chocar con otros plugins)
    implementation("co.aikar:acf-bukkit:0.5.1-SNAPSHOT")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.17.0")
}

// El plugin queda para Java 17 (lo que usa Spigot 1.20.1). Se compila con un JDK más nuevo porque
// BetterModel viene compilado para Java 21 y javac 17 no puede leer sus clases.
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    disableAutoTargetJvm()
}

tasks.withType<JavaCompile> {
    options.release.set(17)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-parameters")) // ACF necesita -parameters
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    archiveFileName.set("VoranixConditions-${project.version}.jar")
    relocate("co.aikar.commands", "com.voranix.conditions.libs.acf")
    relocate("co.aikar.locales", "com.voranix.conditions.libs.locales")
    relocate("net.kyori", "com.voranix.conditions.libs.kyori")
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
