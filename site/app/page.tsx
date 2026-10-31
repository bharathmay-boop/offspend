import { Benefits, Download, FaqSection, Footer, Hero, HowItWorks, PrivacyBand } from "@/components/sections";
import { Nav } from "@/components/nav";
import { faqs } from "@/lib/faqs";
import { SITE_URL } from "@/lib/config";

export default function Home() {
  const ld = {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "SoftwareApplication",
        name: "Offspend",
        operatingSystem: "Android",
        applicationCategory: "FinanceApplication",
        description: "Turns bank and UPI transaction SMS into expenses on your phone.",
        url: SITE_URL,
      },
      {
        "@type": "FAQPage",
        mainEntity: faqs.map((f) => ({ "@type": "Question", name: f.q, acceptedAnswer: { "@type": "Answer", text: f.a } })),
      },
    ],
  };
  return (
    <>
      <Nav />
      <main id="main">
        <Hero />
        <PrivacyBand />
        <HowItWorks />
        <Benefits />
        <FaqSection />
        <Download />
      </main>
      <Footer />
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(ld).replace(/</g, String.fromCharCode(92) + "u003c") }} />
    </>
  );
}
