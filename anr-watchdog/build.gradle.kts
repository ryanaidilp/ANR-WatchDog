plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("signing")
}

group = "com.github.anrwatchdog"
version = "2.2.0-SNAPSHOT"

android {
    namespace = "com.github.anrwatchdog"
    compileSdk = 34

    defaultConfig {
        minSdk = 16
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.7.1")

    // Unit testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = project.group.toString()
            artifactId = "anrwatchdog"
            version = project.version.toString()

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("ANR-Watchdog")
                description.set("A simple watchdog that detects Android ANR (Application Not Responding) error and throws a meaningful exception")
                url.set("https://github.com/ryanaidilp/ANR-WatchDog")

                licenses {
                    license {
                        name.set("MIT")
                        url.set("http://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("salomonbrys")
                        name.set("Salomon BRYS")
                        email.set("salomon.brys@gmail.com")
                    }
                    developer {
                        id.set("ryanaidilp")
                        name.set("Ryan Aidil Putra")
                    }
                }

                scm {
                    url.set("https://github.com/ryanaidilp/ANR-WatchDog")
                    connection.set("scm:git:https://github.com/ryanaidilp/ANR-WatchDog.git")
                    developerConnection.set("scm:git:git@github.com:ryanaidilp/ANR-WatchDog.git")
                    tag.set("HEAD")
                }

                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/ryanaidilp/ANR-WatchDog/issues")
                }
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

            credentials {
                username = findProperty("ossrhUsername")?.toString() ?: ""
                password = findProperty("ossrhPassword")?.toString() ?: ""
            }
        }
    }
}

// Only sign when signing properties are available (not on JitPack)
val hasSigningKey = findProperty("signing.keyId") != null

signing {
    setRequired { false }
    if (hasSigningKey) {
        sign(publishing.publications["release"])
    }
}

tasks.withType<Sign>().configureEach {
    onlyIf { hasSigningKey }
}
