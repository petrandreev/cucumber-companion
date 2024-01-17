/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gradle.cucumber.companion

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskContainer

fun generateCucumberSuiteCompanion(suite: JvmTestSuite, project: Project) {
    val taskContainer = project.tasks
    val buildDirectory = project.layout.buildDirectory
    generateCucumberSuiteCompanion(suite, taskContainer, buildDirectory)
}

fun generateCucumberSuiteCompanion(
    suite: JvmTestSuite,
    taskContainer: TaskContainer,
    buildDirectory: DirectoryProperty
) {
    val sourceSet = suite.sources
    generateCucumberSuiteCompanion(taskContainer, buildDirectory, sourceSet, suite.name)
}

fun generateCucumberSuiteCompanion(
    taskContainer: TaskContainer,
    buildDirectory: DirectoryProperty,
    sourceSet: SourceSet,
    name: String
) {
    val companionTask = taskContainer.register(
        "${name}GenerateCucumberSuiteCompanion",
        GenerateCucumberSuiteCompanionTask::class.java
    )
    val outputDir = buildDirectory.dir("generated-sources/cucumberCompanion-$name")

    companionTask.configure {
        // this is a bit icky, ideally we'd use a SourceDirectorySet ourselves, but I'm not sure that is proper
        this.cucumberFeatureSources.set(sourceSet.resources.srcDirs.first())
        this.outputDirectory.set(outputDir)
    }
    sourceSet.java.srcDir(companionTask)
}
