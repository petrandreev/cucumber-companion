/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package org.gradle.cucumber.companion

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.testing.base.TestingExtension

class CucumberCompanionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            CucumberCompanionExtension::class.java,
            CucumberCompanionExtension.NAME,
            CucumberCompanionExtension::class.java
        )

        project.afterEvaluate {
            if (extension.enableForStandardTestTask.get()) {
                val name = "test"
                val testSuite =
                    project.extensions.findByType(TestingExtension::class.java)?.suites?.withType(JvmTestSuite::class.java)
                        ?.findByName(name)
                if (testSuite != null) {
                    generateCucumberSuiteCompanion(testSuite, project)
                } else {
                    generateCucumberSuiteCompanion(
                        project.tasks,
                        project.layout.buildDirectory,
                        project.extensions.getByType(JavaPluginExtension::class.java).sourceSets.named(name).get(),
                        name
                    )
                }
            }
        }
    }
}
