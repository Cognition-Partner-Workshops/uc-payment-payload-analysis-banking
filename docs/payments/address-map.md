# Outbound payment address map

## Entry and storage

Addresses enter through `PaymentController.payloads` and `PaymentController.create` in
`src/main/java/com/cbarkinozer/onlinebankingrestapi/app/pay/PaymentController.java`. Jackson binds each
debtor and creditor to `PaymentInstruction.Party`. A party supplies exactly one of:

- legacy `addressLines` (up to three 35-character lines), retained so recorded and existing requests deserialize; or
- `postalAddress`, the validated ISO PostalAddress24 subset in `PostalAddress.java`.

`PostalAddressStructurer.structure` is the single normalization point. It returns supplied structured data unchanged,
or deterministically extracts only values supported by legacy text, including numeric and two-token UK postcodes
(`SW1A 2AA`, `M1 1AE`, `EC1V 9HX`, `B33 8TH`). It takes country from the IBAN only when the text does not contain
one. A stated ISO country (including supplied structured data) must match the IBAN country; otherwise normalization
fails with `stated country X conflicts with IBAN country Y`. Non-ISO region tokens such as `NY` and `TX` are retained
in locality text, never used as countries. Missing or conflicting towns and more than two hybrid leftovers are rejected.

## Outbound writers

All writers are in `PaymentRenderer.render` and receive the same normalized debtor and creditor addresses.

| Payload | Before | After | Writer |
|---|---|---|---|
| MT103 | `:50K:` / `:59:` followed by name and free-text lines | `:50F:` / `:59F:`, numbered `1/` name, `2/` address detail and mandatory `3/CC/Town`; at most four numbered lines | `PaymentRenderer.mt103`, `mtParty` |
| pacs.008 | `<PstlAdr>` containing only `<AdrLine>` | PostalAddress24 order: `StrtNm`, `BldgNb`, `BldgNm`, `PstBx`, `PstCd`, `TwnNm`, `Ctry`, then at most two `AdrLine` | `PaymentRenderer.pacs008`, `xmlParty` |
| Kafka `outbound-payments` event | Jackson serialization of `Party.addressLines`, schema version 1 | Explicit schema-version 2 party object containing `name`, `iban`, `bic`, then ordered `postalAddress` fields; null and empty values omitted | `PaymentRenderer.event`, `eventParty`; published by `PaymentPublisher.publish` |

If normalization or MT103 line limits fail, `AddressIncompleteException` identifies the party and reason.
`PaymentController` returns HTTP 400 before `PaymentPublisher.publish` can run.

MT103 numbered lines are limited to 35 characters. Detail fields are joined with spaces and packed across `2/` lines
without truncation. Long towns use a first `3/CC/` line followed by `3/` continuations, splitting at word boundaries
where possible. All name, detail, and town lines count toward the four-line limit; overflow is rejected with
`address too long for MT103 option F`, distinct from `town name is missing`.

## Parity evidence

`PaymentReplayTest` replays every request in `payloads/recorded`, masks only the address regions (and the event
`schemaVersion` line for the version 1 to 2 rule change), and compares all other bytes. It writes
`target/parity/parity.csv` and `target/parity/parity-report.md`; known no-town records are listed for operations rather
than completed by guessing.
