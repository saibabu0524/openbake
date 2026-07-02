import type { MetadataRoute } from "next";

export const dynamic = "force-static";

const BASE = process.env.NEXT_PUBLIC_SITE_URL ?? "https://srivinayakabakery.in";

export default function sitemap(): MetadataRoute.Sitemap {
  return [
    { url: BASE,               lastModified: new Date(), changeFrequency: "daily",   priority: 1.0 },
    { url: `${BASE}/menu`,     lastModified: new Date(), changeFrequency: "daily",   priority: 0.9 },
    { url: `${BASE}/cart`,     lastModified: new Date(), changeFrequency: "weekly",  priority: 0.6 },
    { url: `${BASE}/orders`,   lastModified: new Date(), changeFrequency: "weekly",  priority: 0.5 },
    { url: `${BASE}/profile`,  lastModified: new Date(), changeFrequency: "monthly", priority: 0.4 },
    { url: `${BASE}/contact`,  lastModified: new Date(), changeFrequency: "monthly", priority: 0.3 },
    { url: `${BASE}/about`,    lastModified: new Date(), changeFrequency: "monthly", priority: 0.3 },
  ];
}
