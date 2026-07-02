import OrderTrackingClient from "./OrderTrackingClient";

// See app/menu/[...id]/page.tsx for why this is a catch-all + single shell.
export function generateStaticParams() {
  return [{ id: ["placeholder"] }];
}

export default function OrderTrackingPage() {
  return <OrderTrackingClient />;
}
