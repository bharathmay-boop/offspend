# SMS Expense Tracker (working name)

An Android app that reads bank and UPI transaction SMS on your phone and asks you to confirm the category with one tap. No typing, no bank login, and your spending data never leaves your phone.

## How it works

1. A bank SMS arrives.
2. The app reads the amount, currency and merchant on the phone.
3. A notification shows the amount and a guessed category.
4. One tap saves it, or you change the category or add a note.

## Status

Early build. Android first, tested in India. iOS later through a Shortcuts automation, since iOS apps cannot read SMS. Work is tracked on the GitHub Projects board.

## Privacy

Spending data stays on the device. Any product stats are opt-in and contain no amounts, merchants or message text.
