import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:2.0.0")
    }
}
plugins {
    kotlin("jvm") version "2.1.20"
    signing
    id("maven-publish")
    id("org.jetbrains.dokka") version "2.0.0"
    id("com.vanniktech.maven.publish") version "0.31.0"
}

group = "io.github.yangentao"

version = "1.1.12"
val artifactName = "hare"
val githubLib = "hare"
val descLib = "Java/Kotlin Annonations."


repositories {
    mavenLocal()
    mavenCentral()
//    maven("https://app800.cn/maven/repository/public/")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib"))
    testImplementation("org.xerial:sqlite-jdbc:[3.45.3.0,)")


    api("io.github.yangentao:xlog:[1.1.3,)")
    api("io.github.yangentao:anno:[1.1.0,)")
    api("io.github.yangentao:kson:[1.1.8,)")
    api("io.github.yangentao:types:[1.1.12,)")
    api("io.github.yangentao:sql:[1.1.12,)")

    api("io.github.yangentao:charcode:[1.0.3,)")
    api("io.github.yangentao:config:[1.1.5,)")
    api("io.github.yangentao:tag:[1.1.0,)")
    api("io.github.yangentao:httpbasic:[1.0.4,)")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates(project.group.toString(), artifactName, project.version.toString())
    pom {
        configPom(this, artifactName, descLib, githubLib)
    }
}
afterEvaluate {
    publishing {
        repositories {
            mavenLocal()
            maven {
                name = "App800"
                url = uri("https://app800.cn/maven/repository/public/")
                credentials {
                    username = providers.gradleProperty("ARCHIVA_USERNAME").get()
                    password = providers.gradleProperty("ARCHIVA_PASSWORD").get()
                }
            }
        }
    }
}



fun configPom(pom: MavenPom, artifactName: String, descLib: String, githubLib: String = artifactName) {
    pom.apply {
        name.set(artifactName)
        description.set(descLib)
        inceptionYear.set("2025")
        url.set("https://github.com/yangentao/$githubLib/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("yangentao")
                name.set("Yang Entao")
                url.set("https://github.com/yangentao/")
            }
        }
        scm {
            url.set("https://github.com/yangentao/$githubLib/")
            connection.set("scm:git:git://github.com/yangentao/$githubLib.git")
            developerConnection.set("scm:git:ssh://git@github.com/yangentao/$githubLib.git")
        }
    }
}