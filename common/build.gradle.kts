dependencies {
    compileOnly(libs.gson)
    compileOnly(libs.log4j2)
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.gson)
    testImplementation(libs.log4j2)
    testImplementation("org.apache.logging.log4j:log4j-core:2.24.1")
    testImplementation("org.mockito:mockito-core:5.13.0")
}
tasks.test { useJUnitPlatform() }
