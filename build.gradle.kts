plugins {
    java
}

group = "com.voranix"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.218-stable")
    compileOnly("io.github.toxicity188:bettermodel-bukkit-api:3.5.1")
}

// Paper 26.x necesita Java 25 (Gradle lo busca solo aunque JAVA_HOME apunte a otro JDK)
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveFileName.set("VoranixConditions-${project.version}.jar")
}
