---
status: accepted
---

# Require Customer registration

Remove the bootstrap Customer account and its automatic recovery behavior, superseding ADR 0016. Every Customer identity must be created through the phone-and-invitation registration workflow so no built-in account bypasses invitation binding, registration-gift accounting, or Customer statistics; retain the built-in `customer` role and its mandatory binding for every active Customer.
