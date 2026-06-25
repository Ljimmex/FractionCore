plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")

    implementation("org.spongepowered:configurate-yaml:4.1.2")
    implementation("com.zaxxer:HikariCP:6.2.1")
    // Database drivers are compileOnly: the server administrator must place the
    // appropriate driver on the server's classpath. This avoids driver version
    // conflicts and keeps the plugin JAR small.
    compileOnly("com.mysql:mysql-connector-j:9.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.47.1.0")
    compileOnly("org.postgresql:postgresql:42.7.3")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.mockito:mockito-core:5.14.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    runServer {
        minecraftVersion("1.20.6")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    test {
        useJUnitPlatform()
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    jar {
        from({
            configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
        })
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
