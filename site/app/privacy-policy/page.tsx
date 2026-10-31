import { readFileSync } from "node:fs";
import path from "node:path";
import type { Metadata } from "next";
import { Footer } from "@/components/sections";
import { Nav } from "@/components/nav";
import { renderPolicy } from "@/lib/policy";

export const metadata: Metadata = {
  title: "Privacy policy | Offspend",
  description: "What the Offspend app reads, what it keeps and what leaves your phone.",
  alternates: { canonical: "/privacy-policy" },
};

// One source of truth: the same file the app repo keeps in docs/. Read at build time.
const source = readFileSync(path.join(process.cwd(), "..", "docs", "privacy-policy.md"), "utf8");
const html = renderPolicy(source);

export default function PrivacyPolicy() {
  return (
    <>
      <Nav />
      <main id="main" className="mx-auto w-full max-w-[680px] px-5 py-16 md:py-24 [&_a]:text-brand [&_a]:underline [&_h1]:mb-4 [&_h1]:text-[clamp(2rem,5vw,3rem)] [&_h1]:font-extrabold [&_h2]:mb-3 [&_h2]:mt-10 [&_h2]:text-2xl [&_h2]:font-bold [&_li]:mb-2 [&_p]:my-4 [&_ul]:list-disc [&_ul]:pl-6">
        <div dangerouslySetInnerHTML={{ __html: html }} />
      </main>
      <Footer />
    </>
  );
}
