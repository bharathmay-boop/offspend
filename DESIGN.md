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

**Category colors**: eight fixed slots, assigned to categories in order and never cycled. A ninth category reuses a neutral color with its icon. Values are in `CategoryPalette`; they were taken from a palette checked for color blindness separation.

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

## Components

Shared pieces in `ui/components/`. Build new screens from these and from Material 3 components.

- `AmountText`: an amount with currency, sign and the right money color. Always use it for money.
- `CategoryChip`: a category with its color dot. Selected and unselected states.
- `TransactionRow`: merchant, category, date and amount in one row.
- `PrivacyNote`: the short "stays on this phone" line with a lock icon.

## Accessibility

- Touch targets at least 48 by 48dp.
- Text contrast at least 4.5:1, large text 3:1, in both themes.
- Every amount is read out in full by TalkBack, for example "Paid 320 rupees to California Burrito, Food".
- Text scales with the system font size; nothing is clipped at 200%.

## Writing

Use the words people use: "Paid", "Received", "Category", "Note". Not "debit transaction", not "entity". Buttons say what they do: "Looks right", "Change", "Add note". Errors say what happened and what to do next.
