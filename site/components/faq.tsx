"use client";

import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import { faqs } from "@/lib/faqs";

export function Faq() {
  return (
    <>
    <noscript>
      <dl className="m-0 grid gap-6">
        {faqs.map(({ q, a }) => (
          <div key={q}><dt className="font-semibold text-text">{q}</dt><dd className="m-0 text-text-2">{a}</dd></div>
        ))}
      </dl>
    </noscript>
    <Accordion className="faq-acc rounded-2xl border border-border bg-surface px-5">
      {faqs.map(({ q, a }) => (
        <AccordionItem key={q} value={q} className="border-border">
          <AccordionTrigger className="min-h-11 py-4 text-[1.0625rem] font-semibold text-text">{q}</AccordionTrigger>
          <AccordionContent className="text-[1.0625rem] leading-relaxed text-text-2">{a}</AccordionContent>
        </AccordionItem>
      ))}
    </Accordion>
    </>
  );
}
