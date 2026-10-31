"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { MenuIcon } from "lucide-react";
import { Sheet, SheetContent, SheetTitle, SheetTrigger } from "@/components/ui/sheet";

const links = [
  ["How it works", "/#how"],
  ["Privacy", "/#privacy"],
  ["FAQ", "/#faq"],
  ["Release status", "/#download"],
] as const;

export function Nav() {
  const [scrolled, setScrolled] = useState(false);
  useEffect(() => {
    const on = () => setScrolled(window.scrollY > 8);
    on();
    window.addEventListener("scroll", on, { passive: true });
    return () => window.removeEventListener("scroll", on);
  }, []);
  const link = "inline-flex min-h-11 items-center rounded-lg px-2 text-text-2 hover:text-text";
  return (
    <header
      data-scrolled={scrolled}
      className="sticky top-0 z-40 border-b border-transparent bg-[rgba(var(--bg-rgb),0.94)] data-[scrolled=true]:border-border"
      style={{ minHeight: "var(--nav-h)" }}
    >
      <a href="#main" className="absolute left-3 top-[-100px] z-50 rounded-lg bg-brand px-4 py-3 font-semibold text-bg focus:top-3">Skip to content</a>
      <nav aria-label="Main" className="mx-auto flex max-w-[1120px] items-center justify-between px-5 py-2 md:px-8 lg:px-10">
        <Link href="/" className="font-[family-name:var(--font-display)] text-xl font-extrabold tracking-tight text-text">Offspend</Link>
        <div className="hidden items-center gap-2 md:flex">
          {links.map(([t, h]) => <a key={h} href={h} className={link}>{t}</a>)}
        </div>
        <Sheet>
          <SheetTrigger className="inline-flex size-11 items-center justify-center rounded-lg text-text md:hidden" aria-label="Open menu">
            <MenuIcon aria-hidden="true" />
          </SheetTrigger>
          <SheetContent side="right" className="bg-bg">
            <SheetTitle className="px-4 pt-4 font-[family-name:var(--font-display)] text-xl">Offspend</SheetTitle>
            <div className="grid gap-1 p-4">
              {links.map(([t, h]) => <a key={h} href={h} className={link + " text-lg"}>{t}</a>)}
            </div>
          </SheetContent>
        </Sheet>
      </nav>
    </header>
  );
}
