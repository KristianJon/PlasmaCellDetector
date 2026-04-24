plugins {
    // Support writing the extension in Groovy (remove this if you don't want to)
    groovy
    // To optionally create a shadow/fat jar that bundle up any non-core dependencies
    id("com.gradleup.shadow") version "8.3.5"
    // QuPath Gradle extension convention plugin
    id("qupath-conventions")
}

// TODO: Configure your extension here (please change the defaults!)
qupathExtension {
    name = "qupath-extension-plasmaCellDetector"
    group = "io.github.KristianJon"
    version = "0.1.0"
    description = "An extension for loading custom YOLO-models (v10, v11, v12) and visualizing predictions"
    automaticModule = "io.github.KristianJon.qupath.extension.template"
}

// TODO: Define your dependencies here
dependencies {

    // https://mvnrepository.com/artifact/ai.djl/api
    implementation("ai.djl:api:0.34.0")

    // https://mvnrepository.com/artifact/ai.djl.pytorch/pytorch-engine
    implementation("ai.djl.pytorch:pytorch-engine:0.34.0")

    // https://mvnrepository.com/artifact/ai.djl.pytorch/pytorch-native-cpu
    implementation("ai.djl.pytorch:pytorch-native-cpu:2.7.1:win-x86_64")

    // Main dependencies for most QuPath extensions
    shadow(libs.bundles.qupath)
    shadow(libs.bundles.logging)
    shadow(libs.qupath.fxtras)

    // For testing
    testImplementation(libs.bundles.qupath)
    testImplementation(libs.junit)
    // Source: https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.0-M1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
