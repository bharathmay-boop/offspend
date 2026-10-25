# Testing the app with friends and family

This guide is for people trying an early build of the expense tracker. Tracked in issue #103.

## What the app does

When your bank sends you a text about a payment, the app reads it on your phone and shows a notification with the amount and a guessed category. One tap files it. You can also add payments and money you received by hand.

## What it reads, and what it can't do

- It reads your text messages to find bank and UPI payment messages. OTPs, offers and personal messages are skipped and never stored.
- It keeps only the amount, the payee's name, the date, the last digits of the account, and the category you pick. The message text itself is not saved.
- It has no internet permission at all, so it has no way to send anything off your phone.

## Installing

### From the Play Store (the main way)

1. Send Bharath the email address of the Google account you use for the Play Store on your phone.
2. Once you're added, open the invite link he sends you, on your phone.
3. Tap "Accept invite", then "Download it on Google Play", and install it like any other app.

Updates arrive through the Play Store like any other app.

### From a file (only if the Play Store route isn't possible)

Bharath may send you an `.apk` file instead.

- In India, Play Protect may block an app that reads SMS when it's installed from WhatsApp, a browser or a file manager. If that happens, use the Play Store route instead. Don't try to get around the block.
- Outside India, open the file and allow your browser or messaging app to install apps when it asks.
- On Android 13 and later, if the SMS permission is greyed out with "Restricted setting", open the app's page in Settings, tap the three dots at the top right, then "Allow restricted settings", and try again.

## Updating

**Never uninstall the app to update it.** The app doesn't back itself up to Google (so your spending stays on your phone), which means uninstalling deletes everything, including payments you added by hand. The Play Store updates in place. If you were sent a file, install the new file over the old one.

## What to try in your first week

- Make a few payments as you normally would (UPI, card, bank transfer) and check each one shows up.
- The guessed category is already filed. Tap "Yes, <category>" (or "Okay" for a shop you've filed before) to clear it, or tap the notification to change it.
- Add a cash payment by hand with the Add button.
- Edit and delete an entry.
- Look at the month summary at the top of the home screen.

## Telling us what went wrong

The most useful reports are:

- **A payment that never showed up.** Say which bank or app it was from and roughly when. Please don't send the message itself; it has your account details in it.
- **A payment read wrong.** The wrong amount, the wrong payee, or money received shown as spent.
- **Anything confusing.** If you had to stop and think, that's worth telling us.
- **A crash.** What you were doing just before.

Send these to Bharath with the report from the app: open Settings, tap **Report a problem**, read what it says, and tap **Share report**. It lists the app version, your phone and Android version, whether the SMS and notification permissions are on, whether battery saver limits the app, and a few counts. It has no messages, amounts, payees or notes. Pick WhatsApp or email in the share sheet, then add a line about what happened.

## For Bharath: test build versions

Each test build gets a higher version number (0.1.0 test 1, test 2, and so on). Ask testers for the version when they report a problem, so each report can be matched to the build it came from.
