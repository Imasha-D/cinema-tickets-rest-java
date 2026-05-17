package uk.gov.dwp.engineering.recruitment;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@Service
public class CinemaTicketsServiceImpl implements CinemaTicketsService {

  // dependencies
  private final PaymentService paymentService;
  private final SeatReservationService seatReservationService;

  // Constructor injection
  public CinemaTicketsServiceImpl(PaymentService paymentService,
      SeatReservationService seatReservationService) {
    this.paymentService = paymentService;
    this.seatReservationService = seatReservationService;
  }

  // constants : fixed ticket prices
  private static final int ADULT_PRICE = 25;
  private static final int CHILD_PRICE = 15;
  private static final int MAX_TICKETS = 25;

  private static final String SUCCESS_MESSAGE = "Ticket purchase completed successfully";

  @Override
  public String purchaseTickets(final Long accountId, final TicketRequest... ticketRequests)
      throws InvalidBookingException {

    // Basic validation : input validations
    this.validateAccount(accountId); // validate account
    this.validateTicketTypeRequests(ticketRequests); // validate Ticket type requests array

    // Create value object
    final PassengerCount passengerCount = countPassengers(ticketRequests);

    // validate business rules
    validateBusinessRules(passengerCount);

    // calculations
    final BigDecimal totalAmount = this.calculateTotalAmount(passengerCount); // calculate total amount
    final Long totalSeats = passengerCount.totalSeats(); // calculate seats

    // call third-party services
    paymentService.debitAccount(accountId, totalAmount);
    seatReservationService.reserveSeats(accountId, totalSeats);
    return SUCCESS_MESSAGE;
  }

  // helper methods
  private void validateAccount(Long accountId) {
    // Assumption 1 : have sufficient funds to pay for any number of tickets
    if (accountId == null || accountId <= 0)
      throw new InvalidBookingException(InvalidBookingException.Reason.INVALID_ACCOUNT_ID);
  }

  private void validateTicketTypeRequests(TicketRequest... ticketRequests) {
    if (ticketRequests == null)
      throw new InvalidBookingException(InvalidBookingException.Reason.EMPTY_REQUEST);
    if (ticketRequests.length == 0)
      throw new InvalidBookingException(InvalidBookingException.Reason.NULL_TICKET_REQUEST);

  }

  private PassengerCount countPassengers(final TicketRequest... ticketRequests) {
    int adultCount = 0;
    int childCount = 0;
    int infantCount = 0;

    for (final TicketRequest ticketRequest : ticketRequests) {
      // validate each ticket request
      validateTicketRequest(ticketRequest);

      switch (ticketRequest.type()) {
        case ADULT -> adultCount += ticketRequest.ticketCount();
        case CHILD -> childCount += ticketRequest.ticketCount();
        case INFANT -> infantCount += ticketRequest.ticketCount();
      }

    }

    return new PassengerCount(adultCount, childCount, infantCount);
  }

  private void validateTicketRequest(TicketRequest ticketRequest) {
    if (ticketRequest == null)
      throw new InvalidBookingException(InvalidBookingException.Reason.EMPTY_REQUEST);
    if (ticketRequest.type() == null)
      throw new InvalidBookingException(InvalidBookingException.Reason.NULL_TICKET_REQUEST);
    if (ticketRequest.ticketCount() <= 0)
      throw new InvalidBookingException(InvalidBookingException.Reason.INVALID_TICKET_QUANTITY);
  }

  private void validateBusinessRules(PassengerCount passengerCount) {
    // Rule 01; Max ticket count should be 25
    if (passengerCount.totalTickets() > MAX_TICKETS)
      throw new InvalidBookingException(InvalidBookingException.Reason.MAX_TICKET_LIMIT_EXCEEDED);

    // Rule 02 : at Least one Adult ticket required to have child and infant tickets
    if (passengerCount.adultCount() == 0 && (passengerCount.childCount > 0 || passengerCount.infantCount > 0))
      throw new InvalidBookingException(InvalidBookingException.Reason.ADULT_REQUIRED);

    // Rule 03 : Lap Rule (infant sit on Adult lap)
    if (passengerCount.infantCount() > passengerCount.adultCount())
      throw new InvalidBookingException(InvalidBookingException.Reason.INFANTS_EXCEED_ADULTS);

  }

  private BigDecimal calculateTotalAmount(PassengerCount passengerCount) {
    int totalAmount = passengerCount.adultCount() * ADULT_PRICE + passengerCount.childCount() * CHILD_PRICE;
    return BigDecimal.valueOf(totalAmount);

  }

  // ================= VALUE OBJECT =================
  private record PassengerCount(int adultCount, int childCount, int infantCount) {

    private int totalTickets() {
      return adultCount + childCount + infantCount;
    }

    private Long totalSeats() {
      return (long) adultCount + childCount;
    }
  }
}
