import type { Metadata, Viewport } from "next";
import "./globals.css";
import { SITE_URL } from "@/lib/config";

const description =
  "Turn bank and UPI transaction SMS into expenses on your phone. Confirm categories with one tap. No account, no server, no internet permission.";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: "Offspend: expense tracking from bank SMS for Android",
  description,
  alternates: { canonical: "/" },
  openGraph: {
    title: "Offspend: expense tracking from bank SMS for Android",
    description,
    url: SITE_URL,
    siteName: "Offspend",
    type: "website",
    images: [{ url: "/og.png", width: 1200, height: 630, alt: "Offspend" }],
  },
  twitter: { card: "summary_large_image", title: "Offspend", description, images: ["/og.png"] },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#F6FAF7" },
    { media: "(prefers-color-scheme: dark)", color: "#0F1512" },
  ],
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <head>
        <link rel="preload" href="/fonts/inter-latin.woff2" as="font" type="font/woff2" crossOrigin="anonymous" />
        <link rel="preload" href="/fonts/bricolage-latin.woff2" as="font" type="font/woff2" crossOrigin="anonymous" />
        <script dangerouslySetInnerHTML={{ __html: "document.documentElement.classList.add('js')" }} />
      </head>
      <body>{children}</body>
    </html>
  );
}
