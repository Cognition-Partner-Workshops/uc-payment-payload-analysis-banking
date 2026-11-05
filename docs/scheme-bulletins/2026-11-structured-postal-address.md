# Cross-border postal address requirements

Effective November 2026, cross-border ISO 20022 credit transfers must carry a structured or hybrid postal address for both debtor and creditor. Fully unstructured postal addresses will be rejected.

Each postal address must include town name (`TwnNm`) and ISO 3166-1 alpha-2 country code (`Ctry`). A structured address uses dedicated address elements. A hybrid address combines structured town and country with a maximum of two `AdrLine` elements.

For equivalent MT103 messages, use option 50F for the ordering customer and option 59F for the beneficiary customer. Include a `3/CC/Town` line, where `CC` is the two-letter country code and `Town` is the town name.

Participants must obtain missing town and country information from the instructing party before submission. An address with insufficient location information must not be completed by guessing.
