"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Image from "next/image";
import Link from "next/link";
import { Star, Minus, Plus, Heart, ShoppingBag, Bell, BellOff } from "lucide-react";
import Navbar from "@/components/layout/Navbar";
import Footer from "@/components/layout/Footer";
import Button from "@/components/ui/Button";
import Badge from "@/components/ui/Badge";
import { useCartStore } from "@/store/cartStore";
import { useAuthStore } from "@/store/authStore";
import { formatPrice } from "@/lib/utils";
import api from "@/lib/api";
import toast from "react-hot-toast";
import type { Product, Review } from "@/types";

export default function ProductDetailClient() {
  const params = useParams();
  const router = useRouter();
  const addItem = useCartStore((s) => s.addItem);
  const { isAuthenticated } = useAuthStore();

  // [...id] catch-all: real product id is read from the actual browser URL at
  // runtime (not baked in at build time), since this route is statically
  // exported as a single shell for any /menu/* deep link.
  const rawId = params.id;
  const id = Array.isArray(rawId) ? rawId[0] : (rawId ?? "");

  const [product, setProduct] = useState<Product | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);
  const [selectedSize, setSelectedSize] = useState("");
  const [selectedFlavor, setSelectedFlavor] = useState("");
  const [cakeMessage, setCakeMessage] = useState("");
  const [isEggless, setIsEggless] = useState(false);
  const [activeImage, setActiveImage] = useState(0);

  // Waitlist state
  const [onWaitlist, setOnWaitlist] = useState(false);
  const [waitlistLoading, setWaitlistLoading] = useState(false);

  const isOutOfStock = product ? product.stock_count <= 0 || !product.is_available : false;

  useEffect(() => {
    if (!id) return;
    async function load() {
      try {
        const [prodRes, revRes] = await Promise.all([
          api.get<Product>(`/products/${id}`),
          api.get<Review[]>(`/products/${id}/reviews`),
        ]);
        setProduct(prodRes.data);
        setReviews(revRes.data);

        const sizes = prodRes.data.variants.filter((v) => v.variant_type === "size");
        const flavors = prodRes.data.variants.filter((v) => v.variant_type === "flavor");
        if (sizes.length > 0) setSelectedSize(sizes[0].value);
        if (flavors.length > 0) setSelectedFlavor(flavors[0].value);
      } catch {
        // Product not found
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id]);

  // Check if user is already on waitlist for this product
  useEffect(() => {
    if (!isAuthenticated || !product || !isOutOfStock) return;
    api.get("/waitlist")
      .then((res) => {
        const items = res.data as Array<{ product_id: string }>;
        setOnWaitlist(items.some((w) => w.product_id === product.id));
      })
      .catch(() => {});
  }, [isAuthenticated, product, isOutOfStock]);

  const toggleWaitlist = async () => {
    if (!isAuthenticated) {
      toast.error("Please login to join the waitlist");
      return;
    }
    if (!product) return;
    setWaitlistLoading(true);
    try {
      if (onWaitlist) {
        await api.delete(`/waitlist/${product.id}`);
        setOnWaitlist(false);
        toast.success("Removed from waitlist");
      } else {
        await api.post(`/waitlist/${product.id}`);
        setOnWaitlist(true);
        toast.success("You'll be notified when this is back in stock!");
      }
    } catch {
      toast.error("Failed to update waitlist");
    } finally {
      setWaitlistLoading(false);
    }
  };

  if (loading) {
    return (
      <>
        <Navbar />
        <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div className="aspect-square bg-border/30 rounded-2xl animate-pulse" />
            <div className="space-y-4">
              <div className="h-8 bg-border/30 rounded w-3/4 animate-pulse" />
              <div className="h-6 bg-border/30 rounded w-1/4 animate-pulse" />
              <div className="h-20 bg-border/30 rounded animate-pulse" />
            </div>
          </div>
        </main>
        <Footer />
      </>
    );
  }

  if (!product) {
    return (
      <>
        <Navbar />
        <main className="flex-1 flex flex-col items-center justify-center py-20">
          <div className="text-5xl mb-4">😕</div>
          <h1 className="font-playfair text-2xl font-bold mb-2">Product not found</h1>
          <Link href="/menu"><Button>Back to Menu</Button></Link>
        </main>
        <Footer />
      </>
    );
  }

  const sizes = product.variants.filter((v) => v.variant_type === "size");
  const flavors = product.variants.filter((v) => v.variant_type === "flavor");

  const sizeExtra = sizes.find((s) => s.value === selectedSize)?.extra_price || 0;
  const flavorExtra = flavors.find((f) => f.value === selectedFlavor)?.extra_price || 0;
  const totalPrice = (product.price + sizeExtra + flavorExtra) * quantity;

  const handleAddToCart = () => {
    if (isOutOfStock) return;
    addItem(product, quantity, {
      size: selectedSize || undefined,
      flavor: selectedFlavor || undefined,
      eggless: isEggless || undefined,
      cake_message: cakeMessage || undefined,
    });
  };

  const handleBuyNow = () => {
    if (isOutOfStock) return;
    handleAddToCart();
    router.push("/cart");
  };

  return (
    <>
      <Navbar />
      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <nav className="text-sm text-text-secondary mb-6">
          <Link href="/" className="hover:text-primary">Home</Link>
          <span className="mx-2">/</span>
          <Link href="/menu" className="hover:text-primary">Menu</Link>
          <span className="mx-2">/</span>
          <span className="text-text-primary">{product.name}</span>
        </nav>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {/* Image Section */}
          <div>
            <div className="bg-white rounded-2xl overflow-hidden shadow-sm relative">
              <div className="relative aspect-square">
                {product.images?.[activeImage] && (product.images[activeImage].startsWith("http") || product.images[activeImage].startsWith("/")) ? (
                  <Image
                    src={product.images[activeImage]}
                    alt={product.name}
                    fill
                    className="object-cover"
                    priority
                  />
                ) : (
                  <div className="w-full h-full bg-cream flex items-center justify-center text-8xl">🧁</div>
                )}
              </div>
              {/* Out of stock overlay */}
              {isOutOfStock && (
                <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
                  <span className="bg-red-600 text-white text-lg font-bold px-6 py-2 rounded-full">
                    OUT OF STOCK
                  </span>
                </div>
              )}
            </div>
            {product.images.filter(img => img.startsWith("http") || img.startsWith("/")).length > 1 && (
              <div className="flex gap-2 mt-3">
                {product.images.filter(img => img.startsWith("http") || img.startsWith("/")).map((img, i) => (
                  <button
                    key={i}
                    onClick={() => setActiveImage(i)}
                    className={`w-16 h-16 rounded-xl overflow-hidden border-2 ${i === activeImage ? "border-primary" : "border-transparent"}`}
                  >
                    <Image src={img} alt="" width={64} height={64} className="object-cover w-full h-full" />
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Details Section */}
          <div className="space-y-6">
            <div>
              <div className="flex items-center gap-2 mb-2 flex-wrap">
                {isOutOfStock && <Badge variant="error">Out of Stock</Badge>}
                {product.is_eggless_available && <Badge variant="success">Eggless Available</Badge>}
                {product.customizable && <Badge variant="info">Customizable</Badge>}
                {!isOutOfStock && product.stock_count <= 5 && product.stock_count > 0 && (
                  <Badge variant="warning">Only {product.stock_count} left!</Badge>
                )}
              </div>
              <h1 className="font-playfair text-3xl font-bold text-text-primary">{product.name}</h1>
              <div className="flex items-center gap-2 mt-2">
                <Star size={18} className="fill-secondary text-secondary" />
                <span className="font-semibold">{product.rating.toFixed(1)}</span>
                <span className="text-text-secondary text-sm">({reviews.length} reviews)</span>
              </div>
            </div>

            <p className="text-2xl font-bold text-primary">{formatPrice(totalPrice)}</p>
            <p className="text-text-secondary leading-relaxed">{product.description}</p>

            {/* Waitlist UI for out-of-stock products */}
            {isOutOfStock && (
              <div className="bg-amber-50 border border-amber-200 rounded-2xl p-6 space-y-3">
                <h3 className="font-semibold text-amber-900">This item is currently unavailable</h3>
                <p className="text-sm text-amber-700">
                  Get notified when it&apos;s back in stock.
                </p>
                <Button
                  onClick={toggleWaitlist}
                  disabled={waitlistLoading}
                  variant={onWaitlist ? "outline" : "primary"}
                  className="gap-2"
                >
                  {onWaitlist ? <BellOff size={16} /> : <Bell size={16} />}
                  {waitlistLoading ? "Updating…" : onWaitlist ? "Leave Waitlist" : "Notify Me When Available"}
                </Button>
              </div>
            )}

            {/* Customization — only when in stock */}
            {!isOutOfStock && (sizes.length > 0 || flavors.length > 0 || product.is_eggless_available) && (
              <div className="bg-cream rounded-2xl p-6 space-y-4">
                <h3 className="font-semibold">Customize Your Order</h3>

                {sizes.length > 0 && (
                  <div>
                    <label className="text-sm font-medium text-text-primary mb-2 block">Size</label>
                    <div className="flex flex-wrap gap-2">
                      {sizes.map((s) => (
                        <button
                          key={s.id}
                          onClick={() => setSelectedSize(s.value)}
                          className={`px-4 py-2 rounded-full text-sm font-medium border transition-all ${
                            selectedSize === s.value
                              ? "bg-primary text-white border-primary"
                              : "bg-white border-border text-text-secondary hover:border-primary"
                          }`}
                        >
                          {s.value} {s.extra_price > 0 && `(+${formatPrice(s.extra_price)})`}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {flavors.length > 0 && (
                  <div>
                    <label className="text-sm font-medium text-text-primary mb-2 block">Flavor</label>
                    <div className="flex flex-wrap gap-2">
                      {flavors.map((f) => (
                        <button
                          key={f.id}
                          onClick={() => setSelectedFlavor(f.value)}
                          className={`px-4 py-2 rounded-full text-sm font-medium border transition-all ${
                            selectedFlavor === f.value
                              ? "bg-primary text-white border-primary"
                              : "bg-white border-border text-text-secondary hover:border-primary"
                          }`}
                        >
                          {f.value} {f.extra_price > 0 && `(+${formatPrice(f.extra_price)})`}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {product.is_eggless_available && (
                  <label className="flex items-center gap-2 text-sm cursor-pointer">
                    <input
                      type="checkbox"
                      className="rounded"
                      checked={isEggless}
                      onChange={(e) => setIsEggless(e.target.checked)}
                    />
                    Make it Eggless
                  </label>
                )}

                {product.customizable && (
                  <div>
                    <label className="text-sm font-medium text-text-primary mb-2 block">
                      Message on Cake (optional)
                    </label>
                    <input
                      type="text"
                      placeholder="e.g., Happy Birthday!"
                      value={cakeMessage}
                      onChange={(e) => setCakeMessage(e.target.value)}
                      maxLength={50}
                      className="w-full border border-border rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                    />
                  </div>
                )}
              </div>
            )}

            {/* Quantity + Actions — only when in stock */}
            {!isOutOfStock && (
              <div className="flex items-center gap-4">
                <div className="flex items-center gap-2 bg-white border border-border rounded-full">
                  <button
                    onClick={() => setQuantity(Math.max(1, quantity - 1))}
                    className="w-10 h-10 flex items-center justify-center hover:bg-cream rounded-full"
                  >
                    <Minus size={16} />
                  </button>
                  <span className="w-8 text-center font-semibold">{quantity}</span>
                  <button
                    onClick={() => setQuantity(Math.min(product.stock_count, quantity + 1))}
                    className="w-10 h-10 flex items-center justify-center hover:bg-cream rounded-full"
                  >
                    <Plus size={16} />
                  </button>
                </div>

                <Button onClick={handleAddToCart} variant="outline" className="flex-1 gap-2">
                  <ShoppingBag size={18} /> Add to Cart
                </Button>
                <Button onClick={handleBuyNow} className="flex-1">
                  Buy Now
                </Button>
              </div>
            )}
          </div>
        </div>

        {/* Reviews Section */}
        {reviews.length > 0 && (
          <section className="mt-12">
            <h2 className="font-playfair text-2xl font-bold mb-6">Customer Reviews</h2>
            <div className="space-y-4">
              {reviews.map((review) => (
                <div key={review.id} className="bg-white rounded-2xl p-4 shadow-sm">
                  <div className="flex items-center gap-2 mb-2">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <Star
                        key={star}
                        size={16}
                        className={star <= review.rating ? "fill-secondary text-secondary" : "text-border"}
                      />
                    ))}
                  </div>
                  {review.comment && <p className="text-text-secondary text-sm">{review.comment}</p>}
                </div>
              ))}
            </div>
          </section>
        )}
      </main>
      <Footer />
    </>
  );
}
