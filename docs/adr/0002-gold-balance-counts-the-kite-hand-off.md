# ADR-0002: The Zen Gold balance counts the Kite hand-off as the purchase

- **Status**: Accepted (interim, until a real purchase acknowledgement exists)
- **Date**: 2026-10-09
- **Deciders**: srinivas

## Context

Zen Gold lets a user who kept their screen-time promise this week invest in gold: they pick a
quantity, review the order, and "Open Kite to authorise" hands them to Zerodha Kite, where they
place the order themselves. ZenMode never holds money, never places the order, and has no way to
see whether the order went through: there's no payment callback or broker API on this path.

Until now the "Gold invested" balance on Home and Zen Gold was a hardcoded ₹0, and the original
Gold Streak plan said a redirect alone must never count as invested: the user would self-report
"I invested ₹X" each week instead. That self-report step was never built, so the balance, and
every share card built on it, stayed at ₹0 forever.

## Decision

For now, the hand-off counts as the purchase. Tapping "Open Kite to authorise" records that order
(units × unit price) in `GoldLedger`, an on-device record. That button is only reachable in a week
whose promise was kept. The balance on Home, Zen Gold and the share cards is the sum of those
orders. Each week holds one order: a second hand-off in the same week replaces the first instead
of adding to it, so a retry can't count twice.

The user is told before and after:

- the review screen says, above the button, that opening Kite adds the order to their Zen Gold
  total and that we can't see whether it went through;
- the gold share sheet says the total counts the orders opened in Kite from Zen Gold.

## Alternatives considered

- **Self-reported "I invested ₹X" per week** (the original plan): honest, but it's one more step
  after a hand-off, and it was never built. It can still be added as a correction on top of this.
- **Keep ₹0 until a real acknowledgement exists** (a broker postback, such as Kite Publisher's
  order status): correct, but the balance and its reward loop stay dead until then.
- **Count every hand-off**: a retry after Kite failed to open would double the week.

## Consequences

- The balance is what the user *set out* to buy, not what their broker holds. An order abandoned
  in Kite still counts. The copy says so; the number is never presented as a portfolio value.
- The amount uses the unit price shown on the review screen, which is a placeholder until there's
  a live price. For the same reason the "+n%" change badge is hidden (`changePercent = null`): with
  no price there's no gain or loss to show, and the old fixed "+38%" would have read as a real one.
- On-device only: clearing app data, reinstalling or deleting the account resets the balance.
  Syncing it (Firestore) is follow-up work, as is replacing this with a real acknowledgement once
  one exists.
