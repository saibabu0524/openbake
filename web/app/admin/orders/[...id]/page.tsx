import AdminOrderDetailClient from "./AdminOrderDetailClient";

// See app/menu/[...id]/page.tsx for why this is a catch-all + single shell.
export function generateStaticParams() {
  return [{ id: ["placeholder"] }];
}

export default function AdminOrderDetailPage() {
  return <AdminOrderDetailClient />;
}
