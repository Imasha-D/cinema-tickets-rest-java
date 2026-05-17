# Cinema Tickets REST Java – Coding Exercise

## Candidate / Application Details

**Candidate Name:** Imasha Dilshani Kanangama Arachchige 
**Application ID:** 17547894  
**Vacancy Reference / Campaign Number:** 459300  
**Role:** Senior Java Software Engineer  
**Organisation:** Department for Work and Pensions  

---

## Overview

This project is a solution for the Department for Work and Pensions coding exercise for the Senior Java Software Engineer role.

The application implements the core cinema ticket booking service logic. It validates ticket purchase requests, applies the required business rules, calculates the correct payment amount, calculates the correct number of seats to reserve, and calls the provided external payment and seat reservation services.

The `CinemaTicketsController` has not been implemented, as the exercise instructions state that controller implementation is not required at this stage.

---

## Business Rules Implemented

The service supports three ticket types:

| Ticket Type | Price | Seat Required |
|---|---:|---|
| INFANT | £0 | No |
| CHILD | £15 | Yes |
| ADULT | £25 | Yes |

The following rules are enforced:

- A maximum of 25 tickets can be purchased at one time.
- An INFANT ticket is free.
- INFANT tickets do not require seat reservations because infants sit on an ADULT’s lap.
- CHILD tickets cannot be purchased without at least one ADULT ticket.
- INFANT tickets cannot be purchased without at least one ADULT ticket.
- The number of INFANT tickets cannot exceed the number of ADULT tickets.
- The account ID must be greater than zero.
- Each ticket request must contain a valid ticket type and a ticket count greater than zero.

---

## Validation Rules

The service rejects invalid bookings where:

- The account ID is `null`, zero, or negative.
- No ticket requests are provided.
- A ticket request is `null`.
- A ticket type is `null`.
- A ticket count is zero or negative.
- The total number of tickets exceeds 25.
- CHILD or INFANT tickets are requested without an ADULT ticket.
- The number of INFANT tickets is greater than the number of ADULT tickets.

Invalid requests throw an `InvalidBookingException`.

---

## Design Approach

### Service Layer

The main business logic is implemented in:

```text
src/main/java/uk/gov/dwp/engineering/recruitment/CinemaTicketsServiceImpl.java