package uk.gov.dwp.engineering.recruitment.exception;

public class InvalidBookingException extends RuntimeException {

  private final Reason reason;

  // public InvalidBookingException(String message) {
  //   super(message);
  // }

  public InvalidBookingException(final Reason reason) {
    super(reason.getMessage());
    this.reason = reason;
  }

  public InvalidBookingException(final Reason reason, final String customMessage) {
    super(customMessage);
    this.reason = reason;
  }

  public Reason getReason() {
    return reason;
  }

  public enum Reason {
    INVALID_ACCOUNT_ID("Account ID must be greater than zero"),
    EMPTY_REQUEST("At least one ticket request must be provided"),
    NULL_TICKET_REQUEST("Ticket request must not be null"),
    INVALID_TICKET_TYPE("Ticket type must not be null"),
    INVALID_TICKET_QUANTITY("Ticket count must be greater than zero"),
    MAX_TICKET_LIMIT_EXCEEDED("Cannot purchase more than 25 tickets at once"),
    ADULT_REQUIRED("Child and infant tickets require at least one adult ticket"),
    INFANTS_EXCEED_ADULTS("Each infant must be accompanied by one adult");

    private final String message;

    Reason(final String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }
  }

}
