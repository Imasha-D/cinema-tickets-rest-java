package uk.gov.dwp.engineering.recruitment.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InvalidBookingExceptionTest {

  // @Test
  // void givenInvalidBookingException_whenGetMessage_thenExpectedValueReturned() {
  //   final InvalidBookingException invalidBookingException = new InvalidBookingException(
  //       "A useful message");
  //   assertEquals("A useful message", invalidBookingException.getMessage());
  // }

  @Test
  void shouldReturnExpectedReason() {
    final InvalidBookingException invalidBookingException =
        new InvalidBookingException(InvalidBookingException.Reason.EMPTY_REQUEST);

    assertEquals(InvalidBookingException.Reason.EMPTY_REQUEST, invalidBookingException.getReason());
  }

}
