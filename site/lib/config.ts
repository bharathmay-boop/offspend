// The only file to edit when the Play listing or the contact address changes.
// PLAY_URL empty: the site shows the "not available yet" placeholder state.
// PLAY_URL set:   the Google Play badge and a QR code for this URL go live.
export const PLAY_URL = "";

// Empty: no contact link is rendered anywhere.
export const CONTACT_EMAIL = "founder@offspend.com";

// Empty: the footer repo link and the manifest proof link are hidden.
// Set to "" when the repo is made private.
export const REPO_URL = "https://github.com/bharathmay-boop/offspend";

export const SITE_URL = "https://offspend.com";

export const PLAY_URL_PATTERN = /^https:\/\/play\.google\.com\/store\/apps\/details\?id=[A-Za-z0-9._]+$/;

export function isValidPlayUrl(url: string): boolean {
  return url === "" || PLAY_URL_PATTERN.test(url);
}

export const MANIFEST_URL = REPO_URL ? `${REPO_URL}/blob/main/app/src/main/AndroidManifest.xml` : "";
