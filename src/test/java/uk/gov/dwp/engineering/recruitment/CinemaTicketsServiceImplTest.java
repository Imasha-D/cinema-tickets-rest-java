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
            Long accountId = 1L;

            ticketService.purchaseTickets(accountId, request(TicketType.ADULT, 2));

            verify(ticketPaymentService).debitAccount(accountId, BigDecimal.valueOf(50));
            verify(seatReservationService).reserveSeats(accountId, 2L);
            verifyNoMoreInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        void shouldPurchaseAdultAndChildTicketsSuccessfully() {
            Long accountId = 7L;

            ticketService.purchaseTickets(
                    accountId,
                    request(TicketType.ADULT, 1),
                    request(TicketType.CHILD, 1));

            verify(ticketPaymentService).debitAccount(accountId, BigDecimal.valueOf(40));
            verify(seatReservationService).reserveSeats(accountId, 2L);
            verifyNoMoreInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        void shouldPurchaseAdultChildInfantTicketsSuccessfully() {
            Long accountId = 1L;

            ticketService.purchaseTickets(
                    accountId,
                    request(TicketType.ADULT, 2), request(TicketType.CHILD, 2), request(TicketType.INFANT, 1));

            verify(ticketPaymentService).debitAccount(accountId, BigDecimal.valueOf(80));
            verify(seatReservationService).reserveSeats(accountId, 4L);
            verifyNoMoreInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        void shouldAllowInfantsEqualToAdults() { // boundary case
            Long accountId = 6L;

            ticketService.purchaseTickets(
                    accountId,
                    request(TicketType.ADULT, 2),
                    request(TicketType.INFANT, 2));

            verify(ticketPaymentService).debitAccount(accountId, BigDecimal.valueOf(50));
            verify(seatReservationService).reserveSeats(accountId, 2L);
            verifyNoMoreInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        void shouldAggregateMultipleRequestsOfSameTicketType() {
            Long accountId = 5L;

            ticketService.purchaseTickets(
                    accountId,
                    request(TicketType.ADULT, 1),
                    request(TicketType.ADULT, 2),
                    request(TicketType.CHILD, 1));

            verify(ticketPaymentService).debitAccount(accountId, BigDecimal.valueOf(90));
            verify(seatReservationService).reserveSeats(accountId, 4L);
            verifyNoMoreInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        void shouldCalculatePaymentAndSeatsCorrectlyForMixedTickets() {
            Long accountId = 2L;

            ticketService.purchaseTickets(
                    accountId,
                    request(TicketType.ADULT, 2),
                    request(TicketType.CHILD, 3),
                    request(TicketType.INFANT, 1));

            verify(ticketPaymentService).debitAccount(accountId, BigDecimal.valueOf(95));
            verify(seatReservationService).reserveSeats(accountId, 5L);
            verifyNoMoreInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        void shouldNotAllocateSeatsForInfants() {
            Long accountId = 3L;

            ticketService.purchaseTickets(
                    accountId,
                    request(TicketType.ADULT, 2),
                    request(TicketType.INFANT, 2));

            verify(ticketPaymentService).debitAccount(accountId, BigDecimal.valueOf(50));
            verify(seatReservationService).reserveSeats(accountId, 2L);
            verifyNoMoreInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        void shouldAllowExactlyTwentyFiveTickets() {
            Long accountId = 4L;

            ticketService.purchaseTickets(
                    accountId,
                    request(TicketType.ADULT, 10),
                    request(TicketType.CHILD, 10),
                    request(TicketType.INFANT, 5));

            verify(ticketPaymentService).debitAccount(accountId, BigDecimal.valueOf(400));
            verify(seatReservationService).reserveSeats(accountId, 20L);
            verifyNoMoreInteractions(ticketPaymentService, seatReservationService);
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

    private void assertInvalidPurchase(InvalidBookingException.Reason expectedReason, Executable action) {

        InvalidBookingException exception = assertThrows(InvalidBookingException.class, action);
        assertEquals(expectedReason, exception.getReason());
        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

}
