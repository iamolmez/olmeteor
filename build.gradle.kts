plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.7.7"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "com.olmeteors"
version = "1.3.0"
description = "OlMeteor event plugin for Paper/Folia 1.21.1+"

val pluginName = rootProject.name
val pluginVersion = version.toString()
val pluginDescription = description
val mockitoAgent by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.helpch.at/releases/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")

    // Server-provided plugin APIs. compileOnly keeps every class out of our JAR;
    // do not add these dependencies to implementation or a shading task.
    // WorldEdit / FAWE
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.11.0") { isTransitive = false }
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.11.0") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.8") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.8") { isTransitive = false }

    // WorldGuard
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.13") { isTransitive = false }
    compileOnly("com.sk89q.worldguard:worldguard-core:7.0.13") { isTransitive = false }

    // Towny — local JAR from GitHub Releases 0.103.0.7
    // (repo.palmergames.com SSL expired, JitPack produces empty JARs)
    compileOnly(files("libs/Towny.jar"))

    // MythicMobs — local JAR (Mythic-Dist shipped with unrelocated JetBrains annotations; we stripped them)
    compileOnly(files("libs/MythicMobs-5.7.2.jar"))

    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.6") { isTransitive = false }

    // DecentHolograms
    compileOnly("com.github.decentsoftware-eu:decentholograms:2.8.9") { isTransitive = false }

    // CommandAPI
    compileOnly("dev.jorel:commandapi-bukkit-core:11.2.0")

    // Test Dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    mockitoAgent("org.mockito:mockito-core:5.14.2")
}

tasks {
    test {
        useJUnitPlatform()
        jvmArgs("-javaagent:${mockitoAgent.asPath}")
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        filesMatching("plugin.yml") {
            expand(
                "name" to pluginName,
                "version" to pluginVersion,
                "description" to pluginDescription
            )
        }
    }

    assemble {
        dependsOn(reobfJar)
    }
}
