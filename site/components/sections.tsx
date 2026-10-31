import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { CONTACT_EMAIL, MANIFEST_URL, PLAY_URL, REPO_URL } from "@/lib/config";
import { HeroPhone } from "./hero-phone";
import { ReleaseBlock } from "./release-block";
import { Story } from "./story";
import { Faq } from "./faq";
import { SmartphoneIcon, HandCoinsIcon, ShieldCheckIcon } from "lucide-react";

const wrap = "mx-auto w-full max-w-[1120px] px-5 md:px-8 lg:px-10";
const h2 = "m-0 text-[clamp(1.875rem,4vw,3rem)] font-bold leading-[1.1] tracking-[-0.015em]";
const eyebrow = "m-0 text-[0.8125rem] font-semibold uppercase tracking-[0.08em] text-brand";

export function Hero() {
  return (
    <section id="top" className={cn(wrap, "grid gap-12 py-16 md:py-24 min-[900px]:grid-cols-[1.05fr_0.95fr] min-[900px]:items-center")}>
      <div className="grid gap-6">
        <h1 className="m-0 text-[clamp(2.5rem,7vw,4.75rem)] font-extrabold leading-[1.04] tracking-[-0.02em]">
          Your bank already texts you. Offspend files it.
        </h1>
        <p className="m-0 max-w-[52ch] text-[clamp(1.125rem,2vw,1.375rem)] leading-[1.55] text-text-2">
          Turn bank and UPI transaction SMS into expenses on your Android phone. Offspend suggests a category; you confirm it with one tap.
        </p>
        <p className="m-0 font-semibold text-text">No bank login. No account. No internet permission.</p>
        <div className="flex flex-wrap gap-3">
          <a href="#how" className={cn(buttonVariants(), "h-12 rounded-full bg-brand px-6 text-base font-semibold text-bg hover:bg-brand-hover")}>
            See how it works
          </a>
        </div>
        <ReleaseBlock id="hero" />
      </div>
      <HeroPhone />
    </section>
  );
}

export function PrivacyBand() {
  const cols = [
    ["What it reads", "Android asks your permission to read messages. Offspend looks at bank, card and UPI transaction texts to find the amount, currency and payee. It skips one-time passwords and personal messages."],
    ["What it keeps", "The amount, payee, last digits of the account, time, category and your note. Not the text of the message."],
    ["Where it stays", "On your phone. Offspend has no internet permission, no account and no server. Uninstall it and it is gone."],
  ];
  const link = "font-semibold text-on-inverse underline underline-offset-4";
  return (
    <section id="privacy" className="on-inverse bg-surface-inverse py-16 text-on-inverse md:py-24">
      <div className={cn(wrap, "grid gap-10")}>
        <div className="grid gap-3">
          <p className={cn(eyebrow, "text-on-inverse")}>Privacy</p>
          <h2 className={cn(h2, "max-w-[20ch] text-on-inverse")}>Reading your bank texts is a big ask. Here is exactly what happens.</h2>
        </div>
        <div className="grid gap-8 md:grid-cols-3">
          {cols.map(([t, b]) => (
            <div key={t} className="grid content-start gap-2">
              <h3 className="m-0 text-[1.375rem] font-bold text-on-inverse">{t}</h3>
              <p className="m-0 text-on-inverse/85">{b}</p>
            </div>
          ))}
        </div>
        <p className="m-0 text-on-inverse/85">
          Alerts from payment apps are optional and off until you turn them on.{" "}
          <a className={link} href="/privacy-policy">Read the privacy policy</a>
          {MANIFEST_URL && (
            <>
              {" "}or <a className={link} href={MANIFEST_URL}>check the app&apos;s permission list</a>
            </>
          )}
          .
        </p>
      </div>
    </section>
  );
}

export function HowItWorks() {
  return (
    <section id="how" className={cn(wrap, "grid gap-10 py-16 md:py-24")}>
      <div className="grid gap-3">
        <p className={eyebrow}>How it works</p>
        <h2 className={cn(h2, "max-w-[18ch]")}>From a bank text to a filed expense in one tap.</h2>
        <p className="m-0 text-text-3">The phone screens here show invented sample data.</p>
      </div>
      <Story />
    </section>
  );
}

export function Benefits() {
  const items = [
    [SmartphoneIcon, "Nothing to type", "One tap files a payment. Cash you spend is added by hand in a few seconds."],
    [HandCoinsIcon, "Learns your corrections", "Fix a category once and Offspend remembers it for that payee. A bank text that matches a cash payment you added is matched, not counted twice."],
    [ShieldCheckIcon, "Built for how you pay", "Bank, card and UPI texts, in any currency. Bank message formats vary, so a few may not be read. Dark mode follows your phone. No ads."],
  ] as const;
  return (
    <section className="bg-surface-alt py-16 md:py-24">
      <div className={cn(wrap, "grid gap-10")}>
        <h2 className={cn(h2, "max-w-[20ch]")}>Small things that add up.</h2>
        <div className="grid gap-5 md:grid-cols-3">
          {items.map(([Icon, t, b]) => (
            <div key={t} className="grid content-start gap-3 rounded-[20px] border border-border bg-surface p-7">
              <Icon aria-hidden="true" className="size-7 text-brand" />
              <h3 className="m-0 text-[1.375rem] font-bold">{t}</h3>
              <p className="m-0 text-text-2">{b}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

export function FaqSection() {
  return (
    <section id="faq" className={cn(wrap, "grid gap-8 py-16 md:py-24")}>
      <h2 className={h2}>Questions people ask first</h2>
      <div className="max-w-[760px]"><Faq /></div>
    </section>
  );
}

export function Download() {
  return (
    <section id="download" className="border-t border-border bg-surface-alt py-16 md:py-24">
      <div className={cn(wrap, "grid gap-6")}>
        <h2 className={cn(h2, "max-w-[20ch]")}>{PLAY_URL ? "Get Offspend on Google Play." : "Offspend is in testing on Android."}</h2>
        <ReleaseBlock id="download-block" />
      </div>
    </section>
  );
}

export function Footer() {
  const link = "inline-flex min-h-11 items-center text-text-2 underline underline-offset-4 hover:text-text";
  return (
    <footer className="border-t border-border py-10">
      <div className={cn(wrap, "grid gap-4 text-[0.9375rem] text-text-2")}>
        <nav aria-label="Footer" className="flex flex-wrap gap-x-6">
          <a className={link} href="/privacy-policy">Privacy policy</a>
          {REPO_URL && <a className={link} href={REPO_URL}>GitHub</a>}
          {CONTACT_EMAIL && <a className={link} href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>}
        </nav>
        <p className="m-0">
          © 2026 Offspend. {PLAY_URL && "Google Play and the Google Play logo are trademarks of Google LLC. "}Android is a trademark of Google LLC.
        </p>
      </div>
    </footer>
  );
}
