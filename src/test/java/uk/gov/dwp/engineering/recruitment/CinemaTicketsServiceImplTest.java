package uk.gov.dwp.engineering.recruitment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

class CinemaTicketsServiceImplTest {

    private PaymentService ticketPaymentService;
    private SeatReservationService seatReservationService;
    private CinemaTicketsServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketPaymentService = mock(PaymentService.class);
        seatReservationService = mock(SeatReservationService.class);
        ticketService = new CinemaTicketsServiceImpl(ticketPaymentService, seatReservationService);
    }

    // Success Scenario : Happy Path
    @Nested
    class ValidPurchaseTests {

        @Test
        void shouldPurchaseAdultTicketsSuccessfully() {

            assertSuccessfulPurchase(
                    1L,
                    BigDecimal.valueOf(50),
                    2L,
                    request(TicketType.ADULT, 2));

        }

        @Test
        void shouldPurchaseAdultAndChildTicketsSuccessfully() {

            assertSuccessfulPurchase(
                    2L,
                    BigDecimal.valueOf(40),
                    2L,
                    request(TicketType.ADULT, 1),
                    request(TicketType.CHILD, 1));
        }

        @Test
        void shouldPurchaseAdultChildInfantTicketsSuccessfully() {

            assertSuccessfulPurchase(
                    3L,
                    BigDecimal.valueOf(80),
                    4L,
                    request(TicketType.ADULT, 2),
                    request(TicketType.CHILD, 2),
                    request(TicketType.INFANT, 1));

        }

        @Test
        void shouldAllowInfantsEqualToAdults() { // boundary case

            assertSuccessfulPurchase(
                    4L,
                    BigDecimal.valueOf(50),
                    2L,
                    request(TicketType.ADULT, 2),
                    request(TicketType.INFANT, 2));
        }

        @Test
        void shouldAggregateMultipleRequestsOfSameTicketType() {

            assertSuccessfulPurchase(
                    5L,
                    BigDecimal.valueOf(90),
                    4L,
                    request(TicketType.ADULT, 1),
                    request(TicketType.ADULT, 2),
                    request(TicketType.CHILD, 1));
        }

        @Test
        void shouldCalculatePaymentAndSeatsCorrectlyForMixedTickets() {

            assertSuccessfulPurchase(
                    6L,
                    BigDecimal.valueOf(95),
                    5L,
                    request(TicketType.ADULT, 2),
                    request(TicketType.CHILD, 3),
                    request(TicketType.INFANT, 1));
        }

        @Test
        void shouldNotAllocateSeatsForInfants() {
            assertSuccessfulPurchase(
                    7L,
                    BigDecimal.valueOf(50),
                    2L,
                    request(TicketType.ADULT, 2),
                    request(TicketType.INFANT, 2));
        }

        @Test
        void shouldAllowExactlyTwentyFiveTickets() {

            assertSuccessfulPurchase(
                    8L,
                    BigDecimal.valueOf(400),
                    20L,
                    request(TicketType.ADULT, 10),
                    request(TicketType.CHILD, 10),
                    request(TicketType.INFANT, 5));
        }

    }

    // Invalid Accounts check
    @Nested
    class InvalidAccountTests {
        @Test
        void shouldRejectWhen_AccountIdIsNull() {

            TicketRequest req = request(TicketType.ADULT, 2);
            assertInvalidPurchase(InvalidBookingException.Reason.INVALID_ACCOUNT_ID,
                    () -> ticketService.purchaseTickets(null, req));

        }

        @Test
        void shouldRejectWhen_AccountIdEqualsZero() {
            TicketRequest req = request(TicketType.ADULT, 2);
            assertInvalidPurchase(InvalidBookingException.Reason.INVALID_ACCOUNT_ID,
                    () -> ticketService.purchaseTickets(0L, req));

        }

        @Test
        void shouldRejectWhen_AccountIdIsNegative() {
            TicketRequest req = request(TicketType.ADULT, 2);
            assertInvalidPurchase(InvalidBookingException.Reason.INVALID_ACCOUNT_ID,
                    () -> ticketService.purchaseTickets(-1L, req));

        }
    }

    // Invalid Ticket Request Check
    @Nested
    class InvalidTicketRequests {

        @Test
        void shouldRejectWhen_TicketRequestIsNull() {

            assertInvalidPurchase(InvalidBookingException.Reason.EMPTY_REQUEST,
                    () -> ticketService.purchaseTickets(1L, (TicketRequest[]) null));

        }

        @Test
        void shouldRejectWhen_TicketRequestIsNotProvided() {

            assertInvalidPurchase(InvalidBookingException.Reason.NULL_TICKET_REQUEST,
                    () -> ticketService.purchaseTickets(1L));

        }

        @Test
        void shouldRejectWhen_RequestContainsNullTicketTypeRequest() {

            assertInvalidPurchase(InvalidBookingException.Reason.EMPTY_REQUEST,
                    () -> ticketService.purchaseTickets(1L, (TicketRequest) null));

        }

        @Test
        void shouldRejectWhen_RequestHasNoTicketType() {
            TicketRequest req = request(null, 2);
            assertInvalidPurchase(InvalidBookingException.Reason.NULL_TICKET_REQUEST,
                    () -> ticketService.purchaseTickets(1L, req));

        }

        @Test
        void shouldRejectWhen_RequestTicketQuantityIsZero() {
            TicketRequest req = request(TicketType.ADULT, 0);
            assertInvalidPurchase(InvalidBookingException.Reason.INVALID_TICKET_QUANTITY,
                    () -> ticketService.purchaseTickets(1L, req));

        }

        @Test
        void shouldRejectWhen_RequestTicketQuantityIsNegative() {
            TicketRequest req = request(TicketType.ADULT, -1);
            assertInvalidPurchase(InvalidBookingException.Reason.INVALID_TICKET_QUANTITY,
                    () -> ticketService.purchaseTickets(1L, req));

        }

    }

    @Nested
    class BusinessRuleValidationTests {

        // Rule 01; Max ticket count should be 25
        @Test
        void shouldRejectWhen_MoreThanTwentyFiveTicketsPurchased() {
            TicketRequest req = request(TicketType.ADULT, 26);
            assertInvalidPurchase(InvalidBookingException.Reason.MAX_TICKET_LIMIT_EXCEEDED,
                    () -> ticketService.purchaseTickets(1L, req));

        }

        @Test
        void shouldRejectWhen_TotalTicketsAcrossRequestsExceedTwentyFive() {
            assertInvalidPurchase(InvalidBookingException.Reason.MAX_TICKET_LIMIT_EXCEEDED,
                    () -> ticketService.purchaseTickets(
                            1L,
                            request(TicketType.ADULT, 10),
                            request(TicketType.CHILD, 10),
                            request(TicketType.INFANT, 6)));
        }

        // Rule 02 : at Least one Adult ticket required to have child and infant tickets
        @Test
        void shouldRejectWhen_ChildTicketsWithoutAdult() {
            TicketRequest req = request(TicketType.CHILD, 2);
            assertInvalidPurchase(InvalidBookingException.Reason.ADULT_REQUIRED,
                    () -> ticketService.purchaseTickets(1L, req));

        }

        @Test
        void shouldRejectWhen_InfantTicketsWithoutAdult() {
            TicketRequest req = request(TicketType.INFANT, 2);
            assertInvalidPurchase(InvalidBookingException.Reason.ADULT_REQUIRED,
                    () -> ticketService.purchaseTickets(1L, req));

        }

        @Test
        void shouldRejectWhen_ChildAndInfantTicketsWithoutAdult() {
            TicketRequest req1 = request(TicketType.CHILD, 2);
            TicketRequest req2 = request(TicketType.INFANT, 2);
            assertInvalidPurchase(InvalidBookingException.Reason.ADULT_REQUIRED,
                    () -> ticketService.purchaseTickets(1L, req1, req2));

        }

        // Rule 03 : Lap Rule (infant sit on Adult lap)
        @Test
        void shouldRejectWhen_InfantsMoreThanAdults() {
            TicketRequest req1 = request(TicketType.ADULT, 1);
            TicketRequest req2 = request(TicketType.INFANT, 2);

            assertInvalidPurchase(InvalidBookingException.Reason.INFANTS_EXCEED_ADULTS,
                    () -> ticketService.purchaseTickets(1L, req1, req2));

        }

    }

    private TicketRequest request(TicketType type, int quantity) {
        return new TicketRequest(type, quantity);
    }

    // Helper
    private void assertSuccessfulPurchase(
            final Long accountId,
            final BigDecimal expectedAmount,
            final Long expectedSeats,
            final TicketRequest... ticketRequests) {

        final String result = ticketService.purchaseTickets(accountId, ticketRequests);

        assertEquals("Ticket purchase completed successfully", result);
        verify(ticketPaymentService).debitAccount(accountId, expectedAmount);
        verify(seatReservationService).reserveSeats(accountId, expectedSeats);
        verifyNoMoreInteractions(ticketPaymentService, seatReservationService);
    }

    private void assertInvalidPurchase(InvalidBookingException.Reason expectedReason, Executable action) {

        InvalidBookingException exception = assertThrows(InvalidBookingException.class, action);
        assertEquals(expectedReason, exception.getReason());
        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

}
