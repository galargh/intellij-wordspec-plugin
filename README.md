# IntelliJ WordSpec Plugin

This [IntelliJ Plugin](https://www.jetbrains.org/intellij/sdk/docs/intro/welcome.html) allows running single [WordSpecLike](https://www.scalatest.org/at_a_glance/WordSpec) [ScalaTest](https://www.scalatest.org) test cases from [IntelliJ IDEA](https://www.jetbrains.com/idea/) using [Gradle](https://gradle.org/).

It depends on [Scala Plugin](https://plugins.jetbrains.com/plugin/1347-scala).

To work correctly, the setup in which it is applied has to understand how to run *WordSpecLike* test cases when they are provided as [fully qualified name patterns](https://docs.gradle.org/current/userguide/java_testing.html#full_qualified_name_pattern).

If more than one `test` task is present on the *project*, user will be asked to choose which one to use on the first run of the configuration.

### Example

```scala
package example

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ExampleTest extends WordSpecLike with Matchers {
  "example" should {
    "showcase how to run a single test case" in {
      1 should be(1)
    }
  }
}
```

For the above code, the plugin would generate :: buttons in the left-hand side menu named:
- `example should *`
- `example should showcase how to run a single test case`

When clicked, run configurations with following parameters would be created (respectively):
- `--tests "example.ExampleTest.example should *"`
- `--tests "example.ExampleTest.example should showcase how to run a single test case"`
