# Payment address parity report

Masked regions: MT103 party address blocks; pacs.008 PstlAdr nodes; event address nodes plus the schemaVersion line (1 to 2). All other bytes must match.

MT103 detail fields are space-packed into 35-character numbered lines; town continuations repeat 3/ without repeating the country. Stated countries must match the IBAN country.

| Format | Payloads compared | Identical | Changed only where rule says | Mismatches |
|---|---:|---:|---:|---:|
| MT103 | 240 | 0 | 234 | 6 |
| pacs.008 | 240 | 0 | 234 | 6 |
| event | 240 | 0 | 234 | 6 |
| total | 720 | 0 | 702 | 18 |

## Payment-level summary

| Payments compared | Identical | Changed only per rule | Held for operations or mismatched |
|---:|---:|---:|---:|
| 240 | 0 | 234 | 6 |

## Mismatches

- PAY000001 / MT103: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000001 / pacs.008: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000001 / event: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000041 / MT103: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000041 / pacs.008: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000041 / event: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000081 / MT103: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000081 / pacs.008: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000081 / event: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000121 / MT103: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000121 / pacs.008: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000121 / event: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000161 / MT103: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000161 / pacs.008: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000161 / event: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000201 / MT103: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000201 / pacs.008: held for operations (no town; not guessed): creditor postal address: town name is missing
- PAY000201 / event: held for operations (no town; not guessed): creditor postal address: town name is missing

## Operations list

- PAY000001 | creditor | [Warehouse Seven, P.O. Box 540, FR] | creditor postal address: town name is missing
- PAY000041 | creditor | [Warehouse Seven, P.O. Box 580, FR] | creditor postal address: town name is missing
- PAY000081 | creditor | [Warehouse Seven, P.O. Box 620, DE] | creditor postal address: town name is missing
- PAY000121 | creditor | [Warehouse Seven, P.O. Box 660, DE] | creditor postal address: town name is missing
- PAY000161 | creditor | [Warehouse Seven, P.O. Box 700, GB] | creditor postal address: town name is missing
- PAY000201 | creditor | [Warehouse Seven, P.O. Box 740, FR] | creditor postal address: town name is missing

Addresses whose country came from IBAN: **192**
