import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

// A plain drawn phone, not a photographed device. All amounts and payees are invented sample data.
export function PhoneFrame({ children, decorative, className }: { children: ReactNode; decorative?: boolean; className?: string }) {
  return (
    <div
      className={cn("relative mx-auto w-full max-w-[280px] rounded-[2.25rem] border border-border bg-surface p-3 shadow-[0_24px_48px_-24px_rgba(var(--text-rgb),0.28)]", className)}
      style={{ aspectRatio: "9 / 19.5" }}
      aria-hidden={decorative ? true : undefined}
    >
      <div className="relative h-full overflow-hidden rounded-[1.6rem] bg-bg">{children}</div>
    </div>
  );
}

export function ExampleTag({ className }: { className?: string }) {
  return <span className={cn("inline-block rounded-full bg-warm px-3 py-1 text-[0.8125rem] font-semibold leading-tight text-on-warm", className)}>Example transaction</span>;
}

export function SmsScreen() {
  return (
    <div className="flex h-full flex-col gap-3 p-4 pt-8">
      <p className="text-[0.8125rem] font-semibold text-text-3">Messages</p>
      <div className="max-w-[92%] rounded-2xl rounded-tl-sm bg-surface-alt p-3 text-[0.8125rem] leading-snug text-text">
        <p className="mb-1 font-semibold">MY-BANK</p>
        Rs 320.00 debited from A/c XX1234 on 12-Oct to VPA california.burrito@upi. UPI Ref 4021 5566 7788.
      </div>
      <ExampleTag className="mt-auto self-start" />
    </div>
  );
}

export function MonthScreen() {
  const rows = [
    ["Food", "California Burrito", "−₹320.00"],
    ["Travel", "Metro card", "−₹200.00"],
    ["Groceries", "Corner store", "−₹845.50"],
  ];
  return (
    <div className="flex h-full flex-col gap-3 p-4 pt-8">
      <p className="font-[family-name:var(--font-display)] text-lg font-bold">October</p>
      <ul className="m-0 grid list-none gap-2 p-0">
        {rows.map(([cat, who, amt]) => (
          <li key={who} className="flex items-center justify-between gap-2 rounded-xl bg-surface p-3 text-[0.8125rem]">
            <span className="min-w-0"><b className="block">{who}</b><span className="text-text-3">{cat}</span></span>
            <span className="tabular shrink-0 font-semibold">{amt}</span>
          </li>
        ))}
      </ul>
      <ExampleTag className="mt-auto self-start" />
    </div>
  );
}

// Static copy of the notification, used by the story. The hero has an interactive one.
export function NotificationCard({ filed, children }: { filed?: boolean; children?: ReactNode }) {
  return (
    <div className="rounded-2xl border border-border bg-surface p-3 text-[0.8125rem] leading-snug shadow-sm">
      <p className="mb-1 text-text-3">Offspend · now</p>
      {filed ? (
        <p className="m-0 font-semibold text-text"><span aria-hidden="true">✓ </span>Filed under Food</p>
      ) : (
        <>
          <p className="m-0 font-semibold tabular">−₹320.00 · California Burrito</p>
          <p className="m-0 text-text-2">Food · Tap to change</p>
        </>
      )}
      {children}
    </div>
  );
}

export function StoryScreen({ step }: { step: number }) {
  if (step === 0) return <SmsScreen />;
  if (step === 3) return <MonthScreen />;
  return (
    <div className="flex h-full flex-col gap-3 p-4 pt-8">
      <NotificationCard filed={step === 2}>
        {step === 1 && (
          <div className="mt-2 flex gap-2" aria-hidden="true">
            <span className="rounded-full bg-mint px-3 py-1.5 font-semibold text-on-mint">Yes, Food</span>
            <span className="rounded-full border border-border px-3 py-1.5 font-semibold text-text-2">Add note</span>
          </div>
        )}
      </NotificationCard>
      <ExampleTag className="mt-auto self-start" />
    </div>
  );
}
