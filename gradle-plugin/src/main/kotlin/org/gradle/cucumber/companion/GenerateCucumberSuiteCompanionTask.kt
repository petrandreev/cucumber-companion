package org.gradle.cucumber.companion

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.gradle.cucumber.companion.generator.CompanionGenerator
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.nio.file.Files


abstract class GenerateCucumberSuiteCompanionTask : DefaultTask() {

    @get:[SkipWhenEmpty InputDirectory IgnoreEmptyDirectories PathSensitive(PathSensitivity.RELATIVE)]
    abstract val cucumberFeatureSources: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generateSuiteCompanionClasses(inputChanges: InputChanges) {
        val outputDir = outputDirectory.get().asFile.toPath()
        val inputDir = cucumberFeatureSources.get().asFile.toPath()
        inputChanges.getFileChanges(cucumberFeatureSources).filter { it.file.name.toString().endsWith(".feature") }
            .forEach {change ->
                val companionFile = CompanionGenerator.resolve(inputDir, outputDir, change.file.toPath())
                when(change.changeType) {
                    ChangeType.ADDED -> CompanionGenerator.create(companionFile)
                    // This means that the source file was modified in some way,
                    // but since we currently only care about the file name, we can ignore it for now.
                    ChangeType.MODIFIED -> {}
                    ChangeType.REMOVED -> Files.deleteIfExists(companionFile.destination)
                    else -> {}
                }
        }
    }
}
