package com.github.joselion.prettyjupiter

import static com.github.joselion.prettyjupiter.helpers.Utils.ESC
import static org.gradle.api.tasks.testing.TestResult.ResultType.SUCCESS
import static org.gradle.api.tasks.testing.TestResult.ResultType.FAILURE
import static org.gradle.api.tasks.testing.TestResult.ResultType.SKIPPED

import java.io.File
import spock.lang.Specification
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.reporting.DirectoryReport
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.api.tasks.testing.TestTaskReports
import org.gradle.testfixtures.ProjectBuilder

import com.github.joselion.prettyjupiter.helpers.Colors
import com.github.joselion.prettyjupiter.helpers.Utils

class PrettyLoggerTest extends Specification {

  def '.logDescriptors'(String className, Integer times) {
    given:
      final Logger logger = Mock()
      final Project project = Stub(Project) { getLogger() >> logger }
      final Test testTask = Stub(Test)
      final PrettyLogger prettyLogger = new PrettyLogger(project, testTask)
      final TestDescriptor descriptor = Stub(TestDescriptor) {
        getParent() >> null
        getClassName() >> className
        getDisplayName() >> 'This is a test description!'
      }

    when:
      prettyLogger.logDescriptors(descriptor)

    then:
      with(logger) {
        times * lifecycle("${ESC}[97mThis is a test description!${ESC}[0m")
      }

    where:
      className | times
      null      | 0
      'any'     | 1
  }

  def '.logResults'(ResultType resultType, String log) {
    given:
      final Logger logger = Mock()
      final Project project = Stub(Project) { getLogger() >> logger }
      final Test testTask = Stub(Test)
      final PrettyLogger prettyLogger = new PrettyLogger(project, testTask)
      final TestDescriptor descriptor = Stub(TestDescriptor) {
        getParent() >> null
        getDisplayName() >> 'This is a test result!'
      }
      final TestResult results = Stub(TestResult) {
        getResultType() >> resultType
        getStartTime() >> 10000
        getEndTime() >> 10010
        getException() >> null
      }

    when:
      prettyLogger.logResults(descriptor, results)

    then:
      with(logger) {
        1 * lifecycle(log)
      }

    where:
      resultType  | log
      SUCCESS     | "✔ ${ESC}[90mThis is a test result!${ESC}[0m (${ESC}[97m10ms${ESC}[0m)"
      FAILURE     | "❌ ${ESC}[31mThis is a test result!${ESC}[0m (${ESC}[97m10ms${ESC}[0m)"
      SKIPPED     | "⚠ ${ESC}[33mThis is a test result!${ESC}[0m (${ESC}[97m10ms${ESC}[0m)"
  }

  def '.logSummary'(ResultType resultType, String icon) {
    given:
      final Logger logger = Mock()
      final Project project = Stub(Project) { getLogger() >> logger }
      final Test testTask = Stub(Test) {
        getReports() >> Stub(TestTaskReports) {
          getHtml() >> Stub(DirectoryReport) {
            getEntryPoint() >> Stub(File) {
              toString() >> 'path/to/report/file.html'
            }
          }
        }
      }
      final PrettyLogger prettyLogger = new PrettyLogger(project, testTask)
      final TestDescriptor descriptor = Stub(TestDescriptor) { getParent() >> null }
      final Exception exception = new Exception("Multi\nline\nexception!")
      final TestResult testRes = Stub(TestResult) {
        getResultType() >> FAILURE
        getException() >> exception
      }
      final TestResult results = Stub(TestResult) {
        getResultType() >> resultType
        getStartTime() >> 1583909261673
        getEndTime() >> 1583909305290
        getTestCount() >> 136
        getSuccessfulTestCount() >> 120
        getFailedTestCount() >> 10
        getSkippedTestCount() >> 6
      }

    when:
      prettyLogger.logResults(desc(1), testRes)
      prettyLogger.logSummary(descriptor, results)

    then:
      with(logger) {
        final String rawText = " ${icon} 136 tests completed, 120 successes, 10 failures, 6 skipped (43.617 seconds) "

        1 * lifecycle('\n\n')
        1 * lifecycle("${ESC}[91m(1)${ESC}[0m  Test 1:")
        1 * lifecycle("       ${ESC}[91mMulti")
        1 * lifecycle('       line')
        1 * lifecycle("       exception!${ESC}[0m")
        1 * lifecycle('')
        1 * lifecycle('     Failure stack trace:')
        1 * lifecycle("       ${ESC}[90m${exception.getStackTrace()[0]}")
        1 * lifecycle("       ${exception.getStackTrace()[1]}")
        1 * lifecycle("       ${exception.getStackTrace()[2]}")
        1 * lifecycle("       ${exception.getStackTrace()[3]}")
        1 * lifecycle("       ${exception.getStackTrace()[4]}")
        1 * lifecycle("       ${exception.getStackTrace()[5]}")
        1 * lifecycle("       ${exception.getStackTrace()[6]}")
        1 * lifecycle("       ${exception.getStackTrace()[7]}")
        1 * lifecycle("       ${exception.getStackTrace()[8]}")
        1 * lifecycle("       ${exception.getStackTrace()[9]}")
        1 * lifecycle("       --- and ${exception.getStackTrace().length - 10} more ---${ESC}[0m")
        2 * lifecycle('\n')
        1 * lifecycle('┌' + '─' * rawText.length() + '┐')
        1 * lifecycle("| ${icon} 136 tests completed, ${ESC}[32m120 successes${ESC}[0m, ${ESC}[31m10 failures${ESC}[0m, ${ESC}[33m6 skipped${ESC}[0m (43.617 seconds) |")
        1 * lifecycle('|' + ' ' * rawText.length() + '|')
        1 * lifecycle('| Report: path/to/report/file.html                                              |')
        1 * lifecycle('└' + '─' * rawText.length() + '┘')
      }

    where:
      resultType  | icon
      SUCCESS     | '✔'
      FAILURE     | '❌'
      SKIPPED     | '⚠'
  }

  def 'duration colors'(Long startTime, Long endTime, Colors color) {
    given:
      final Logger logger = Mock()
      final Project project = Stub(Project) { getLogger() >> logger }
      final Test testTask = Stub(Test)
      final PrettyJupiterPluginExtension extension = new PrettyJupiterPluginExtension()
      final PrettyLogger prettyLogger = new PrettyLogger(project, testTask, extension)
      final TestDescriptor descriptor = Stub(TestDescriptor) {
        getParent() >> null
        getDisplayName() >> 'Another test description comes here'
      }
      final TestResult results = Stub(TestResult) {
        getResultType() >> SUCCESS
        getStartTime() >> startTime
        getEndTime() >> endTime
      }

      when:
        prettyLogger.logResults(descriptor, results)

      then:
        with(logger) {
          final int colorCode = color.getCode()
          final long diff = endTime - startTime
          1 * lifecycle("✔ ${ESC}[90mAnother test description comes here${ESC}[0m (${ESC}[${colorCode}m${diff}ms${ESC}[0m)")
        }

      where:
        startTime | endTime | color
        0         | 100     | Colors.RED
        0         | 75      | Colors.RED
        0         | 50      | Colors.YELLOW
        0         | 38      | Colors.YELLOW
        0         | 25      | Colors.WHITE
        0         | 0       | Colors.WHITE
  }

  def 'disable duration'() {
    final Logger logger = Mock()
    final Project project = Stub(Project) { getLogger() >> logger }
    final Test testTask = Stub(Test)
    final PrettyJupiterPluginExtension extension = new PrettyJupiterPluginExtension()
    extension.duration.enabled = false
    final PrettyLogger prettyLogger = new PrettyLogger(project, testTask, extension)
    final TestDescriptor descriptor = Stub(TestDescriptor) {
      getParent() >> null
      getDisplayName() >> 'Some tests without duration'
    }
    final TestResult result = Stub(TestResult) {
      getResultType() >> SUCCESS
      getStartTime() >> 100
      getEndTime() >> 500
    }

    when:
      prettyLogger.logResults(descriptor, result)

    then:
      with(logger) {
        1 * lifecycle("✔ ${ESC}[90mSome tests without duration${ESC}[0m")
      }
  }

  private TestDescriptor desc(Integer parents = 0) {
    final Integer num = parents + 1

    return Stub(TestDescriptor) {
      getParent() >> Stub(TestDescriptor) {
        getParent() >> descriptorWithParents(num)
      }
    }
  }

  private TestDescriptor descriptorWithParents(Integer num) {
    if (num == null) {
      return null
    }

    if (num == 0) {
      return Stub(TestDescriptor) {
        getParent() >> null
        getDisplayName() >> "Test ${num - 1}"
      }
    }

    return Stub(TestDescriptor) {
      getParent() >> descriptorWithParents(num - 1)
      getDisplayName() >> "Test ${num - 1}"
    }
  }
}
