import QRCode from "qrcode";
import { PLAY_URL } from "@/lib/config";

// Reads lib/config.ts. Empty PLAY_URL: honest placeholder. Set: official badge + QR made at build time (no third party).
export async function ReleaseBlock({ id }: { id: string }) {
  if (!PLAY_URL) {
    return (
      <div className="grid gap-4" aria-labelledby={`${id}-status`}>
        <p id={`${id}-status`} className="m-0 text-text-2">
          <b className="text-text">Android only.</b> In friends-and-family testing on Google Play. A public download link is not available yet.
        </p>
        <div className="flex flex-wrap items-center gap-4">
          <div aria-hidden="true" className="flex h-[60px] w-[200px] items-center justify-center rounded-lg border-2 border-dashed border-border px-3 text-center text-[0.8125rem] font-semibold text-text-3">
            Google Play badge goes here
          </div>
          <div aria-hidden="true" className="hidden h-[120px] w-[120px] items-center justify-center rounded-lg border-2 border-dashed border-border p-2 text-center text-[0.8125rem] font-semibold text-text-3 min-[900px]:[@media(hover:hover)]:flex">
            QR code appears here
          </div>
        </div>
      </div>
    );
  }
  const svg = await QRCode.toString(PLAY_URL, { type: "svg", margin: 4, errorCorrectionLevel: "M", color: { dark: "#000000", light: "#ffffff" } });
  const qr = `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
  return (
    <div className="grid gap-4">
      <p className="m-0 text-text-2"><b className="text-text">Android only.</b> Free to try during the test.</p>
      <div className="flex flex-wrap items-center gap-4">
        <a href={PLAY_URL} className="inline-block">
          {/* official, unmodified badge from Google's badge generator */}
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src="/google-play-badge.png" alt="Get it on Google Play" width={200} height={60} />
        </a>
        <div className="hidden min-[900px]:[@media(hover:hover)]:block">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={qr} alt="QR code for the Google Play page" width={120} height={120} />
        </div>
      </div>
      <p className="m-0 text-[0.9375rem]"><a className="text-brand underline underline-offset-4" href={PLAY_URL}>{PLAY_URL}</a></p>
    </div>
  );
}
