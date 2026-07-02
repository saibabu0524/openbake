"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import Navbar from "@/components/layout/Navbar";
import Footer from "@/components/layout/Footer";
import Badge from "@/components/ui/Badge";
import Button from "@/components/ui/Button";
import { useAuthStore } from "@/store/authStore";
import { formatPrice, formatDate } from "@/lib/utils";
import api from "@/lib/api";
import toast from "react-hot-toast";
import type { Order, OrderStatus, OrderStatusEvent } from "@/types";

const BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8000/api";

const ALL_STATUSES: OrderStatus[] = ["placed", "accepted", "preparing", "dispatched", "delivered"];

const statusVariant: Record<OrderStatus, "success" | "warning" | "error" | "info" | "default"> = {
  placed: "info",
  accepted: "info",
  preparing: "warning",
  dispatched: "warning",
  delivered: "success",
  cancelled: "error",
};

export default function OrderTrackingClient() {
  const params = useParams();
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();

  // [...id] catch-all: real order id is read from the actual browser URL at
  // runtime, since this route is statically exported as a single shell.
  const rawId = params.id;
  const id = Array.isArray(rawId) ? rawId[0] : (rawId ?? "");

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  // SSE live tracking state
  const [sseConnected, setSseConnected] = useState(false);
  const [liveStatus, setLiveStatus] = useState<OrderStatus | null>(null);
  const [liveEta, setLiveEta] = useState<number | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);

  const currentStatus: OrderStatus = liveStatus ?? order?.status ?? "placed";
  const isTerminal = currentStatus === "delivered" || currentStatus === "cancelled";

  const fetchOrder = useCallback(async (silent = false) => {
    if (!silent) setRefreshing(true);
    try {
      const res = await api.get<Order>(`/orders/${id}`);
      setOrder((prev) => {
        if (prev && prev.status !== res.data.status && !liveStatus) {
          toast.success(`Order status updated to: ${res.data.status}`);
        }
        return res.data;
      });
      setLastUpdated(new Date());
    } catch {
      // silently ignore
    } finally {
      if (!silent) setRefreshing(false);
    }
  }, [id, liveStatus]);

  useEffect(() => {
    if (!id) return;
    if (!isAuthenticated) {
      router.push("/login");
      return;
    }
    api.get<Order>(`/orders/${id}`)
      .then((res) => {
        setOrder(res.data);
        setLastUpdated(new Date());
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [isAuthenticated, id, router]);

  // ── SSE real-time tracking ───────────────────────────────────────────────────
  useEffect(() => {
    if (loading || isTerminal || !order) return;

    const token = localStorage.getItem("access_token");
    const url = `${BASE_URL}/orders/${id}/stream${token ? `?token=${encodeURIComponent(token)}` : ""}`;

    const es = new EventSource(url);
    eventSourceRef.current = es;

    es.onopen = () => setSseConnected(true);

    es.onmessage = (event) => {
      try {
        const data: OrderStatusEvent = JSON.parse(event.data);
        setLiveStatus(data.status);
        if (data.estimated_delivery_minutes != null) setLiveEta(data.estimated_delivery_minutes);
        setLastUpdated(new Date());

        // Show toast on status change
        if (order && data.status !== order.status) {
          toast.success(`Order status: ${data.status}`);
        }

        // Terminal — close stream
        if (data.status === "delivered" || data.status === "cancelled") {
          es.close();
          setSseConnected(false);
          fetchOrder(true); // refresh full order
        }
      } catch {
        // ignore malformed events
      }
    };

    es.onerror = () => {
      setSseConnected(false);
      // EventSource auto-reconnects; no manual action needed
    };

    return () => {
      es.close();
      setSseConnected(false);
    };
  }, [loading, isTerminal, order?.id]); // eslint-disable-line react-hooks/exhaustive-deps

  // Fallback poll every 60s in case SSE drops
  useEffect(() => {
    if (loading || isTerminal || sseConnected) return;
    const timer = setInterval(() => fetchOrder(true), 60_000);
    return () => clearInterval(timer);
  }, [loading, isTerminal, sseConnected, fetchOrder]);

  const cancelOrder = async () => {
    try {
      const res = await api.patch<Order>(`/orders/${id}/cancel`);
      setOrder(res.data);
      setLiveStatus(null);
      setLastUpdated(new Date());
      toast.success("Order cancelled");
    } catch {
      toast.error("Could not cancel order");
    }
  };

  if (loading) {
    return (
      <>
        <Navbar />
        <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="bg-white rounded-2xl p-6 shadow-sm animate-pulse">
            <div className="h-6 bg-border/30 rounded w-1/3 mb-4" />
            <div className="h-4 bg-border/30 rounded w-1/2 mb-2" />
            <div className="h-4 bg-border/30 rounded w-full" />
          </div>
        </main>
        <Footer />
      </>
    );
  }

  if (!order) {
    return (
      <>
        <Navbar />
        <main className="flex-1 flex flex-col items-center justify-center py-20">
          <div className="text-5xl mb-4">😕</div>
          <h1 className="font-playfair text-2xl font-bold mb-2">Order not found</h1>
          <Link href="/orders"><Button>View All Orders</Button></Link>
        </main>
        <Footer />
      </>
    );
  }

  const currentIndex = currentStatus === "cancelled" ? -1 : ALL_STATUSES.indexOf(currentStatus);
  const eta = liveEta ?? order.estimated_delivery_minutes;

  return (
    <>
      <Navbar />
      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-start justify-between mb-2">
          <h1 className="font-playfair text-2xl font-bold">Order Tracking</h1>
          <div className="flex items-center gap-3">
            {sseConnected && (
              <span className="flex items-center gap-1.5 text-xs text-green-600 font-medium">
                <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
                Live
              </span>
            )}
            <button
              onClick={() => fetchOrder(false)}
              disabled={refreshing}
              className="flex items-center gap-1.5 text-sm text-primary hover:underline disabled:opacity-50"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="15"
                height="15"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                className={refreshing ? "animate-spin" : ""}
              >
                <path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8" />
                <path d="M21 3v5h-5" />
              </svg>
              {refreshing ? "Refreshing…" : "Refresh"}
            </button>
          </div>
        </div>
        <p className="text-text-secondary mb-1">Order #{order.id.slice(0, 8)} &middot; {formatDate(order.created_at)}</p>
        {lastUpdated && !isTerminal && (
          <p className="text-xs text-text-secondary mb-6">
            {sseConnected ? "Live tracking active" : "Polling every 60 s"} &mdash; last updated {lastUpdated.toLocaleTimeString()}
          </p>
        )}
        {isTerminal && <div className="mb-6" />}

        <div className="bg-white rounded-2xl p-6 shadow-sm mb-6">
          <div className="flex items-center gap-2 mb-6 flex-wrap">
            <span className="font-semibold">Status:</span>
            <Badge variant={statusVariant[currentStatus]}>
              {currentStatus.charAt(0).toUpperCase() + currentStatus.slice(1)}
            </Badge>
            {eta != null && !isTerminal && (
              <span className="text-sm text-text-secondary ml-2">ETA ~{eta} min</span>
            )}
          </div>

          {currentStatus !== "cancelled" ? (
            <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 sm:gap-0">
              {ALL_STATUSES.map((status, i) => (
                <div key={status} className="flex items-center gap-2 sm:flex-1">
                  <div
                    className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold shrink-0 ${
                      i <= currentIndex ? "bg-success text-white" : "bg-border/30 text-text-secondary"
                    }`}
                  >
                    {i <= currentIndex ? "✓" : i + 1}
                  </div>
                  <div className="flex flex-col">
                    <span className="text-sm capitalize">{status}</span>
                    {i <= currentIndex && order.status_timestamps?.[status] && (
                      <span className="text-[10px] text-text-secondary">
                        {new Date(order.status_timestamps[status]).toLocaleString("en-IN", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" })}
                      </span>
                    )}
                    {i === 0 && i <= currentIndex && !order.status_timestamps?.placed && (
                      <span className="text-[10px] text-text-secondary">
                        {new Date(order.created_at).toLocaleString("en-IN", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" })}
                      </span>
                    )}
                  </div>
                  {i < ALL_STATUSES.length - 1 && (
                    <div className={`hidden sm:block flex-1 h-px mx-2 ${i < currentIndex ? "bg-success" : "bg-border"}`} />
                  )}
                </div>
              ))}
            </div>
          ) : (
            <p className="text-error font-medium">This order has been cancelled.</p>
          )}
        </div>

        {/* Order Details */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 bg-white rounded-2xl p-6 shadow-sm">
            <h2 className="font-semibold text-lg mb-4">Order Items</h2>
            <div className="space-y-3">
              {order.items.map((item) => (
                <div key={item.id} className="flex items-center justify-between py-2 border-b border-border last:border-0">
                  <div>
                    <p className="font-medium">{item.product_name || "Product"}</p>
                    <p className="text-sm text-text-secondary">Qty: {item.quantity} &times; {formatPrice(item.unit_price)}</p>
                  </div>
                  <p className="font-semibold">{formatPrice(item.unit_price * item.quantity)}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-white rounded-2xl p-6 shadow-sm h-fit">
            <h2 className="font-semibold text-lg mb-4">Summary</h2>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-text-secondary">Subtotal</span>
                <span>{formatPrice(order.subtotal)}</span>
              </div>
              {order.discount > 0 && (
                <div className="flex justify-between text-success">
                  <span>Discount</span>
                  <span>-{formatPrice(order.discount)}</span>
                </div>
              )}
              <div className="flex justify-between">
                <span className="text-text-secondary">Delivery</span>
                <span>{order.delivery_fee > 0 ? formatPrice(order.delivery_fee) : "Free"}</span>
              </div>
              <div className="border-t border-border pt-2 mt-2 flex justify-between font-bold text-lg">
                <span>Total</span>
                <span className="text-primary">{formatPrice(order.total)}</span>
              </div>
            </div>

            <div className="mt-4 text-sm space-y-1">
              <p><span className="text-text-secondary">Payment:</span> {order.payment_method || "N/A"}</p>
              <p><span className="text-text-secondary">Payment Status:</span> {order.payment_status}</p>
              {order.time_slot && <p><span className="text-text-secondary">Time Slot:</span> {order.time_slot}</p>}
              {order.special_note && <p><span className="text-text-secondary">Note:</span> {order.special_note}</p>}
            </div>

            {(currentStatus === "placed" || currentStatus === "accepted") && (
              <Button variant="danger" className="w-full mt-4" onClick={cancelOrder}>
                Cancel Order
              </Button>
            )}
          </div>
        </div>
      </main>
      <Footer />
    </>
  );
}
