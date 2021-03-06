package com.flexport.bazeljunit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

class BazelJUnit5ConsoleLauncherTest {

  static class TestClass {
    void testMethod() {}

    void testMethod(int a) {}

    void testMethod(int a, Integer b) {}

    void testMethod1() {}
  }

  @Test
  void fixXmlOutputFile() throws IOException {
    Path tempFile = File.createTempFile("test-", ".xml").toPath();
    Files.write(tempFile, "foo-bar-baz".getBytes());

    Path reportsDir = tempFile.getParent();
    Files.move(tempFile, reportsDir.resolve("TEST-junit-jupiter.xml"));
    Path xmlOutputFile = reportsDir.resolve("test.xml");

    ReflectionTestUtils.invokeMethod(
        BazelJUnit5ConsoleLauncher.class, "fixXmlOutputFile", xmlOutputFile.toString());

    assertThat(xmlOutputFile.toFile().exists()).isTrue();
    assertThat(new String(Files.readAllBytes(xmlOutputFile), StandardCharsets.UTF_8))
        .isEqualTo("foo-bar-baz");
  }

  @ParameterizedTest(name = "{0}; {1} -> {2}")
  @CsvSource(
      value = {
          "''; ; ; ''",
          "--opt Opt; ; ; --opt Opt",
          "--opt Opt; ''; ; --opt Opt",
          "''; com.flexport.bazeljunit5; ; --select-package=com.flexport.bazeljunit5",
          "''; com.flexport.bazeljunit5; ''; --select-package=com.flexport.bazeljunit5",
          "''; com.flexport.bazeljunit5; 'foo/bar.xml'; "
              + "--select-package=com.flexport.bazeljunit5 --reports-dir=foo",
      },
      delimiter = ';')
  void transformArgs(
      String argsText, String testOnly, String xmlOutputFile, String expectedArgsText) {
    String[] args = argsText.isEmpty() ? new String[] {} : argsText.split(" ");

    String[] newArgs =
        ReflectionTestUtils.invokeMethod(
            BazelJUnit5ConsoleLauncher.class, "transformArgs", args, testOnly, xmlOutputFile);

    String[] expectedArgs =
        expectedArgsText.isEmpty() ? new String[] {} : expectedArgsText.split(" ");

    assertThat(newArgs).isEqualTo(expectedArgs);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource(
      value = {
          "{package}; --select-package={package}",
          "{class}; --select-class={class}",
          "{class}#; --select-class={class}",
          "{class}#testMethod; "
              + "--select-method={class}#testMethod()|"
              + "--select-method={class}#testMethod(int)|"
              + "--select-method={class}#testMethod(int, java.lang.Integer)",
          "{class}#testMethod$; "
              + "--select-method={class}#testMethod()|"
              + "--select-method={class}#testMethod(int)|"
              + "--select-method={class}#testMethod(int, java.lang.Integer)",
          "{class}#testMethod(); --select-method={class}#testMethod()",
          "{class}#testMethod1; --select-method={class}#testMethod1()",
      },
      delimiter = ';')
  void parseOptions(String testOnly, String expectedOptionsText) {
    List<String> options =
        ReflectionTestUtils.invokeMethod(
            BazelJUnit5ConsoleLauncher.class,
            "parseOptions",
            testOnly.replace("{class}", TestClass.class.getName()));

    List<String> expectedOptions =
        expectedOptionsText.isEmpty()
            ? Arrays.asList()
            : Arrays.stream(expectedOptionsText.split("\\|"))
            .map(s -> s.replace("{class}", TestClass.class.getName()))
            .collect(Collectors.toList());

    assertThat(options).hasSameElementsAs(expectedOptions);
  }

  @Test
  @DisplayName("parseOptions - class not found")
  void parseOptionsWithException() {
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                ReflectionTestUtils.invokeMethod(
                    BazelJUnit5ConsoleLauncher.class,
                    "parseOptions",
                    "AnUnknownTestClass#testMethod"));

    assertThat(exception).hasCauseInstanceOf(ClassNotFoundException.class);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource(
      value = {
          "'', ''",
          "--select-package com.flexport, ''",
          "--select-package=com.flexport, ''",
          "--select-package, ''",
          "--select-package=com.flexport --select-package com.flexport --opt=Opt, --opt=Opt",
          "--opt=Opt, --opt=Opt",
          "--opt Opt, --opt Opt",
          "Opt, Opt",
      })
  void filterOptions(String argsText, String expectedArgsText) {
    List<String> args = argsText.isEmpty() ? Arrays.asList() : Arrays.asList(argsText.split(" "));
    List<String> newArgs =
        ReflectionTestUtils.invokeMethod(
            BazelJUnit5ConsoleLauncher.class,
            "filterOptions",
            args,
            Arrays.asList("--select-package"));

    List<String> expectedArgs =
        expectedArgsText.isEmpty() ? Arrays.asList() : Arrays.asList(expectedArgsText.split(" "));
    assertThat(newArgs).isEqualTo(expectedArgs);
  }
}
