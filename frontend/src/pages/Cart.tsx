import { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

interface Category {
  id: number;
  name: string;
}

interface Product {
  id: number;
  name: string;
  price: number;
  imageUrl?: string;
  category?: Category;
}

interface CartItem {
  id: number;
  quantity: number;
  product: Product;
}

interface Cart {
  id: number;
  userId: number;
  items: CartItem[];
}

function CartPage() {
  const userId = 1;

  const navigate = useNavigate();

  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchCart = async () => {
    try {
      const response = await axios.get<Cart>(
        `${import.meta.env.VITE_API_URL}/api/cart/${userId}`,
      );

      setCart(response.data);
    } catch (error) {
      console.error("Error fetching cart:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCart();
  }, []);

  const updateQuantity = async (cartItemId: number, quantity: number) => {
    if (quantity < 1) {
      return;
    }

    try {
      await axios.put(
        `${import.meta.env.VITE_API_URL}/api/cart/items/${cartItemId}`,
        null,
        {
          params: {
            quantity: quantity,
          },
        },
      );

      fetchCart();
    } catch (error) {
      console.error("Error updating cart:", error);

      alert("Unable to update quantity.");
    }
  };

  const removeItem = async (cartItemId: number) => {
    try {
      await axios.delete(
        `${import.meta.env.VITE_API_URL}/api/cart/items/${cartItemId}`,
      );

      fetchCart();
    } catch (error) {
      console.error("Error removing cart item:", error);

      alert("Unable to remove item.");
    }
  };

  const calculateTotal = () => {
    if (!cart) {
      return 0;
    }

    return cart.items.reduce(
      (total, item) => total + item.product.price * item.quantity,
      0,
    );
  };

  const createOrder = async () => {
    try {
      const response = await axios.post(
        `${import.meta.env.VITE_API_URL}/api/orders/${userId}`,
      );

      const orderId = response.data.id;

      alert(`Order #${orderId} created successfully!`);

      navigate("/orders");
    } catch (error) {
      console.error("Error creating order:", error);

      if (axios.isAxiosError(error)) {
        alert(error.response?.data?.message || "Unable to create order.");
      } else {
        alert("Unable to create order.");
      }
    }
  };

  if (loading) {
    return (
      <div className="page-container">
        <h2>Your Cart</h2>
        <p>Loading cart...</p>
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="page-container">
        <div className="empty-state">
          <div
            style={{
              fontSize: "60px",
              marginBottom: "20px",
            }}
          >
            🛒
          </div>

          <h2>Your cart is empty</h2>

          <p>Add some products to your cart before checking out.</p>

          <button
            className="primary-button"
            style={{ marginTop: "25px" }}
            onClick={() => navigate("/products")}
          >
            Browse Products
          </button>
        </div>
      </div>
    );
  }

  const total = calculateTotal();

  return (
    <div className="page-container">
      <div className="page-header">
        <p className="section-label">SHOPPING CART</p>

        <h1>Your Cart</h1>

        <p>Review your items before checkout.</p>
      </div>

      <div className="cart-layout">
        <div className="cart-items">
          {cart.items.map((item) => (
            <div className="cart-item" key={item.id}>
              <div className="cart-product-image">
                {item.product.imageUrl ? (
                  <img src={item.product.imageUrl} alt={item.product.name} />
                ) : (
                  <span>🛍️</span>
                )}
              </div>

              <div className="cart-product-info">
                <span className="product-category">
                  {item.product.category?.name || "Product"}
                </span>

                <h3>{item.product.name}</h3>

                <p className="cart-price">
                  ₹{item.product.price.toLocaleString("en-IN")}
                </p>

                <button
                  className="remove-button"
                  onClick={() => removeItem(item.id)}
                >
                  Remove
                </button>
              </div>

              <div className="quantity-control">
                <button
                  onClick={() => updateQuantity(item.id, item.quantity - 1)}
                  disabled={item.quantity <= 1}
                >
                  −
                </button>

                <span>{item.quantity}</span>

                <button
                  onClick={() => updateQuantity(item.id, item.quantity + 1)}
                >
                  +
                </button>
              </div>

              <div className="cart-item-total">
                ₹{(item.product.price * item.quantity).toLocaleString("en-IN")}
              </div>
            </div>
          ))}
        </div>

        <div className="order-summary">
          <h2>Order Summary</h2>

          <div className="summary-row">
            <span>Items</span>
            <span>
              {cart.items.reduce((total, item) => total + item.quantity, 0)}
            </span>
          </div>

          <div className="summary-row">
            <span>Subtotal</span>

            <span>₹{total.toLocaleString("en-IN")}</span>
          </div>

          <div className="summary-divider" />

          <div className="summary-total">
            <span>Total</span>

            <span>₹{total.toLocaleString("en-IN")}</span>
          </div>

          <button className="checkout-button" onClick={createOrder}>
            Proceed to Checkout
          </button>
        </div>
      </div>
    </div>
  );
}

export default CartPage;
