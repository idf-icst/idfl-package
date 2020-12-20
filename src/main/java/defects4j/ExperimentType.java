package defects4j;

import java.util.function.Predicate;

public enum ExperimentType {
  SINGLE("single/", Bug::isSingleLocation),
  MULTIPLE("multiple/", Bug::isMultiple);

  String relativePath;
  Predicate<Bug> filter;

  ExperimentType(String s, Predicate<Bug> filter) {
    relativePath = s;
    this.filter = filter;
  }
}
