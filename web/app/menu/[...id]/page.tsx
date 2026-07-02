import ProductDetailClient from "./ProductDetailClient";

// Static export can't pre-render one page per product id (unknown at build
// time), so this catch-all segment builds exactly one static shell; the
// Spring Boot server serves that same shell for any /menu/* deep link, and
// ProductDetailClient reads the real id from the browser URL at runtime.
export function generateStaticParams() {
  return [{ id: ["placeholder"] }];
}

export default function ProductDetailPage() {
  return <ProductDetailClient />;
}
