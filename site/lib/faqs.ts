// Plain strings, so the same text feeds the page and the FAQ structured data. Every claim here is true of the current build.
export const faqs: { q: string; a: string }[] = [
  {
    q: "Is it safe to give an app SMS access?",
    a: "Android asks you once. Offspend uses it to find payment messages on your phone. It has no internet permission, so it cannot send anything out. It skips one-time passwords and personal messages, and it does not store the text of any message. Only the amount, payee, last digits, time, category and your note are kept.",
  },
  {
    q: "Why does it need SMS access?",
    a: "Your bank's text is the record of the payment. Reading it on your phone means you never type the amount. When you first set up, Offspend also files the last 90 days of bank messages so your history is not empty.",
  },
  {
    q: "Which banks and countries does it work with?",
    a: "It reads the usual formats from banks, cards and UPI apps, and works with any currency. Message formats vary, so a few may not be read. If one is missed, add it by hand and tell us from Settings, Report a problem.",
  },
  { q: "Is there an iPhone version?", a: "No. iOS does not let apps read text messages, so Offspend is Android only." },
  { q: "What does it cost?", a: "Free to try during the test. Public pricing is not set yet." },
  {
    q: "How do I delete my data?",
    a: "Uninstall the app, or clear its storage in Android settings. Nothing is kept anywhere else, because there is no account and no server.",
  },
  {
    q: "What if a message is read wrong?",
    a: "Tap the notification to change the category or add a note. Offspend remembers your correction for that payee. To report a message it got wrong, use Settings, Report a problem. It sends counts and states, never message text.",
  },
];
