# Security Policy

This repository contains the Online Banking REST API (Spring Boot, Maven) and its
outbound payments module, which renders MT103 / pacs.008 payloads and Kafka events.
Because it handles account, card, loan and payment data, please treat any
vulnerability report as confidential.

## Supported Versions

Only the latest commit on the `main` branch is supported. There are no maintained
release branches; fixes are applied to `main` only.

| Version              | Supported |
| -------------------- | --------- |
| `main` (latest)      | Yes       |
| Any older commit/tag | No        |

## Reporting a Vulnerability

**Do not open a public issue, pull request or discussion for security problems.**

Report vulnerabilities privately through GitHub Security Advisories:

1. Go to the repository's **Security** tab.
2. Select **Advisories** > **Report a vulnerability**
   (direct link: <https://github.com/Cognition-Partner-Workshops/uc-payment-payload-analysis-banking/security/advisories/new>).
3. Include the affected component (e.g. `app/acc`, `app/pay`, `app/sec`), steps to
   reproduce, impact, and any suggested fix. Never include real customer data,
   card numbers, credentials or JWT signing keys; use synthetic data only.

If the private reporting form is unavailable, contact the repository owners listed
in [`.github/CODEOWNERS`](.github/CODEOWNERS) and ask them to open a draft advisory.

## What to Expect

- Acknowledgement within 3 business days.
- An initial assessment and severity (CVSS) within 10 business days.
- Fixes are developed in a private advisory fork and released to `main`; the
  advisory is published once a fix is available. Reporters are credited unless
  they prefer otherwise.

## Scope

In scope: authentication/authorization (Spring Security, JWT), account and money
transfer logic, credit card and loan handling, payment payload rendering, Kafka
messaging, and dependencies declared in `pom.xml`, the `Dockerfile` and GitHub
Actions workflows.
