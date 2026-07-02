"use client";

import { useEffect, useState } from "react";
import { useRouter, useParams } from "next/navigation";
import { useAuthStore } from "@/store/authStore";
import { formatPrice, formatDate } from "@/lib/utils";
import Badge from "@/components/ui/Badge";
import Button from "@/components/ui/Button";
import api from "@/lib/api";
import toast from "react-hot-toast";
import type { Order, OrderStatus } from "@/types";
import {
  ArrowLeft,
  MapPin,
  Phone,
  Mail,
  User,
  Clock,
  CreditCard,
  Package,
  Truck,
  ExternalLink,
} from "lucide-react";

interface CustomerInfo {
  id: string;
  name: string | null;
  email: string | null;
  phone: string | null;
  profile_image_url: string | null;
}

interface AdminOrderDetail extends Order {
  customer?: CustomerInfo;
}

const NEXT_STATUS: Record<string, string> = {
  placed: "accepted",
  accepted: "preparing",
  preparing: "dispatched",
  dispatched: "delivered",
};

const statusVariant: Record<OrderStatus, "success" | "warning" | "error" | "info" | "default"> = {
  placed: "info",
  accepted: "info",
  preparing: "warning",
  dispatched: "warning",
  delivered: "success",
  cancelled: "error",
};

export default function AdminOrderDetailClient() {
  const router = useRouter();
  const params = useParams();

  // [...id] catch-all: real order id is read from the actual browser URL at
  // runtime, since this route is statically exported as a single shell.
  const rawId = params.id;
  const orderId = Array.isArray(rawId) ? rawId[0] : (rawId ?? "");

  const { isAuthenticated, user } = useAuthStore();
  const [order, setOrder] = useState<AdminOrderDetail | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!orderId) return;
    if (!isAuthenticated || user?.role !== "admin") {
      router.push("/login");
      return;
    }
    fetchOrder();
  }, [isAuthenticated, user, router, orderId]);

  const fetchOrder = async () => {
    try {
      const res = await api.get<AdminOrderDetail>(`/admin/orders/${orderId}`);
      setOrder(res.data);
    } catch {
      toast.error("Failed to load order details");
      router.push("/admin/orders");
    } finally {
      setLoading(false);
    }
  };

  const updateStatus = async (newStatus: string) => {
    try {
      await api.patch(`/admin/orders/${orderId}`, { status: newStatus });
      toast.success(`Order updated to ${newStatus}`);
      fetchOrder();
    } catch {
      toast.error("Failed to update order");
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
      </div>
    );
  }

  if (!order) return null;

  const paymentColor =
    order.payment_status === "paid"
      ? "bg-green-50 text-green-700 border-green-200"
      : order.payment_status === "failed"
      ? "bg-red-50 text-red-700 border-red-200"
      : "bg-yellow-50 text-yellow-700 border-yellow-200";

  return (
    <div>
      {/* Header */}
      <div className="flex items-center gap-4 mb-6">
        <button onClick={() => router.push("/admin/orders")} className="p-2 rounded-lg hover:bg-cream transition-colors">
          <ArrowLeft size={20} />
        </button>
        <div className="flex-1">
          <h1 className="font-playfair text-2xl font-bold">Order #{order.id.slice(0, 8)}</h1>
          <p className="text-sm text-text-secondary">{formatDate(order.created_at)}</p>
        </div>
        <Badge variant={statusVariant[order.status]} className="text-sm px-3 py-1">
          {order.status}
        </Badge>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column — Order Info */}
        <div className="lg:col-span-2 space-y-6">
          {/* Items */}
          <div className="bg-white rounded-2xl shadow-sm p-6">
            <h2 className="font-semibold text-lg mb-4 flex items-center gap-2">
              <Package size={18} className="text-primary" /> Order Items
            </h2>
            <div className="divide-y divide-border">
              {order.items.map((item) => (
                <div key={item.id} className="flex items-center justify-between py-3">
                  <div>
                    <p className="font-medium">{item.product_name || item.product_id}</p>
                    {item.customization && (
                      <p className="text-xs text-text-secondary mt-0.5">
                        {Object.entries(item.customization)
                          .filter(([, v]) => v)
                          .map(([k, v]) => `${k}: ${v}`)
                          .join(", ")}
                      </p>
                    )}
                  </div>
                  <div className="text-right">
                    <p className="font-medium">{formatPrice(item.unit_price * item.quantity)}</p>
                    <p className="text-xs text-text-secondary">
                      {item.quantity} × {formatPrice(item.unit_price)}
                    </p>
                  </div>
                </div>
              ))}
            </div>

            {/* Totals */}
            <div className="mt-4 pt-4 border-t border-border space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-text-secondary">Subtotal</span>
                <span>{formatPrice(order.subtotal)}</span>
              </div>
              {order.discount > 0 && (
                <div className="flex justify-between text-green-600">
                  <span>Discount {order.coupon_code && `(${order.coupon_code})`}</span>
                  <span>-{formatPrice(order.discount)}</span>
                </div>
              )}
              {order.delivery_fee > 0 && (
                <div className="flex justify-between">
                  <span className="text-text-secondary">Delivery Fee</span>
                  <span>{formatPrice(order.delivery_fee)}</span>
                </div>
              )}
              <div className="flex justify-between font-bold text-base pt-2 border-t border-border">
                <span>Total</span>
                <span>{formatPrice(order.total)}</span>
              </div>
            </div>
          </div>

          {/* Payment & Delivery Info */}
          <div className="bg-white rounded-2xl shadow-sm p-6">
            <h2 className="font-semibold text-lg mb-4 flex items-center gap-2">
              <CreditCard size={18} className="text-primary" /> Payment & Delivery
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <p className="text-xs text-text-secondary mb-1">Payment Method</p>
                <p className="font-medium capitalize">{order.payment_method || "—"}</p>
              </div>
              <div>
                <p className="text-xs text-text-secondary mb-1">Payment Status</p>
                <span className={`inline-block text-xs px-2.5 py-1 rounded-full font-medium border ${paymentColor}`}>
                  {order.payment_status}
                </span>
              </div>
              <div>
                <p className="text-xs text-text-secondary mb-1">Order Type</p>
                <p className="font-medium capitalize flex items-center gap-1">
                  {order.order_type === "delivery" ? <Truck size={14} /> : <Package size={14} />}
                  {order.order_type}
                </p>
              </div>
              <div>
                <p className="text-xs text-text-secondary mb-1">Time Slot</p>
                <p className="font-medium flex items-center gap-1">
                  <Clock size={14} />
                  {order.time_slot || "Not specified"}
                </p>
              </div>
              {order.razorpay_order_id && (
                <div>
                  <p className="text-xs text-text-secondary mb-1">Razorpay Order</p>
                  <p className="font-mono text-xs">{order.razorpay_order_id}</p>
                </div>
              )}
              {order.razorpay_payment_id && (
                <div>
                  <p className="text-xs text-text-secondary mb-1">Razorpay Payment</p>
                  <p className="font-mono text-xs">{order.razorpay_payment_id}</p>
                </div>
              )}
              {order.estimated_delivery_minutes && (
                <div>
                  <p className="text-xs text-text-secondary mb-1">Est. Delivery</p>
                  <p className="font-medium">{order.estimated_delivery_minutes} min</p>
                </div>
              )}
              {order.special_note && (
                <div className="col-span-2">
                  <p className="text-xs text-text-secondary mb-1">Special Note</p>
                  <p className="text-sm bg-cream rounded-lg px-3 py-2">{order.special_note}</p>
                </div>
              )}
            </div>
          </div>

          {/* Address */}
          {order.address && (
            <div className="bg-white rounded-2xl shadow-sm p-6">
              <h2 className="font-semibold text-lg mb-4 flex items-center gap-2">
                <MapPin size={18} className="text-primary" /> Delivery Address
              </h2>
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-medium">
                    {order.address.label}
                  </span>
                  {order.address.recipient_name && (
                    <span className="text-sm font-medium">{order.address.recipient_name}</span>
                  )}
                </div>
                {order.address.house_number && (
                  <p className="text-sm text-text-secondary">
                    {order.address.house_number}
                    {order.address.street ? `, ${order.address.street}` : ""}
                  </p>
                )}
                <p className="text-sm">{order.address.full_address}</p>
                {order.address.landmark && (
                  <p className="text-sm text-text-secondary">Landmark: {order.address.landmark}</p>
                )}
                <p className="text-sm text-text-secondary">
                  {order.address.city}
                  {order.address.state ? `, ${order.address.state}` : ""} — {order.address.pincode}
                </p>
                {order.address.recipient_phone && (
                  <p className="text-sm flex items-center gap-1">
                    <Phone size={14} className="text-primary" />
                    <a href={`tel:${order.address.recipient_phone}`} className="text-primary hover:underline">
                      {order.address.recipient_phone}
                    </a>
                  </p>
                )}
                {order.address.lat && order.address.lng && (
                  <a
                    href={`https://maps.google.com/?q=${order.address.lat},${order.address.lng}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1 text-sm text-primary hover:underline mt-1"
                  >
                    <ExternalLink size={14} /> Open in Google Maps
                  </a>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Right Column — Customer & Actions */}
        <div className="space-y-6">
          {/* Customer Info */}
          {order.customer && (
            <div className="bg-white rounded-2xl shadow-sm p-6">
              <h2 className="font-semibold text-lg mb-4 flex items-center gap-2">
                <User size={18} className="text-primary" /> Customer
              </h2>
              <div className="flex items-center gap-3 mb-4">
                {order.customer.profile_image_url ? (
                  <img
                    src={order.customer.profile_image_url}
                    alt={order.customer.name || "Customer"}
                    className="w-12 h-12 rounded-full object-cover"
                  />
                ) : (
                  <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-lg">
                    {(order.customer.name || "?").charAt(0).toUpperCase()}
                  </div>
                )}
                <div>
                  <p className="font-semibold">{order.customer.name || "Unknown"}</p>
                  <p className="text-xs text-text-secondary">ID: {order.customer.id.slice(0, 8)}</p>
                </div>
              </div>
              <div className="space-y-3">
                {order.customer.email && (
                  <div className="flex items-center gap-2 text-sm">
                    <Mail size={14} className="text-text-secondary shrink-0" />
                    <a href={`mailto:${order.customer.email}`} className="text-primary hover:underline truncate">
                      {order.customer.email}
                    </a>
                  </div>
                )}
                {order.customer.phone && (
                  <div className="flex items-center gap-2 text-sm">
                    <Phone size={14} className="text-text-secondary shrink-0" />
                    <a href={`tel:${order.customer.phone}`} className="text-primary hover:underline">
                      {order.customer.phone}
                    </a>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Status Actions */}
          <div className="bg-white rounded-2xl shadow-sm p-6">
            <h2 className="font-semibold text-lg mb-4">Update Status</h2>
            {NEXT_STATUS[order.status] ? (
              <div className="space-y-3">
                <Button
                  onClick={() => updateStatus(NEXT_STATUS[order.status])}
                  className="w-full"
                >
                  Mark as {NEXT_STATUS[order.status]}
                </Button>
                {(order.status === "placed" || order.status === "accepted") && (
                  <Button
                    variant="ghost"
                    onClick={() => updateStatus("cancelled")}
                    className="w-full text-red-600 hover:bg-red-50"
                  >
                    Cancel Order
                  </Button>
                )}
              </div>
            ) : (
              <p className="text-sm text-text-secondary">
                This order is <span className="font-medium">{order.status}</span> — no further actions.
              </p>
            )}
          </div>

          {/* Order Timeline */}
          <div className="bg-white rounded-2xl shadow-sm p-6">
            <h2 className="font-semibold text-lg mb-4 flex items-center gap-2">
              <Clock size={18} className="text-primary" /> Timeline
            </h2>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-text-secondary">Created</span>
                <span>{formatDate(order.created_at)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-text-secondary">Last Updated</span>
                <span>{formatDate(order.updated_at)}</span>
              </div>
              {order.scheduled_date && (
                <div className="flex justify-between">
                  <span className="text-text-secondary">Scheduled</span>
                  <span>{order.scheduled_date}</span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
