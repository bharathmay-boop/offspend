# Google Play release checklist

Everything Google Play asks for before this app can go public, with the answers already written. Tracked in issue #15.

The app reads SMS, which Google Play restricts, so there are two extra gates on top of a normal release: the SMS permission declaration, and (for a new personal developer account) a closed test with at least 12 testers who stay opted in for 14 days in a row.

## 1. Accounts and setup

- [ ] Google Play Console developer account ($25 one time, identity verification).
- [ ] App name and icon chosen (the name is still open; the repo uses a working name).
- [ ] Privacy policy published at a public URL. The text is in [privacy-policy.md](privacy-policy.md); GitHub Pages on this repo is enough.
- [ ] Release build signed with an upload key kept outside the repo (never committed).

## 2. SMS permission declaration

Play Console, App content, Sensitive permissions. The app requests `READ_SMS` and `RECEIVE_SMS`.

**Core functionality to select:** SMS-based money management (apps that track and manage budgets).

**Why the app needs it (paste into the form):**

> The app's core function is tracking spending without typing. When the user's bank sends a transaction SMS, the app reads it on the device, extracts the amount, currency and payee, and files it as an expense or income entry the user confirms with one tap. RECEIVE_SMS is used to catch each payment as it happens. READ_SMS is used once at setup to file the last 90 days of bank messages, and afterwards only to catch up on bank messages the app missed while the phone had stopped it. Non-transaction messages (OTPs, personal messages, offers) are skipped and never stored. Only the parsed fields are kept, never the message text. All processing happens on the device, and the app has no internet permission, so no SMS content can leave the phone.

**Prominent disclosure:** the permission screen (`ui/PermissionScreen.kt`) is the first screen a new user sees, before Android's own prompt. Without any tap it says that the app reads SMS to find bank payments, that OTPs and personal messages are skipped, and that nothing leaves the phone. The fuller list (what is kept, the 3 month import, no internet) is under "What does it read?" on the same screen. Keep the visible sentence if the screen is redesigned again, since Play requires the disclosure in normal use.

**Demo video:** Play asks for a short video showing the feature. Record: install, permission screen, grant, a bank SMS arriving, the notification, tapping "Looks right", the monthly view. The walkthrough video made for the README can be reused.

**Possible questions from review and the answer:**

| Question | Answer |
|---|---|
| Is there an alternative that does not need SMS access? | No. The app's purpose is automatic capture from bank SMS. Manual entry is what the app replaces. |
| Is SMS data sent off the device? | No. The app does not request INTERNET; `PermissionsTest` fails the build if it ever does. |
| Why READ_SMS as well as RECEIVE_SMS? | To file past bank messages once at setup, and to catch up on messages missed while the system stopped the app. |

## 3. Notification access

The app also offers (off by default) to read payment notifications from a fixed list of payment and bank apps (`PaymentApps`). This uses a notification listener, which the user turns on in system settings. It is not a Play-restricted permission, but describe it in the store listing and privacy policy, and keep the explanation card that appears before the settings screen.

## 4. Data safety form

| Question | Answer | Why |
|---|---|---|
| Does the app collect or share any of the required user data types? | No | Nothing is sent off the device; the app has no internet permission. |
| Is all user data encrypted in transit? | Not applicable | No data is transmitted. |
| Can users request that data be deleted? | Yes | Uninstalling or clearing the app's storage deletes everything. |

Data that stays on the device (not "collected" in Play's sense, but worth stating in the listing): transaction amount, currency, direction, payee name as written in the message, the last digits of the account, the category and any note.

If Android backup is turned back on later (see issue #29), revisit this section: backed-up data goes to the user's own Google account.

## 5. Content rating and other declarations

- [ ] Content rating questionnaire: no violence, no user-generated content shared with others, no gambling. Expected rating: Everyone.
- [ ] Target audience: 18 and over (financial data).
- [ ] Financial features declaration: the app is a personal budgeting and expense tracking tool. It does not offer loans, payments, investment, banking or crypto services.
- [ ] Ads: none.

## 6. Closed test (new personal accounts)

- [ ] Create a closed testing track and upload the release build.
- [ ] Invite at least 12 testers, ideally with Indian bank accounts so the parser meets real message formats. Ask more than 12 (aim for 15 to 20), since testers who opt out reset the count.
- [ ] Keep at least 12 opted in for 14 consecutive days.
- [ ] Collect feedback and missed-message reports during the test.
- [ ] Apply for production access from the Dashboard and answer the questions about the test.

## 7. Store listing

- [ ] Short description (80 characters), for example: "Track spending from your bank SMS. One tap per payment. Nothing leaves your phone."
- [ ] Full description, screenshots (phone), feature graphic, app icon.
- [ ] Category: Finance.
- [ ] Contact email and the privacy policy URL.
