/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package org.gradle.cucumber.companion

import org.gradle.cucumber.companion.fixtures.CompanionAssertions
import org.gradle.cucumber.companion.fixtures.CucumberFeature
import org.gradle.cucumber.companion.fixtures.CucumberFixture
import org.gradle.cucumber.companion.fixtures.ExpectedCompanionFile
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir
import spock.util.io.FileSystemFixture

import java.nio.file.Files
import java.nio.file.Path

class CucumberCompanionPluginFunctionalTest extends Specification {

    static final String CUCUMBER_VERSION = "7.14.0"
    static final String JUNIT_JUPITER_VERSION = "5.10.0"

    @TempDir
    FileSystemFixture workspace
    def buildFile
    def settingsFile


    @Delegate
    CucumberFixture cucumberFixture = new CucumberFixture()
    CompanionAssertions companionAssertions = new CompanionAssertions(this::companionFile)

    @Delegate
    TestContextRunner runner

    def setup() {
        runner = new TestContextRunner(workspace.currentPath)
    }

    def "companion task can be registered"(BuildScriptLanguage buildScriptLanguage) {
        given:
        setupPlugin(buildScriptLanguage)

        when:
        def result = run("tasks", "--all")

        then:
        result.output.contains("testGenerateCucumberSuiteCompanion")

        where:
        buildScriptLanguage << BuildScriptLanguage.values()
    }

    def "testGenerateCucumberSuiteCompanion generates valid companion files"(BuildScriptLanguage buildScriptLanguage, Variant variant) {
        given:
        setupPlugin(buildScriptLanguage, variant)
        createFeatureFiles(workspace)

        when:
        def result = run("testGenerateCucumberSuiteCompanion")

        then:
        result.output.contains("testGenerateCucumberSuiteCompanion")

        def expectedCompanions = expectedCompanionFiles()

        expectedCompanions.forEach {
            companionAssertions.assertCompanionFile(it)
        }

        where:
        [buildScriptLanguage, variant] << [BuildScriptLanguage.values(), Variant.values()].combinations()
    }

    def "generated companion files are picked up by Gradle's test task and tests succeed"() {
        given:
        def succeedingFeatures = CucumberFeature.allSucceeding()
        setupPlugin(buildScriptLanguage, variant)
        createFeatureFiles(workspace, succeedingFeatures)
        createStepFiles(workspace, succeedingFeatures)

        when:
        def result = run("test")

        then:
        succeedingFeatures
            .collect { it.toExpectedTestTaskOutput("PASSED") }
            .every {
                result.output.contains(it)
            }

        and:
        result.task(":test").outcome == TaskOutcome.SUCCESS

        where:
        [buildScriptLanguage, variant] << [BuildScriptLanguage.values(), Variant.values()].combinations()
    }

    def "can run failing cucumber test"() {
        def failingFeatures = [CucumberFeature.FailingFeature]
        setupPlugin(buildScriptLanguage, variant)
        createFeatureFiles(workspace, failingFeatures)
        createStepFiles(workspace, failingFeatures)

        when:
        def result = runAndFail("test")

        then:
        failingFeatures
            .collect { it.toExpectedTestTaskOutput("FAILED") }
            .every {
                result.output.contains(it)
            }

        and:
        result.task(":test").outcome == TaskOutcome.FAILED

        where:
        [buildScriptLanguage, variant] << [BuildScriptLanguage.values(), Variant.values()].combinations()
    }

    def "testGenerateCucumberSuiteCompanion is incremental"(BuildScriptLanguage buildScriptLanguage) {
        given: "starting with a single feature"
        setupPlugin(buildScriptLanguage)
        createFeatureFiles(workspace, [CucumberFeature.ProductSearch])

        when: "running the generate task"
        def result = run("testGenerateCucumberSuiteCompanion")

        then: "feature companion is present"
        result.output.contains("testGenerateCucumberSuiteCompanion")

        expectedCompanionFiles('', [CucumberFeature.ProductSearch]).forEach {
            companionAssertions.assertCompanionFile(it)
        }

        when: "adding another feature"
        createFeatureFiles(workspace, [CucumberFeature.PasswordReset])

        and: "running the generate task again"
        result = run("testGenerateCucumberSuiteCompanion")

        then: "both companion files are present"
        result.output.contains("testGenerateCucumberSuiteCompanion")

        expectedCompanionFiles('', [CucumberFeature.ProductSearch, CucumberFeature.PasswordReset]).forEach {
            companionAssertions.assertCompanionFile(it)
        }

        when: "deleting a feature file"
        Files.delete(workspace.file("src/test/resources/${CucumberFeature.ProductSearch.featureFilePath}"))

        and: "running the generate task again"
        result = run("testGenerateCucumberSuiteCompanion")

        then: "one companion remains"
        result.output.contains("testGenerateCucumberSuiteCompanion")

        expectedCompanionFiles('', [CucumberFeature.PasswordReset]).forEach {
            companionAssertions.assertCompanionFile(it)
        }

        and: "the other is gone"
        expectedCompanionFiles('', [CucumberFeature.ProductSearch]).forEach {
            with(companionFile(it)) {
                !Files.exists(it)
            }
        }

        where:
        buildScriptLanguage << BuildScriptLanguage.values()
    }

    Path companionFile(ExpectedCompanionFile companion) {
        return workspace.resolve("build/generated-sources/cucumberCompanion-test/${companion.relativePath}")
    }

    void setupPlugin(BuildScriptLanguage language, Variant variant = Variant.Implicit_with_TestSuites) {
        switch (language) {
            case BuildScriptLanguage.Groovy:
                switch (variant) {
                    case Variant.Implicit:
                        setupPluginGroovy(false)
                        break
                    case Variant.Implicit_with_TestSuites:
                        setupPluginGroovy(true)
                        break
                    case Variant.Explicit:
                        setupPluginExplicitGroovy()
                        break
                }
                break
            case BuildScriptLanguage.Kotlin:
                switch (variant) {
                    case Variant.Implicit:
                        setupPluginKotlin(false)
                        break
                    case Variant.Implicit_with_TestSuites:
                        setupPluginKotlin(true)
                        break
                    case Variant.Explicit:
                        setupPluginExplicitKotlin()
                        break
                }
                break
            default:
                throw new IllegalArgumentException("Unsupported language: $language")
        }
    }

    enum BuildScriptLanguage {
        Groovy, Kotlin
    }

    enum Variant {
        Implicit,
        Implicit_with_TestSuites,
        Explicit;

        @Override
        String toString() {
            return name().replaceAll("_", " ")
        }
    }

    private void setupPluginGroovy(boolean withJvmTestSuite = true) {
        buildFile = workspace.file("build.gradle")
        settingsFile = workspace.file("settings.gradle")
        settingsFile.text = ""
        buildFile.text = """\
            plugins {
                id('java')
                ${withJvmTestSuite ? "id('jvm-test-suite')" : ""}
                id('org.gradle.cucumber.companion')
            }
            repositories {
                mavenCentral()
            }
            dependencies {
            ${dependenciesRequiredForExecution()}
            }
            tasks.withType(Test) {
                useJUnitPlatform()
                testLogging {
                    events("standardOut", "passed", "failed")
                }
            }
            """.stripIndent(true)
    }

    private void setupPluginKotlin(boolean withJvmTestSuite = true) {
        buildFile = workspace.file("build.gradle.kts")
        settingsFile = workspace.file("settings.gradle.kts")
        settingsFile.text = ""
        buildFile.text = """\
            plugins {
                java
                ${withJvmTestSuite ? 'id("jvm-test-suite")' : ""}
                id("org.gradle.cucumber.companion")
            }
            repositories {
                mavenCentral()
            }
            dependencies {
            ${dependenciesRequiredForExecution()}
            }
            tasks.withType<Test>().configureEach {
                useJUnitPlatform()
                testLogging {
                    events("standardOut", "passed", "failed")
                }
            }
            """.stripIndent(true)
    }

    private void setupPluginExplicitGroovy() {
        buildFile = workspace.file("build.gradle")
        settingsFile = workspace.file("settings.gradle")
        settingsFile.text = ""
        buildFile.text = """\
            plugins {
                id('java')
                id('jvm-test-suite')
                id('org.gradle.cucumber.companion')
            }
            repositories {
                mavenCentral()
            }

            cucumberCompanion {
                enableForStandardTestTask = false
            }
            dependencies {
            ${dependenciesRequiredForExecution()}
            }
            testing {
                suites {
                    test {
                        cucumberCompanion.generateCucumberSuiteCompanion(delegate)
                        targets {
                            all {
                                testTask.configure {
                                    useJUnitPlatform()
                                    testLogging {
                                        events("standardOut", "passed", "failed")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            """.stripIndent(true)
    }

    private void setupPluginExplicitKotlin() {
        buildFile = workspace.file("build.gradle.kts")
        settingsFile = workspace.file("settings.gradle.kts")
        settingsFile.text = ""
        buildFile.text = """\
            plugins {
                java
                `jvm-test-suite`
                id("org.gradle.cucumber.companion")
            }

            repositories {
                mavenCentral()
            }

            cucumberCompanion {
                enableForStandardTestTask.set(false)
            }
            dependencies {
            ${dependenciesRequiredForExecution()}
            }
            testing {
                suites {
                    val test by getting(JvmTestSuite::class) {
                        generateCucumberSuiteCompanion(project)
                        targets {
                            all {
                                testTask.configure {
                                    useJUnitPlatform()
                                    testLogging {
                                        events("standardOut", "passed", "failed")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            """.stripIndent(true)
    }

    def dependenciesRequiredForExecution() {
        return """\
        testImplementation(platform("org.junit:junit-bom:$JUNIT_JUPITER_VERSION"))
        testImplementation("io.cucumber:cucumber-java:$CUCUMBER_VERSION")
        testImplementation("io.cucumber:cucumber-junit-platform-engine:$CUCUMBER_VERSION")
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation("org.junit.platform:junit-platform-suite")
        """.stripIndent(true)
    }
}
