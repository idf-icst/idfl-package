package defects4j;

/**
 * Triggering mode
 */
public enum Mode {
  COMPLETE(),
  FIRST_FAILED_TEST(0),
  SECOND_FAILED_TEST(1),
  THIRD_FAILED_TEST(2),
  FOURTH_FAILED_TEST(3),
  FIFTH_FAILED_TEST(4),
  EXTRA_PASSED_TESTS_1(1),
  EXTRA_PASSED_TESTS_2(2),
  EXTRA_PASSED_TESTS_3(3),
  EXTRA_PASSED_TESTS_4(4),
  EXTRA_PASSED_TESTS_5(5),
  EXTRA_PASSED_TESTS_6(6),
  EXTRA_PASSED_TESTS_7(7),
  EXTRA_PASSED_TESTS_8(8),
  EXTRA_PASSED_TESTS_9(9),
  EXTRA_PASSED_TESTS_10(10);

  int value;

  Mode(int i) {
    value = i;
  }

  Mode() {

  }

  public int getValue() {
    return value;
  }
}
