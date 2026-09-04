import { useEffect, useState } from "react";
import axios from "axios";
import PaymentButton from "../components/PaymentButton";

interface Product {
  id: number;
  name: string;
  price: number;
  imageUrl?: string;
}

interface OrderItem {
  id: number;
  quantity: number;
  price: number;
  product: Product;
}

interface Order {
  id: number;
  userId: number;
  totalAmount: number;
  status: string;
  deliveryStatus: string;
  createdAt: string;
  items: OrderItem[];
}

type OrderFilter = "ALL" | "PAID" | "DELIVERED" | "CANCELLED";

function Orders() {
  const userId = 1;

  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [cancellingOrder, setCancellingOrder] = useState<number | null>(null);

  const [trackingOrder, setTrackingOrder] = useState<Order | null>(null);

  const [activeFilter, setActiveFilter] = useState<OrderFilter>("ALL");

  const fetchOrders = async () => {
    try {
      const response = await axios.get<Order[]>(
        `${import.meta.env.VITE_API_URL}/api/orders/user/${userId}`,
      );

      setOrders(response.data);
    } catch (error) {
      console.error("Error fetching orders:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  // ============================================================
  // CANCEL PENDING ORDER
  // ============================================================

  const cancelOrder = async (orderId: number) => {
    const confirmed = window.confirm(
      `Are you sure you want to cancel Order #${orderId}?`,
    );

    if (!confirmed) {
      return;
    }

    try {
      setCancellingOrder(orderId);

      await axios.put(
        `${import.meta.env.VITE_API_URL}/api/orders/${orderId}/cancel`,
      );

      alert(`Order #${orderId} cancelled successfully.`);

      await fetchOrders();
    } catch (error) {
      console.error("Error cancelling order:", error);

      if (axios.isAxiosError(error)) {
        alert(error.response?.data?.message || "Unable to cancel order.");
      } else {
        alert("Unable to cancel order.");
      }
    } finally {
      setCancellingOrder(null);
    }
  };

  // ============================================================
  // CANCEL PAID ORDER + REFUND
  // ============================================================

  const cancelPaidOrder = async (orderId: number) => {
    const confirmed = window.confirm(
      `Are you sure you want to cancel Order #${orderId}? Your payment will be refunded.`,
    );

    if (!confirmed) {
      return;
    }

    try {
      setCancellingOrder(orderId);

      await axios.put(
        `${import.meta.env.VITE_API_URL}/api/orders/${orderId}/cancel-paid`,
      );

      alert(`Order #${orderId} cancelled and refund initiated successfully.`);

      await fetchOrders();
    } catch (error) {
      console.error("Error cancelling paid order:", error);

      if (axios.isAxiosError(error)) {
        alert(
          error.response?.data?.message ||
            "Unable to cancel order or process refund.",
        );
      } else {
        alert("Unable to cancel order or process refund.");
      }
    } finally {
      setCancellingOrder(null);
    }
  };

  // ============================================================
  // TRACK ORDER
  // ============================================================

  const openTracking = (order: Order) => {
    setTrackingOrder(order);
  };

  const closeTracking = () => {
    setTrackingOrder(null);
  };

  // ============================================================
  // FILTER ORDERS
  // ============================================================

  const filteredOrders = orders.filter((order) => {
    if (activeFilter === "ALL") {
      return order.status !== "CANCELLED";
    }

    if (activeFilter === "PAID") {
      return order.status === "PAID";
    }

    if (activeFilter === "DELIVERED") {
      return order.status === "PAID" && order.deliveryStatus === "DELIVERED";
    }

    if (activeFilter === "CANCELLED") {
      return order.status === "CANCELLED";
    }

    return true;
  });

  // ============================================================
  // TRACKING HELPERS
  // ============================================================

  const deliverySteps = [
    {
      status: "PREPARING",
      title: "Preparing",
      description: "Your order is being prepared",
    },
    {
      status: "SHIPPED",
      title: "Shipped",
      description: "Your package has been shipped",
    },
    {
      status: "OUT_FOR_DELIVERY",
      title: "Out for Delivery",
      description: "Your package is on the way",
    },
    {
      status: "DELIVERED",
      title: "Delivered",
      description: "Package delivered successfully",
    },
  ];

  const getDeliveryStepIndex = (deliveryStatus: string) => {
    const index = deliverySteps.findIndex(
      (step) => step.status === deliveryStatus,
    );

    return index === -1 ? 0 : index;
  };

  // ============================================================
  // LOADING
  // ============================================================

  if (loading) {
    return (
      <div className="page-container">
        <h2>My Orders</h2>
        <p>Loading orders...</p>
      </div>
    );
  }

  return (
    <div className="page-container">
      {/* ======================================================
          PAGE HEADER
      ====================================================== */}

      <div className="page-header">
        <p className="section-label">ORDER HISTORY</p>

        <h1>My Orders</h1>

        <p>View your orders, track deliveries, and manage your purchases.</p>
      </div>

      {/* ======================================================
          ORDER FILTERS
      ====================================================== */}

      <div className="order-filters">
        <button
          className={
            activeFilter === "ALL" ? "order-filter active" : "order-filter"
          }
          onClick={() => setActiveFilter("ALL")}
        >
          All Orders
        </button>

        <button
          className={
            activeFilter === "PAID" ? "order-filter active" : "order-filter"
          }
          onClick={() => setActiveFilter("PAID")}
        >
          ✓ Payment Successful
        </button>

        <button
          className={
            activeFilter === "DELIVERED"
              ? "order-filter active"
              : "order-filter"
          }
          onClick={() => setActiveFilter("DELIVERED")}
        >
          📦 Delivered
        </button>

        <button
          className={
            activeFilter === "CANCELLED"
              ? "order-filter active cancelled-filter"
              : "order-filter"
          }
          onClick={() => setActiveFilter("CANCELLED")}
        >
          ✕ Cancelled
        </button>
      </div>

      {/* ======================================================
          NO ORDERS FOR FILTER
      ====================================================== */}

      {filteredOrders.length === 0 ? (
        <div className="empty-state">
          <div
            style={{
              fontSize: "60px",
              marginBottom: "20px",
            }}
          >
            📦
          </div>

          <h2>
            {activeFilter === "CANCELLED"
              ? "No cancelled orders"
              : activeFilter === "DELIVERED"
                ? "No delivered orders"
                : activeFilter === "PAID"
                  ? "No paid orders"
                  : "No orders yet"}
          </h2>

          <p>
            {activeFilter === "CANCELLED"
              ? "Cancelled orders will appear here."
              : activeFilter === "DELIVERED"
                ? "Delivered orders will appear here."
                : activeFilter === "PAID"
                  ? "Successfully paid orders will appear here."
                  : "Your orders will appear here after checkout."}
          </p>
        </div>
      ) : (
        <div className="orders-list">
          {filteredOrders.map((order) => (
            <div className="order-card" key={order.id}>
              {/* ==================================================
                  ORDER HEADER
              ================================================== */}

              <div className="order-card-header">
                <div>
                  <span className="section-label">ORDER</span>

                  <h2>#{order.id}</h2>
                </div>

                <div className="order-status">
                  <span
                    className={
                      order.status === "CANCELLED"
                        ? "status-cancelled"
                        : order.status === "PAID" &&
                            order.deliveryStatus === "DELIVERED"
                          ? "status-delivered"
                          : order.status === "PAID"
                            ? "status-paid"
                            : "status-pending"
                    }
                  >
                    {order.status === "CANCELLED"
                      ? "CANCELLED"
                      : order.status === "PAID" &&
                          order.deliveryStatus === "DELIVERED"
                        ? "DELIVERED"
                        : order.status === "PAID"
                          ? "PAYMENT SUCCESSFUL"
                          : order.status}
                  </span>
                </div>
              </div>

              {/* ==================================================
                  ORDER DATE
              ================================================== */}

              <div className="order-date">
                {new Date(order.createdAt).toLocaleString("en-IN")}
              </div>

              {/* ==================================================
                  ORDER ITEMS
              ================================================== */}

              <div className="order-items">
                {order.items.map((item) => (
                  <div className="order-item" key={item.id}>
                    <div className="order-product-image">
                      {item.product.imageUrl ? (
                        <img
                          src={item.product.imageUrl}
                          alt={item.product.name}
                        />
                      ) : (
                        <span>🛍️</span>
                      )}
                    </div>

                    <div className="order-product-info">
                      <h3>{item.product.name}</h3>

                      <p>Quantity: {item.quantity}</p>

                      <p>₹{item.price.toLocaleString("en-IN")} each</p>
                    </div>

                    <div className="order-item-total">
                      ₹{(item.price * item.quantity).toLocaleString("en-IN")}
                    </div>
                  </div>
                ))}
              </div>

              {/* ==================================================
                  ORDER FOOTER
              ================================================== */}

              <div className="order-footer">
                <div className="order-total">
                  <span>Total</span>

                  <strong>₹{order.totalAmount.toLocaleString("en-IN")}</strong>
                </div>

                {/* ================================================
                    PENDING
                ================================================ */}

                {order.status === "PENDING" && (
                  <div className="order-actions">
                    <PaymentButton
                      orderId={order.id}
                      onPaymentSuccess={fetchOrders}
                    />

                    <button
                      className="cancel-order-button"
                      onClick={() => cancelOrder(order.id)}
                      disabled={cancellingOrder === order.id}
                    >
                      {cancellingOrder === order.id
                        ? "Cancelling..."
                        : "Cancel Order"}
                    </button>
                  </div>
                )}

                {/* ================================================
                    PAID
                ================================================ */}

                {order.status === "PAID" &&
                  order.deliveryStatus !== "DELIVERED" && (
                    <div className="order-actions">
                      <div className="payment-success">
                        ✓ Payment Successful
                      </div>

                      <button
                        className="track-order-button"
                        onClick={() => openTracking(order)}
                      >
                        📦 Track Order
                      </button>

                      <button
                        className="cancel-paid-order-button"
                        onClick={() => cancelPaidOrder(order.id)}
                        disabled={cancellingOrder === order.id}
                      >
                        {cancellingOrder === order.id
                          ? "Processing..."
                          : "❌ Cancel Order"}
                      </button>
                    </div>
                  )}

                {/* ================================================
                    DELIVERED
                ================================================ */}

                {order.status === "PAID" &&
                  order.deliveryStatus === "DELIVERED" && (
                    <div className="order-actions">
                      <button
                        className="track-order-button"
                        onClick={() => openTracking(order)}
                      >
                        📦 Track Order
                      </button>

                      <button className="return-order-button">
                        ↩ Return Order
                      </button>
                    </div>
                  )}

                {/* ================================================
                    CANCELLED
                ================================================ */}

                {order.status === "CANCELLED" && (
                  <div className="order-cancelled">✕ Order Cancelled</div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ========================================================
          TRACKING MODAL
      ======================================================== */}

      {trackingOrder && (
        <div className="tracking-overlay" onClick={closeTracking}>
          <div
            className="tracking-modal"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="tracking-header">
              <div>
                <p className="section-label">ORDER TRACKING</p>

                <h2>Order #{trackingOrder.id}</h2>
              </div>

              <button className="tracking-close" onClick={closeTracking}>
                ×
              </button>
            </div>

            <div className="tracking-product">
              <div className="tracking-product-icon">📦</div>

              <div>
                <strong>
                  {trackingOrder.items[0]?.product.name || "Your Order"}
                </strong>

                <p>
                  Total: ₹{trackingOrder.totalAmount.toLocaleString("en-IN")}
                </p>

                <p>
                  Status:{" "}
                  <strong>
                    {trackingOrder.deliveryStatus.replaceAll("_", " ")}
                  </strong>
                </p>
              </div>
            </div>

            <div className="tracking-timeline">
              {/* ORDER PLACED */}

              <div className="tracking-step completed">
                <div className="tracking-dot">✓</div>

                <div className="tracking-content">
                  <strong>Order Placed</strong>

                  <span>Your order has been placed</span>
                </div>
              </div>

              <div className="tracking-line completed-line" />

              {/* PAYMENT */}

              <div className="tracking-step completed">
                <div className="tracking-dot">✓</div>

                <div className="tracking-content">
                  <strong>Payment Confirmed</strong>

                  <span>Your payment was successfully received</span>
                </div>
              </div>

              {/* DELIVERY STEPS */}

              {deliverySteps.map((step, index) => {
                const currentIndex = getDeliveryStepIndex(
                  trackingOrder.deliveryStatus,
                );

                const isCompleted = index < currentIndex;
                const isActive = index === currentIndex;

                return (
                  <div key={step.status}>
                    <div
                      className={`tracking-step ${
                        isCompleted ? "completed" : isActive ? "active" : ""
                      }`}
                    >
                      <div className="tracking-dot">
                        {isCompleted ? "✓" : isActive ? "•" : "○"}
                      </div>

                      <div className="tracking-content">
                        <strong>{step.title}</strong>

                        <span>{step.description}</span>
                      </div>
                    </div>

                    {index < deliverySteps.length - 1 && (
                      <div
                        className={
                          index < currentIndex
                            ? "tracking-line completed-line"
                            : "tracking-line"
                        }
                      />
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Orders;
