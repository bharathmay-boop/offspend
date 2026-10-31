"use client";

import { useEffect, useRef, useState } from "react";
import { PhoneFrame, StoryScreen } from "./phone";

const steps = [
  ["A bank text arrives", "Your bank, card or UPI app sends its usual message. You do nothing different."],
  ["Offspend reads it on your phone", "It finds the amount, the payee and the currency, and suggests a category. Passwords and personal messages are skipped."],
  ["You tap Yes, Food", "One tap files it. If the guess is wrong, pick another category or add a note. Offspend remembers the correction for that payee."],
  ["The month adds up", "Every payment lands in your month, by category. Cash you add by hand is matched to the bank text when it arrives."],
] as const;

// Reading order is the plain HTML steps. On big, tall screens with motion allowed (CSS decides) one phone stays put
// and swaps its screen as each step scrolls past. Everywhere else every step shows its own phone.
export function Story() {
  const [active, setActive] = useState(0);
  const refs = useRef<(HTMLLIElement | null)[]>([]);

  useEffect(() => {
    const io = new IntersectionObserver(
      (entries) => {
        for (const e of entries) if (e.isIntersecting) setActive(Number((e.target as HTMLElement).dataset.step));
      },
      { rootMargin: "-45% 0px -45% 0px" },
    );
    refs.current.forEach((el) => el && io.observe(el));
    return () => io.disconnect();
  }, []);

  return (
    <div className="grid gap-12 lg:grid-cols-[1fr_320px] lg:gap-20">
      <ol className="m-0 grid list-none gap-16 p-0 lg:gap-0">
        {steps.map(([title, body], i) => (
          <li
            key={title}
            data-step={i}
            ref={(el) => { refs.current[i] = el; }}
            className="grid gap-6 lg:min-h-[70vh] lg:content-center"
          >
            <div className="grid gap-2">
              <p className="m-0 text-[0.8125rem] font-semibold uppercase tracking-[0.08em] text-brand">Step {i + 1}</p>
              <h3 className="m-0 text-[1.375rem] font-bold leading-tight">{title}</h3>
              <p className="m-0 max-w-[46ch] text-text-2">{body}</p>
            </div>
            <div className="story-inline" aria-hidden="true">
              <PhoneFrame decorative><StoryScreen step={i} /></PhoneFrame>
            </div>
          </li>
        ))}
      </ol>
      <div className="story-sticky" aria-hidden="true">
        <div className="sticky" style={{ top: "calc(var(--nav-h) + 24px)" }}>
          <PhoneFrame decorative>
            {steps.map((_, i) => (
              <div key={i} className="story-screen" data-active={active === i}><StoryScreen step={i} /></div>
            ))}
          </PhoneFrame>
        </div>
      </div>
    </div>
  );
}
