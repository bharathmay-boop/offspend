import { Renderer, marked } from "marked";

const escape = (s: string) => s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
const SAFE_HREF = /^(https?:|mailto:|\/|#)/i;

// Renders the policy markdown. Raw HTML is shown as text, and links must be http, https, mailto or relative.
export function renderPolicy(source: string): string {
  const renderer = new Renderer();
  renderer.html = ({ text }) => escape(text);
  const link = renderer.link.bind(renderer);
  renderer.link = (token) => (SAFE_HREF.test(token.href) ? link(token) : escape(token.text));
  return marked.parse(source, { async: false, renderer }) as string;
}
