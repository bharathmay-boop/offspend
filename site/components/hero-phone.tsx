"use client";

import { useEffect, useRef, useState } from "react";
import { ExampleTag, PhoneFrame } from "./phone";

// The one touch of delight: tap "Yes, Food" and the notification files itself. Works with motion off.
export function HeroPhone() {
  const [filed, setFiled] = useState(false);
  const touched = useRef(false);
  const yesRef = useRef<HTMLButtonElement>(null);
  const undoRef = useRef<HTMLButtonElement>(null);
  // The tapped button is replaced by the other one, so hand focus over instead of dropping it.
  useEffect(() => {
    if (touched.current) (filed ? undoRef : yesRef).current?.focus();
  }, [filed]);
  return (
    <div className="enter enter-2">
      <PhoneFrame>
        <div className="flex h-full flex-col gap-4 p-4 pt-10">
          <div aria-live="polite" className="rounded-2xl border border-border bg-surface p-3 text-[0.8125rem] leading-snug shadow-sm">
            <p className="mb-1 text-text-3">Offspend · now</p>
            {filed ? (
              <>
                <p className="m-0 font-semibold text-text"><span aria-hidden="true">✓ </span>Filed under Food</p>
                <button type="button" ref={undoRef} onClick={() => { touched.current = true; setFiled(false); }} className="mt-2 min-h-11 rounded-full px-3 font-semibold text-brand underline underline-offset-4">
                  Tap again to undo
                </button>
              </>
            ) : (
              <>
                <p className="m-0 font-semibold tabular">−₹320.00 · California Burrito</p>
                <p className="m-0 text-text-2">Food · Tap to change</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  <button type="button" ref={yesRef} onClick={() => { touched.current = true; setFiled(true); }} className="min-h-11 rounded-full bg-mint px-4 font-semibold text-on-mint hover:brightness-95">
                    Yes, Food
                  </button>
                </div>
              </>
            )}
          </div>
          <p className="m-0 px-1 text-[0.8125rem] text-text-3">{filed ? "That is the whole job." : "Try it: tap Yes, Food."}</p>
          <ExampleTag className="mt-auto self-start" />
        </div>
      </PhoneFrame>
    </div>
  );
}
