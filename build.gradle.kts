plugins {
    id("java")
    // Using the 'build-health' convention ID ensures the analysis engine binds 
    // cleanly to the root project compilation tasks
    //id("com.autonomousapps.build-health") version "2.14.0" 
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencyAnalysis {
    reporting {
        printBuildHealth(true)
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-core:2.19.2") 
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.1") 
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
}

tasks.register<Copy>("createStudentDistribution") {
    group = "education"
    description = "Generates the student template repository by stripping solution code and polishing configuration files."

    // 1. Copy everything from your root folder except IDE, build, and local distribution artifacts
    from(projectDir) {
        exclude(".gradle", ".idea", "build", "out", "dist", ".git", "local.properties")
    }

    // 2. Output to a local distribution directory inside your project
    into(layout.projectDirectory.dir("dist/student-assignment"))

    // 3. Strip solution blocks dynamically from main Java source files
    eachFile {
        if (this.path.contains("src/main/java")) {
            // A closure-safe mutable flag to track if we are inside a solution block
            val skipping = booleanArrayOf(false)

            this.filter { line ->
                when {
                    line.contains("// //STUB_START") -> {
                        skipping[0] = true
                        null // Remove this marker line
                    }
                    line.contains("// //STUB_END") -> {
                        skipping[0] = false
                        null // Remove this marker line
                    }
                    skipping[0] -> {
                        null // Skip and permanently remove lines inside the solution block
                    }
                    else -> {
                        line // Keep student template code and // //TODOs
                    }
                }
            }
        }
    }

    // 4. Automatically polish the project name inside the student settings file
    filesMatching("settings.gradle.kts") {
        filter { line ->
            if (line.contains("rootProject.name")) {
                // If your folder/project is named "assignment-1-solution", 
                // this cleanly rewrites it to just "assignment-1" for the students.
                line.replace("-Solution", "").replace("-master", "")
            } else {
                line
            }
        }
    }
}
