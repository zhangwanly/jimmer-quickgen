import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

plugins {
    `java-library`
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.babyfish.jimmer:jimmer-sql:0.10.10") {
        exclude("*")
    }
    implementation("org.babyfish.jimmer:jimmer-core:0.10.10") {
        exclude("*")
    }
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
    implementation("com.squareup:javapoet:1.13.0")
    implementation("org.jspecify:jspecify:1.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("com.h2database:h2:2.2.224")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("com.mysql:mysql-connector-j:9.7.0")
    testImplementation("org.postgresql:postgresql:42.7.3")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "jimmer-quickgen"

            pom {
                name = "Jimmer QuickGen"
                description = "A Java code generation tool that reverse-engineers database schemas into Jimmer entity interfaces"
                url = "https://github.com/zhangwanly/jimmer-quickgen"
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
                developers {
                    developer {
                        id = "zhangwanly"
                        name = "zhangwanly"
                        email = "zhangwanly@gmail.com"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/zhangwanly/jimmer-quickgen.git"
                    developerConnection = "scm:git:ssh://github.com:zhangwanly/jimmer-quickgen.git"
                    url = "https://github.com/zhangwanly/jimmer-quickgen"
                }
            }
        }
    }
    repositories {
        maven {
            name = "sonatype"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("sonatypeUsername") as String?
                password = project.findProperty("sonatypePassword") as String?
            }
        }
    }
}

tasks.register("finalizePortalDeployment") {
    description = "Notify Central Portal to process the uploaded deployment"
    group = "publishing"

    val username = project.findProperty("sonatypeUsername") as String?
    val password = project.findProperty("sonatypePassword") as String?

    doLast {
        val ns = "io.github.zhangwanly"
        val user = username ?: error("sonatypeUsername not set")
        val pass = password ?: error("sonatypePassword not set")

        val url = "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/$ns"
        val auth = Base64.getEncoder().encodeToString("$user:$pass".toByteArray())

        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Basic $auth")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 30_000
        conn.readTimeout = 30_000

        val code = conn.responseCode
        val body = conn.inputStream.bufferedReader().readText()
        println("Portal finalize response: $code")
        if (body.isNotBlank()) println(body)
        if (code !in 200..299) {
            error("Failed to finalize deployment: HTTP $code")
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["maven"])
}
