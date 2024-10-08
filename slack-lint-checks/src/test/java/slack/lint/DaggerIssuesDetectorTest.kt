// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

class DaggerIssuesDetectorTest : BaseSlackLintTest() {

  private companion object {
    private val javaxInjectStubs =
      kotlin(
          """
        package javax.inject

        annotation class Inject
        annotation class Qualifier
      """
        )
        .indented()

    private val daggerStubs =
      kotlin(
          """
        package dagger

        annotation class Binds
        annotation class Provides
        annotation class Module
      """
        )
        .indented()
  }

  override val skipTestModes: Array<TestMode> =
    arrayOf(
      TestMode.WHITESPACE,
      TestMode.SUPPRESSIBLE,
      // This mode adds new parameters to binds() functions, resulting in different error messages
      TestMode.JVM_OVERLOADS,
    )

  override fun getDetector() = DaggerIssuesDetector()

  override fun getIssues() = DaggerIssuesDetector.ISSUES.toList()

  @Test
  fun `bindings cannot be extension functions`() {
    lint()
      .files(
        javaxInjectStubs,
        daggerStubs,
        kotlin(
            """
                  package foo
                  import javax.inject.Qualifier
                  import dagger.Binds
                  import dagger.Provides
                  import dagger.Module

                  @Qualifier
                  annotation class MyQualifier

                  @Module
                  interface MyModule {
                    @Binds fun Int.bind(): Number
                    @Binds fun Long.bind(): Number
                    @Binds fun Double.bind(): Number
                    @Binds fun Float.bind(): Number
                    @Binds fun Short.bind(): Number
                    @Binds fun Byte.bind(): Number
                    @Binds fun Char.bind(): Comparable<Char>
                    @Binds fun String.bind(): Comparable<String>
                    @Binds fun @receiver:MyQualifier Boolean.bind(): Comparable<Boolean>
                  }

                  @Module
                  interface MyModule2 {
                    @Provides fun Int.bind(): Number = this@bind
                    @Provides fun Long.bind(): Number = this@bind
                    @Provides fun Double.bind(): Number = this@bind
                    @Provides fun Float.bind(): Number = this@bind
                    @Provides fun Short.bind(): Number = this@bind
                    @Provides fun Byte.bind(): Number = this@bind
                    @Provides fun Char.bind(): Comparable<Char> = this@bind
                    @Provides fun String.bind(): Comparable<String> = this@bind
                    @Provides fun @receiver:MyQualifier Boolean.bind(): Comparable<Boolean> = this@bind
                  }
                """
          )
          .indented(),
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/foo/MyQualifier.kt:12: Error: @Binds/@Provides functions cannot be extensions [BindingReceiverParameter]
          @Binds fun Int.bind(): Number
                     ~~~
        src/foo/MyQualifier.kt:13: Error: @Binds/@Provides functions cannot be extensions [BindingReceiverParameter]
          @Binds fun Long.bind(): Number
                     ~~~~
        src/foo/MyQualifier.kt:14: Error: @Binds/@Provides functions cannot be extensions [BindingReceiverParameter]
          @Binds fun Double.bind(): Number
                     ~~~~~~
        src/foo/MyQualifier.kt:15: Error: @Binds/@Provides functions cannot be extensions [BindingReceiverParameter]
          @Binds fun Float.bind(): Number
                     ~~~~~
        src/foo/MyQualifier.kt:16: Error: @Binds/@Provides functions cannot be extensions [BindingReceiverParameter]
          @Binds fun Short.bind(): Number
                     ~~~~~
        src/foo/MyQualifier.kt:17: Error: @Binds/@Provides functions cannot be extensions [BindingReceiverParameter]
          @Binds fun Byte.bind(): Number
                     ~~~~
        src/foo/MyQualifier.kt:18: Error: @Binds/@Provides functions cannot be extensions [BindingReceiverParameter]
          @Binds fun Char.bind(): Comparable<Char>
                     ~~~~
        src/foo/MyQualifier.kt:19: Error: @Binds/@Provides functions cannot be extensions [BindingReceiverParameter]
          @Binds fun String.bind(): Comparable<String>
                     ~~~~~~
        src/foo/MyQualifier.kt:20: Error: @Binds/@Provides functions cannot be extensions [BindingReceiverParameter]
          @Binds fun @receiver:MyQualifier Boolean.bind(): Comparable<Boolean>
                                           ~~~~~~~
        src/foo/MyQualifier.kt:25: Error: @Provides functions cannot be abstract [ProvidesMustNotBeAbstract]
          @Provides fun Int.bind(): Number = this@bind
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyQualifier.kt:26: Error: @Provides functions cannot be abstract [ProvidesMustNotBeAbstract]
          @Provides fun Long.bind(): Number = this@bind
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyQualifier.kt:27: Error: @Provides functions cannot be abstract [ProvidesMustNotBeAbstract]
          @Provides fun Double.bind(): Number = this@bind
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyQualifier.kt:28: Error: @Provides functions cannot be abstract [ProvidesMustNotBeAbstract]
          @Provides fun Float.bind(): Number = this@bind
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyQualifier.kt:29: Error: @Provides functions cannot be abstract [ProvidesMustNotBeAbstract]
          @Provides fun Short.bind(): Number = this@bind
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyQualifier.kt:30: Error: @Provides functions cannot be abstract [ProvidesMustNotBeAbstract]
          @Provides fun Byte.bind(): Number = this@bind
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyQualifier.kt:31: Error: @Provides functions cannot be abstract [ProvidesMustNotBeAbstract]
          @Provides fun Char.bind(): Comparable<Char> = this@bind
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyQualifier.kt:32: Error: @Provides functions cannot be abstract [ProvidesMustNotBeAbstract]
          @Provides fun String.bind(): Comparable<String> = this@bind
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyQualifier.kt:33: Error: @Provides functions cannot be abstract [ProvidesMustNotBeAbstract]
          @Provides fun @receiver:MyQualifier Boolean.bind(): Comparable<Boolean> = this@bind
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        18 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `binds type mismatches`() {
    lint()
      .files(
        javaxInjectStubs,
        daggerStubs,
        kotlin(
            "src/foo/TestModule.kt",
            """
                  package foo
                  import javax.inject.Qualifier
                  import dagger.Binds
                  import dagger.Module

                  sealed interface ItemDetail {
                    object DetailTypeA : ItemDetail
                  }

                  interface ItemMapper<T : ItemDetail>

                  class DetailTypeAItemMapper : ItemMapper<ItemDetail.DetailTypeA>

                  @Module
                  interface MyModule {
                    @Binds fun validBind(real: Int): Number
                    @Binds fun validBind(real: Boolean): Comparable<Boolean>
                    @Binds fun invalidBind(real: Long): String
                    @Binds fun invalidBind(real: Long): Comparable<Boolean>

                    @Binds fun validComplexBinding(real: DetailTypeAItemMapper): ItemMapper<out ItemDetail>
                    @Binds fun validComplexBinding2(real: DetailTypeAItemMapper): ItemMapper<*>
                    @Binds fun invalidComplexBinding(real: DetailTypeAItemMapper): ItemMapper<ItemDetail>
                  }
                """,
          )
          .indented(),
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/foo/TestModule.kt:18: Error: @Binds parameter/return must be type-assignable [BindsTypeMismatch]
          @Binds fun invalidBind(real: Long): String
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/TestModule.kt:19: Error: @Binds parameter/return must be type-assignable [BindsTypeMismatch]
          @Binds fun invalidBind(real: Long): Comparable<Boolean>
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/TestModule.kt:23: Error: @Binds parameter/return must be type-assignable [BindsTypeMismatch]
          @Binds fun invalidComplexBinding(real: DetailTypeAItemMapper): ItemMapper<ItemDetail>
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        3 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `redundant types`() {
    lint()
      .files(
        javaxInjectStubs,
        daggerStubs,
        kotlin(
            """
                  package foo
                  import javax.inject.Qualifier
                  import dagger.Binds
                  import dagger.Module

                  @Qualifier
                  annotation class MyQualifier

                  @Module
                  interface MyModule {
                    @MyQualifier @Binds fun validBind(real: Boolean): Boolean
                    @Binds fun validBind(@MyQualifier real: Boolean): Boolean
                    @Binds fun invalidBind(real: Long): Long
                    @Binds fun invalidBind(real: Long): Long
                  }
                """
          )
          .indented(),
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/foo/MyQualifier.kt:13: Error: @Binds functions should return a different type [RedundantBinds]
          @Binds fun invalidBind(real: Long): Long
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyQualifier.kt:14: Error: @Binds functions should return a different type [RedundantBinds]
          @Binds fun invalidBind(real: Long): Long
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        2 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `invalid return types`() {
    lint()
      .files(
        javaxInjectStubs,
        daggerStubs,
        kotlin(
            """
                  package foo
                  import javax.inject.Qualifier
                  import dagger.Binds
                  import dagger.Provides
                  import dagger.Module

                  @Qualifier
                  annotation class MyQualifier

                  @Module
                  abstract class MyModule {
                    @Binds fun invalidBind1(@MyQualifier real: Unit)
                    @Binds fun invalidBind2(@MyQualifier real: Unit): Unit
                    @Provides fun invalidBind3() {

                    }
                    @Provides fun invalidBind4(): Unit {
                     return Unit
                    }
                  }
                """
          )
          .indented(),
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/foo/MyQualifier.kt:14: Error: @Binds/@Provides must have a return type [BindingReturnType]
          @Provides fun invalidBind3() {
          ^
        src/foo/MyQualifier.kt:17: Error: @Binds/@Provides must have a return type [BindingReturnType]
          @Provides fun invalidBind4(): Unit {
          ^
        src/foo/MyQualifier.kt:12: Error: @Binds functions must be abstract [BindsMustBeAbstract]
          @Binds fun invalidBind1(@MyQualifier real: Unit)
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyQualifier.kt:13: Error: @Binds functions must be abstract [BindsMustBeAbstract]
          @Binds fun invalidBind2(@MyQualifier real: Unit): Unit
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        4 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `binds param counts`() {
    lint()
      .files(
        javaxInjectStubs,
        daggerStubs,
        kotlin(
            """
                  package foo
                  import dagger.Binds
                  import dagger.Module

                  @Module
                  interface MyModule {
                    @Binds fun validBind(real: Int): Number
                    @Binds fun invalidBind(real: Int, second: Int): Number
                    @Binds fun invalidBind(): Number
                  }
                """
          )
          .indented(),
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/foo/MyModule.kt:8: Error: @Binds must have one parameter [BindsWrongParameterCount]
          @Binds fun invalidBind(real: Int, second: Int): Number
                                ~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyModule.kt:9: Error: @Binds must have one parameter [BindsWrongParameterCount]
          @Binds fun invalidBind(): Number
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        2 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `binds must be abstract`() {
    lint()
      .files(
        javaxInjectStubs,
        daggerStubs,
        kotlin(
            """
                  package foo
                  import dagger.Binds
                  import dagger.Module

                  @Module
                  interface MyModule {
                    @Binds fun validBind(real: Int): Number
                    @Binds fun invalidBind(real: Int): Number { return real }
                    @Binds fun invalidBind(real: Int): Number = real
                  }

                  @Module
                  abstract class MyModule2 {
                    @Binds abstract fun validBind(real: Int): Number
                    @Binds fun invalidBind(real: Int): Number { return real }
                    @Binds fun invalidBind(real: Int): Number = real
                  }
                """
          )
          .indented(),
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/foo/MyModule.kt:8: Error: @Binds functions must be abstract [BindsMustBeAbstract]
          @Binds fun invalidBind(real: Int): Number { return real }
                                                    ~~~~~~~~~~~~~~~
        src/foo/MyModule.kt:9: Error: @Binds functions must be abstract [BindsMustBeAbstract]
          @Binds fun invalidBind(real: Int): Number = real
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyModule.kt:15: Error: @Binds functions must be abstract [BindsMustBeAbstract]
          @Binds fun invalidBind(real: Int): Number { return real }
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyModule.kt:16: Error: @Binds functions must be abstract [BindsMustBeAbstract]
          @Binds fun invalidBind(real: Int): Number = real
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        4 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `provides cannot be abstract`() {
    lint()
      .files(
        javaxInjectStubs,
        daggerStubs,
        kotlin(
            """
                  package foo
                  import dagger.Provides
                  import dagger.Module

                  @Module
                  interface MyModule {
                    @Provides fun invalidBind(real: Int): Number
                    @Provides fun invalidBind(real: Int): Number { return real }
                    @Provides fun invalidBind(real: Int): Number = real
                  }

                  @Module
                  abstract class MyModule2 {
                    @Provides abstract fun invalidProvides(real: Int): Number
                    @Provides fun validBind(real: Int): Number { return real }
                    @Provides fun validBind(real: Int): Number = real
                  }
                """
          )
          .indented(),
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/foo/MyModule.kt:7: Error: @Provides functions cannot be abstract [ProvidesMustNotBeAbstract]
          @Provides fun invalidBind(real: Int): Number
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyModule.kt:8: Error: @Provides functions cannot be abstract [ProvidesMustNotBeAbstract]
          @Provides fun invalidBind(real: Int): Number { return real }
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyModule.kt:9: Error: @Provides functions cannot be abstract [ProvidesMustNotBeAbstract]
          @Provides fun invalidBind(real: Int): Number = real
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyModule.kt:14: Error: @Provides functions cannot be abstract [ProvidesMustNotBeAbstract]
          @Provides abstract fun invalidProvides(real: Int): Number
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        4 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `must be in a module`() {
    lint()
      .files(
        javaxInjectStubs,
        daggerStubs,
        kotlin(
            """
                  package foo
                  import dagger.Binds
                  import dagger.Provides
                  import dagger.Module

                  interface MyModule {
                    @Binds fun invalidBind(real: Int): Number

                    companion object {
                      @Provides fun invalidBind(): Int = 3
                    }
                  }

                  abstract class MyModule2 {
                    @Binds abstract fun invalidBind(real: Int): Number

                    companion object {
                      @Provides fun invalidBind(): Int = 3
                    }
                  }

                  @Module
                  interface MyModule3 {
                    @Binds fun validBind(real: Int): Number

                    companion object {
                      @Provides fun validBind(): Int = 3
                    }
                  }

                  @Module
                  abstract class MyModule4 {
                    @Binds abstract fun validBind(real: Int): Number

                    companion object {
                      @Provides fun validBind(): Int = 3
                    }
                  }
                """
          )
          .indented(),
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/foo/MyModule.kt:7: Error: @Binds/@Provides functions must be in modules [MustBeInModule]
          @Binds fun invalidBind(real: Int): Number
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyModule.kt:10: Error: @Binds/@Provides functions must be in modules [MustBeInModule]
            @Provides fun invalidBind(): Int = 3
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyModule.kt:15: Error: @Binds/@Provides functions must be in modules [MustBeInModule]
          @Binds abstract fun invalidBind(real: Int): Number
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyModule.kt:18: Error: @Binds/@Provides functions must be in modules [MustBeInModule]
            @Provides fun invalidBind(): Int = 3
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        4 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
