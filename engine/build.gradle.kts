import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.BufferedReader
import java.io.InputStreamReader

group = "com.zenaton.engine"
version = "1.0-SNAPSHOT"

plugins {
    application
    kotlin("jvm") version "1.3.70"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    id("com.commercehub.gradle.plugin.avro") version "0.19.1"
}

application {
    mainClassName = "com.zenaton.MainKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.apache.pulsar:pulsar-client:2.5.+")
    implementation("org.apache.pulsar:pulsar-functions-api:2.5.+")
    implementation("org.apache.avro:avro:1.9.+")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.+")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.+")
    testImplementation("org.jeasy:easy-random-core:4.2.+")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.0.+")
    testImplementation("io.kotest:kotest-property-jvm:4.0.+")
    testImplementation("io.kotest:kotest-core-jvm:4.0.+")
    testImplementation("io.mockk:mockk:1.9.+")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

tasks {
    named<ShadowJar>("shadowJar") {
        mergeServiceFiles()
    }
}

tasks {
    build {
        dependsOn("ktlintFormat")
        dependsOn(shadowJar)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

avro {
    isCreateSetters.set(false)
    isCreateOptionalGetters.set(false)
    isGettersReturnOptional.set(false)
    fieldVisibility.set("PRIVATE")
    outputCharacterEncoding.set("UTF-8")
    stringType.set("String")
    templateDirectory.set(null as String?)
    isEnableDecimalLogicalType.set(true)
    dateTimeLogicalType.set("JSR310")
}

tasks.register("install") {
    group = "Zenaton"
    description = "Install Zenaton into Pulsar"
    dependsOn("assemble")
    doLast {
        createSchemaFiles()
        uploadSchemaToTopic(
            name = "AvroTaskEngineMessage",
            topic = "tasks"
        )
        uploadSchemaToTopic(
            name = "AvroTaskMetricMessage",
            topic = "tasks-metrics"
        )
        setZenatonFunction(
            className = "com.zenaton.taskmanager.pulsar.engine.TaskEngineFunction",
            topicIn = "tasks",
            action = "create"
        )
        setZenatonFunction(
            className = "com.zenaton.workflowengine.pulsar.topics.workflows.functions.WorkflowEngineFunction",
            topicIn = "workflows",
            action = "create"
        )
        setZenatonFunction(
            className = "com.zenaton.decisionmanager.pulsar.functions.DecisionEngineFunction",
            topicIn = "decisions",
            action = "create"
        )
        setZenatonFunction(
            className = "com.zenaton.workflowengine.pulsar.topics.delays.functions.DelayEngineFunction",
            topicIn = "delays",
            action = "create"
        )
    }
}

tasks.register("update") {
    group = "Zenaton"
    description = "Update Zenaton into Pulsar"
    dependsOn("assemble")
    doLast {
        createSchemaFiles()
        uploadSchemaToTopic(
            name = "AvroTaskEngineMessage",
            topic = "tasks"
        )
        uploadSchemaToTopic(
            name = "AvroTaskMetricMessage",
            topic = "tasks-metrics"
        )
        setZenatonFunction(
            className = "com.zenaton.taskmanager.pulsar.engine.TaskEngineFunction",
            topicIn = "tasks"
        )
        setZenatonFunction(
            className = "com.zenaton.workflowengine.pulsar.topics.workflows.functions.WorkflowEngineFunction",
            topicIn = "workflows"
        )
        setZenatonFunction(
            className = "com.zenaton.decisionmanager.pulsar.functions.DecisionEngineFunction",
            topicIn = "decisions"
        )
        setZenatonFunction(
            className = "com.zenaton.workflowengine.pulsar.topics.delays.functions.DelayEngineFunction",
            topicIn = "delays"
        )
    }
}

tasks.register("delete") {
    group = "Zenaton"
    description = "Delete Zenaton from Pulsar"
    doLast {
        deleteZenatonFunction("WorkflowEngineFunction")
        deleteZenatonFunction("DecisionEngineFunction")
        deleteZenatonFunction("TaskEngineFunction")
        deleteZenatonFunction("DelayEngineFunction")
        forceDeleteTopic("workflows")
        forceDeleteTopic("decisions")
        forceDeleteTopic("tasks")
        forceDeleteTopic("tasks-metrics")
        forceDeleteTopic("delays")
        forceDeleteTopic("logs")
    }
}

fun createSchemaFiles() {
    // create schema files
    println("Creating schemas files...")
    val cmd = arrayOf("java", "-cp", "build/libs/engine-1.0-SNAPSHOT-all.jar", "com.zenaton.commons.utils.avro.MainKt")
    exec(cmd)
}

fun uploadSchemaToTopic(
    name: String,
    topic: String,
    tenant: String = "public",
    namespace: String = "default"
) {
    println("Uploading $name schema to $topic topic...")
    val cmd = arrayOf("docker-compose", "exec", "-T", "pulsar", "bin/pulsar-admin",
        "schemas", "upload",
        "persistent://$tenant/$namespace/$topic",
        "--filename", "/zenaton/engine/schemas/$name.schema"
    )
    exec(cmd)
}

fun setZenatonFunction(
    className: String,
    topicIn: String,
    topicOut: String? = null,
    action: String = "update",
    tenant: String = "public",
    namespace: String = "default"
) {
    println("Doing $action $className in $topicIn...")
    val cmd = mutableListOf("docker-compose", "exec", "-T", "pulsar", "bin/pulsar-admin",
        "functions", action,
        "--jar", "/zenaton/engine/libs/engine-1.0-SNAPSHOT-all.jar",
        "--classname", className,
        "--inputs", "persistent://$tenant/$namespace/$topicIn"
    )
    if (topicOut != null) {
        cmd.add("--output")
        cmd.add("persistent://$tenant/$namespace/$topicOut")
    }
    exec(cmd.toTypedArray())
}

fun deleteZenatonFunction(name: String, tenant: String = "public", namespace: String = "default") {
    println("Deleting $name function from $tenant/$namespace...")
    val cmd = arrayOf("docker-compose", "exec", "-T", "pulsar", "bin/pulsar-admin",
        "functions", "delete",
        "--tenant", tenant,
        "--namespace", namespace,
        "--name", name
    )
    exec(cmd)
}

fun forceDeleteTopic(topic: String, tenant: String = "public", namespace: String = "default") {
    println("Deleting $topic topic from $tenant/$namespace...")
    val cmd = arrayOf("docker-compose", "exec", "-T", "pulsar", "bin/pulsar-admin",
        "topics", "delete",
        "persistent://$tenant/$namespace/$topic",
        "--deleteSchema",
        "--force"
    )
    exec(cmd)
}

fun exec(cmd: Array<String>) {
    val p = Runtime.getRuntime().exec(cmd)
    val output = BufferedReader(InputStreamReader(p.inputStream))
    val error = BufferedReader(InputStreamReader(p.errorStream))
    var line: String? = ""
    while (output.readLine().also { line = it } != null) println(line)
    while (error.readLine().also { line = it } != null) println(line)
    p.waitFor()
}
