# Design system

Every screen in this app follows this file. If a screen needs something that is not here, add it here first, then use it.

The code lives in `app/src/main/kotlin/app/smsexpense/ui/theme/` (tokens) and `ui/components/` (shared pieces). Screens use those and never set their own colors, font sizes, corner sizes or animation timings.

## What the app should feel like

The main moment in this app is small and frequent: a payment happens, a notification shows the amount and a guessed category, and the person taps once. It happens many times a day, often while they are doing something else. So the design aims for four things.

| Principle | What it means in practice |
|---|---|
| **Calm** | Spending is normal, so it is shown in plain ink, never alarm red. Red is only for real errors. Money coming in gets a quiet green. |
| **Quick** | The common action is one tap and sits where the thumb is. Motion is short and springy, never something people wait for. |
| **Honest** | Show what was read from the message. When the app is unsure (no merchant found, low confidence), it says so and asks, instead of guessing quietly. |
| **Private** | Data stays on the phone, and the app says so in plain words where it matters (permission screens, settings). |

## Foundation

Material 3 through Jetpack Compose (`androidx.compose.material3`, from the Compose BOM). We use the ideas from Material 3 Expressive (larger rounded shapes, spring based motion, bold type for the most important number) through the stable Material 3 APIs, because the Expressive components are still internal or alpha in Compose at the time of writing. When they become stable, swap them in behind the same tokens.

Light and dark themes are both designed. Dynamic color (wallpaper colors) is off, so the money colors and category colors stay meaningful.

## Color

Brand color is a deep ledger green. Full role list is in `Color.kt`.

| Role | Light | Dark | Used for |
|---|---|---|---|
| primary | `#0D6B58` | `#8CD5BD` | Main buttons, selected states |
| primaryContainer | `#B8EFDC` | `#005142` | Selected category chip, highlights |
| tertiary | `#7A5900` | `#F5BF48` | "Needs your tap" (unconfirmed transactions) |
| surface | `#F6FAF7` | `#0F1512` | Screen background |
| onSurface | `#171D1A` | `#DEE4E0` | Text, and debit amounts |
| error | `#BA1A1A` | `#FFB4AB` | Errors only |

**Money colors** (in `AppColors`, read with `AppTheme.colors`):

- Debit: `onSurface`, shown with a leading minus sign. Never red.
- Credit: `credit` (`#1B7F3B` light, `#7BDA8F` dark), shown with a leading plus sign.
- Color is never the only signal: the sign and the words "Paid" or "Received" always come with it.

**Category colors**: eight fixed slots, assigned to categories in order and never cycled. A ninth category reuses a neutral color with its icon. Values are in `AppColors.categories`; they were taken from a palette checked for color blindness separation.

## Icons

Material Symbols, **Rounded** family only, added as vector drawables in `res/drawable` (no icon library). Each built-in category has one (Food: restaurant, Groceries: shopping cart, and so on, mapped in `categoryIconRes`). When creating a category, the person can pick one of 25 icons from the same family (`PickableIcons`), shown as a scrolling row under the name field; tapping the picked one again clears it. With no icon picked, the category shows its first letter. A payment with no category shows a question mark on the tertiary container color.

A category icon sits on a circle of the category's color, with the icon in ink or white, whichever has more contrast on that color. Sizes: 40dp at the start of a payment row, 24dp in chips and the spending breakdown. The icon is decorative: the category name next to it is what TalkBack reads.

## Type

Two typefaces, both bundled in the app (no download, no internet):

- **Bricolage Grotesque** for display text and the big amount on the confirm sheet. It has character, which is where the brand shows.
- **Inter** for everything else, with tabular figures so numbers line up in lists.

| Style | Face | Size / line | Weight | Where |
|---|---|---|---|---|
| amountHero | Bricolage | 44 / 52 | SemiBold | The amount on the confirm sheet |
| displaySmall | Bricolage | 34 / 40 | SemiBold | Month total |
| headlineSmall | Bricolage | 24 / 32 | SemiBold | Screen titles |
| titleMedium | Inter | 16 / 24 | SemiBold | Merchant names, section titles |
| bodyLarge | Inter | 16 / 24 | Regular | Body text |
| bodyMedium | Inter | 14 / 20 | Regular | Secondary text |
| labelLarge | Inter | 14 / 20 | SemiBold | Buttons, chips |
| labelMedium | Inter | 12 / 16 | Medium | Small labels, dates |
| amountRow | Inter | 16 / 24 | SemiBold, tabular | Amounts in lists |

## Shape

| Token | Radius | Where |
|---|---|---|
| extraSmall | 8dp | Small badges |
| small | 12dp | Chips, text fields |
| medium | 16dp | Cards, list groups |
| large | 24dp | Dialogs |
| extraLarge | 32dp | Top corners of bottom sheets |
| full | pill | Primary buttons, category chips |

## Spacing

A 4dp grid. Use `Spacing.xs` (4), `s` (8), `m` (12), `l` (16), `xl` (24), `xxl` (32), `xxxl` (48). Screen side padding is `l` (16dp). Space between list groups is `xl`.

## Motion

Spring based, from the Material 3 Expressive motion scheme. Use `Motion.fast` for small things (chip selection, button press), `Motion.default` for sheets and screen elements moving, and `Motion.effects` for fades and color changes. No tween durations in screens. Respect the system "remove animations" setting.

A confirmation stays on screen for `Motion.CONFIRM_HOLD_MS` (350ms) before what it confirms closes: picking a category on the sheet shows a check mark in the chip, then the sheet files and slides away.

The month total counts from its last value to the new one with `Motion.count()` instead of jumping, so a change gets noticed. The spending bar grows in from the left the first time it shows. Check `reducedMotion()` for anything that would otherwise animate on its own: when animations are off, show the end state at once.

Changing month slides the page like turning pages: a later month comes in from the right, an earlier one from the left, with a `Haptics.tick`. The chevron buttons and a horizontal swipe anywhere on the page both change month. The outgoing page keeps its own data while it leaves, so it never shows the new month's numbers.

## The month card

One glance, not a table. The total is the largest thing on the screen. Under it, one bar split by category in each category's color shows the shape of the month. Under that, the top 4 categories as pills (icon and amount); the rest are added up as "Other", unless only one would be left over. Tapping the bar or a pill shows every category with its own bar, and "Show less" folds it back.

## Haptics

Haptics say what happened, never decorate. Use the tokens in `Haptics` through `LocalHapticFeedback`, which follows the system haptics setting and needs no permission.

| Token | When |
|---|---|
| `Haptics.confirm` | Something was filed or saved |
| `Haptics.tick` | A small choice changed: a chip picked while a note is open, a month changed |
| `Haptics.reject` | An action could not complete |

## Filing a payment

On the category sheet one tap on a category files the payment. The app's guess is the first chip, labelled "Suggested". The note field sits behind an "Add note" chip. A Save button appears only while a note or a new category name is being typed, because then the tap is no longer the last step. A new category name can also be filed with the keyboard's Done key.

## Components

Shared pieces in `ui/components/`. Build new screens from these and from Material 3 components.

- `AmountText`: an amount with currency, sign and the right money color. Always use it for money.
- `CategoryIcon`: a category's icon (or letter) on a circle of its color.
- `CategoryChip`: a category with its icon. Selected and unselected states.
- `TransactionRow`: category icon, merchant, category and amount in one row. The day is not repeated on the row; the list groups payments under day headers ("Today", "Yesterday", "Tue, 6 Oct").
- `displayMerchant`: shows a payee the way people write it (Swiggy, not SWIGGY), keeping short forms like KFC and IRCTC in capitals. Use it wherever a merchant name is shown, including the notification.
- `PrivacyNote`: the short "stays on this phone" line with a lock icon.

## Adding a payment by hand

The home screen's one floating button, "Add" at the bottom right, opens the add sheet. The amount comes first with the number keypad already up, because it is the one thing always needed. Above it, a two-way toggle: Paid (default) or Received. The currency defaults to the one used most in the last 3 months. "Paid to" is optional and suggests names used before; a known name puts its learned category first as "Suggested". The date chip says "Today" and opens a date picker (no future dates). As on the category sheet, one tap on a category saves; with "Add note" open, a Save button does. Tapping a category without a valid amount saves nothing: it shows "Enter an amount above zero" and gives the reject haptic. Hand-added entries are stored as confirmed with source MANUAL.

## Editing and deleting

Tapping a payment opens its sheet with a three-dot menu ("More options") holding Edit and Delete. Edit opens the add sheet filled in. An entry the person typed in can change in full. One read from a bank SMS or notification keeps its amount, currency and direction, which show disabled with "From your bank's message, so it can't be changed"; its payee, date, category and note can change. Delete has no confirm dialog. The home screen shows "Payment deleted" with Undo, which is quicker and just as safe. A deleted entry is kept and hidden, so the same message is never filed again by a catch-up.

## Reviewing payments that need a category

When any payments in the month have no category, a line under the month card says "3 payments need a category" with a Review button. Review opens the category sheet for each in turn, with a "1 of 3" counter, and filing one moves straight to the next. A **Skip** chip after "Add note" leaves a payment as it is (no category, nothing learned) and moves on; it only shows in a review run. The line goes away when there is nothing left.

## Messages on the home screen

A message that is news once (how many payments the first import filed) is a snackbar, shown once, never a banner that stays. Cards that stay, like the offer to read UPI app notifications, get a title, one line and their buttons. While the first import runs, the list shows static placeholder rows in its own shape instead of claiming there are no payments. An empty month that is over says "No payments in September", never "yet".

## Accessibility

- Touch targets at least 48 by 48dp.
- Text contrast at least 4.5:1, large text 3:1, in both themes.
- Every amount is read out in full by TalkBack, for example "Paid 320 rupees to California Burrito, Food".
- Text scales with the system font size; nothing is clipped at 200%.

## Writing

Say less. One short line per idea, and anything longer goes behind a button that expands in place ("What does it read?"), except what a policy needs on screen. Prefer an icon and a few words over a sentence.

Use the words people use: "Paid", "Received", "Category", "Note". Not "debit transaction", not "entity". Buttons say what they do: "Looks right", "Change", "Add note". Errors say what happened and what to do next.
