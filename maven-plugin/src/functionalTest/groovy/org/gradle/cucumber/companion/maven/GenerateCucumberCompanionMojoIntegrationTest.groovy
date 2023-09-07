package org.gradle.cucumber.companion.maven

import groovy.xml.XmlSlurper
import org.gradle.maven.functest.MavenDistribution

import java.nio.file.Files

class GenerateCucumberCompanionMojoIntegrationTest extends BaseCucumberCompanionMavenFuncTest {

    def "generate-cucumber-companion-files mojo generates valid companion file"() {
        given:
        createProject()
        cucumberFixture.createFeatureFiles(workspace.fileSystem)
        cucumberFixture.createStepFiles(workspace.fileSystem)

        when:
        def result = maven.execute(workspace, "test")

        then:
        noExceptionThrown()
        result.assertErrorFreeLog()
        result.log.each { println(it) }

        and:
        def expectedCompanions = cucumberFixture.expectedCompanionFiles("Test")

        expectedCompanions.forEach {
            companionAssertions.assertCompanionFile(it)
        }
    }

    def "generate-cucumber-companion-files mojo generates valid companion files that are picked up by surefire"() {
        given:
        createProject()
        cucumberFixture.createFeatureFiles(workspace.fileSystem)
        cucumberFixture.createStepFiles(workspace.fileSystem)

        when:
        def result = maven.execute(workspace, "test")

        then:
        noExceptionThrown()
        result.assertErrorFreeLog()
        result.log.each { println(it) }

        and:
        def expectedCompanions = cucumberFixture.expectedCompanionFiles("Test")
        expectedCompanions.forEach {
            verifyAll(testReport(it)) {
                Files.exists(it)
                def testsuite = new XmlSlurper().parse(it)
                testsuite.testcase.size() == 1
            }
        }
    }

}
