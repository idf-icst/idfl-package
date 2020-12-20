package statistics.entity;

public enum TestStatus {
  PASSED("passed"),
  FAILED("failed");

  String stringValue;

  TestStatus(String value) {
    stringValue = value;
  }
}
