plugins {
    `java-library`
    id("com.vanniktech.maven.publish") version "0.34.0" apply false
}

group = "com.ohalee.database"
version = "1.0.0"

allprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.vanniktech.maven.publish")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
        publishToMavenCentral()

        signAllPublications()

        coordinates(rootProject.group.toString(), project.name, rootProject.version.toString())

        pom {
            name.set(project.name)
            description.set("A flexible and lightweight Java library providing unified abstractions for database connections with built-in support for MariaDB and Redis.")
            url.set("https://github.com/ohAleee/DatabaseProvider")

            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }

            developers {
                developer {
                    id.set("ohalee")
                    name.set("ohAlee")
                    email.set("business@ohalee.com")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/ohAleee/DatabaseProvider.git")
                developerConnection.set("scm:git:ssh://github.com:ohAleee/DatabaseProvider.git")
                url.set("https://github.com/ohAleee/DatabaseProvider")
            }

            issueManagement {
                system.set("GitHub")
                url.set("https://github.com/ohAleee/DatabaseProvider/issues")
            }
        }
    }
}