// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.utils.SdkUtils
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.MalformedURLException
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
abstract class BaseSlackLintTest : LintDetectorTest() {
  private val rootPath = "resources/test/com/slack/lint/data/"
  private val stubsPath = "testStubs"

  /**
   * Lint periodically adds new "TestModes" to LintDetectorTest. These modes act as a sort of chaos
   * testing mechanism, adding different common variations of code (extra spaces, extra parens, etc)
   * to the test files to ensure that lints are robust against them. They also make it quite
   * difficult to test against and need extra work sometimes to properly support, so we expose this
   * property to allow tests to skip certain modes that need more work in subsequent PRs.
   */
  open val skipTestModes: Array<TestMode>? = null

  fun loadStub(stubName: String): TestFile {
    return copy(stubsPath + File.separatorChar + stubName, "src/main/java/$stubName")
  }

  abstract override fun getDetector(): Detector

  abstract override fun getIssues(): List<Issue>

  override fun lint(): TestLintTask {
    val sdkLocation = System.getProperty("android.sdk") ?: System.getenv("ANDROID_HOME")
    val lintTask = super.lint()
    sdkLocation?.let { lintTask.sdkHome(File(it)) }
    lintTask.allowCompilationErrors(false)

    skipTestModes?.let { testModesToSkip -> lintTask.skipTestModes(*testModesToSkip) }
    return lintTask
  }

  /**
   * The default finder for resources doesn't work with our file structure; this ensures it will.
   *
   * https://www.bignerdranch.com/blog/building-custom-lint-checks-in-android/
   */
  override fun getTestResource(relativePath: String, expectExists: Boolean): InputStream {
    val path = (rootPath + relativePath).replace('/', File.separatorChar)
    val file = File(getTestDataRootDir(), path)
    if (file.exists()) {
      try {
        return BufferedInputStream(FileInputStream(file))
      } catch (_: FileNotFoundException) {
        if (expectExists) {
          fail("Could not find file $relativePath")
        }
      }
    }
    return BufferedInputStream(ByteArrayInputStream("".toByteArray()))
  }

  private fun getTestDataRootDir(): File? {
    val source = javaClass.protectionDomain.codeSource
    if (source != null) {
      val location = source.location
      try {
        val classesDir = SdkUtils.urlToFile(location)
        return classesDir.parentFile!!.absoluteFile.parentFile!!.parentFile
      } catch (e: MalformedURLException) {
        fail(e.localizedMessage)
      }
    }
    return null
  }
}
