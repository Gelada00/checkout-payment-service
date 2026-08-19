# Design Considerations and Assumptions

This document covers the key design decisions and assumptions made while building this
payment gateway, as requested for the offline take-home submission.

## Overview

The service exposes two endpoints:

- `POST /payment` — validates a card payment request, forwards it to the acquiring
  bank simulator if valid, and returns an `Authorized`, `Declined`, or `Rejected` result.
- `GET /payment/{id}` — retrieves a previously processed payment by its ID.

The architecture is a straightforward layered design:

```
Controller  →  Service (validation + orchestration)  →  BankClient (acquiring bank HTTP call)
                                                     ↘  PaymentsRepository (in-memory store)
```

## Key decisions

**Validation happens entirely inside the gateway, before the bank is ever called.**
The bank simulator is treated as an external, untrusted, potentially slow/unreliable
dependency. Rejecting obviously invalid requests locally (bad card number, expired
date, unsupported currency, malformed CVV, non-positive amount) avoids unnecessary
network calls and matches the spec's definition of "Rejected" as *"no payment could
be created as invalid information was supplied to the payment gateway"* — i.e.
rejection is a gateway-side decision, not a bank-side one.

**CVV is modelled as a `String`, not a number.** The spec defines it as "3-4
characters, numeric" — a string-shaped constraint. Storing it as an `int` would
silently corrupt valid values with leading zeros (e.g. `"012"` becoming `12`) and
would fail the `100–9999` range check for that same reason. It's validated with the
regex `\d{3,4}`.

**Only three ISO currencies are supported (`GBP`, `USD`, `EUR`)**, per the
instruction to *"validate against no more than 3 currency codes."* These were chosen
as the most common currencies likely to appear in test/demo scenarios. Adding a
fourth is a one-line change to the `SUPPORTED_CURRENCIES` set.


**The full card number and CVV are never persisted or returned.** Only the last
four digits of the card number are stored/returned, matching the PCI-conscious
requirement in the spec. The full card number and CVV are also masked out of
`toString()` on the request object so they can't leak into application logs if the
object is ever logged by the framework or an exception handler.

**The bank simulator's expiry date format is zero-padded** (`04/2025`, not
`4/2025`) to match the example in the assessment spec exactly.

**HTTP status codes:**
- `200 OK` — payment processed (Authorized or Declined; both are "successful calls"
  from the gateway's point of view, differing only in the bank's decision).
- `400 Bad Request` — payment Rejected by the gateway (invalid input) or a malformed
  path variable on the GET endpoint.
- `404 Not Found` — no payment exists for the given ID.
- `503 Service Unavailable` — the acquiring bank could not be reached or errored.