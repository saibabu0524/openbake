import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Fail the build on TS errors in CI
  typescript: { ignoreBuildErrors: false },

  // Static export — the output is embedded into the Spring Boot jar and
  // served as static files from the same origin as the API, so next/image's
  // server-side optimization pipeline (which needs a Node server) isn't
  // available; images are served as-is via the Cloudinary/Unsplash URLs
  // already baked into the product data.
  images: { unoptimized: true },

  output: "export",

  // Security headers (CSP, X-Frame-Options, etc.) moved to the Spring Boot
  // server — the headers() config option isn't supported for static export
  // since there's no Next.js server to apply them at request time.
};

export default nextConfig;
