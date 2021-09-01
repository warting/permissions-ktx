/*
 * Copyright 2020 Marcel Pinto Biescas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.net.URL

plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.dokka") version "1.4.32"
    id("com.jfrog.bintray") version "1.8.5"
    `maven-publish`
    id("com.gladed.androidgitversion") version "0.4.14"
}


androidGitVersion {
    tagPattern = "^v[0-9]+.*"
}


android {
    compileSdk = 30

    defaultConfig {
        minSdk = 21
        targetSdk = 30

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        named("release") {
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
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xinline-classes", "-Xopt-in=kotlin.RequiresOptIn")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))

    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")

    api("androidx.startup:startup-runtime:1.0.0")
    api("androidx.activity:activity-ktx:1.4.0-alpha01")
    api("androidx.fragment:fragment-ktx:1.3.5")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:${KotlinCompilerVersion.VERSION}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:${KotlinCompilerVersion.VERSION}")
}

val libraryName = "permissions-ktx"
val libraryGroup = "se.warting"
val libraryVersion = androidGitVersion.name().replace("v", "")

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            displayName.set(libraryName)
            //includes.from("../README.md")
            sourceLink {
                localDirectory.set(file("src/main/java"))
                remoteUrl.set(
                    URL("https://github.com/marcelpinto/permissions-ktx/tree/main/lib-compose/src/main/java")
                )
                remoteLineSuffix.set("#L")
            }
        }
    }
}

val androidJavadocJar by tasks.register<Jar>("androidJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

val androidHtmlJar by tasks.register<Jar>("androidHtmlJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-doc")
}

//val androidSourcesJar by tasks.register<Jar>("androidSourcesJar") {
//    archiveClassifier.set("sources")
//    from(android.sourceSets.getByName("main").java.srcDirs())
//}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/warting/permissions-ktx")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("permLib") {

            artifactId = libraryName
            groupId = libraryGroup
            version = libraryVersion

            afterEvaluate { artifact(tasks.getByName("bundleReleaseAar")) }
            //artifact(tasks.getByName("androidJavadocJar"))
            //artifact(tasks.getByName("androidHtmlJar"))
            //artifact(tasks.getByName("androidSourcesJar"))

            pom {
                name.set(libraryName)
                description.set("Kotlin Lightweight Android permissions library that follows the best practices.")
                url.set("https://github.com/marcelpinto/permissions-ktx")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("marcelpinto")
                        name.set("Marcel Pinto")
                        email.set("marcel.pinto.biescas@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/marcelpinto/permissions-ktx.git")
                    developerConnection.set("scm:git:ssh://github.com/marcelpinto/permissions-ktx.git")
                    url.set("https://github.com/marcelpinto/permissions-ktx")
                }

                withXml {
                    fun groovy.util.Node.addDependency(dependency: Dependency, scope: String) {
                        appendNode("dependency").apply {
                            appendNode("groupId", dependency.group)
                            appendNode("artifactId", dependency.name)
                            appendNode("version", dependency.version)
                            appendNode("scope", scope)
                        }
                    }

                    asNode().appendNode("dependencies").let { dependencies ->
                        // List all "api" dependencies as "compile" dependencies
                        configurations.api.get().dependencies.forEach {
                            dependencies.addDependency(it, "compile")
                        }
                        // List all "implementation" dependencies as "runtime" dependencies
                        configurations.implementation.get().dependencies.forEach {
                            dependencies.addDependency(it, "runtime")
                        }
                    }
                }
            }
        }
    }
}