import { useNavigate } from "react-router-dom";
import { useEffect, useRef, useState } from "react";

type Product = {
  id: number;
  name: string;
  price: number;
  description?: string;
  imageUrl?: string;
  stock?: number;
  keywords?: string;
};

type AIResponse = {
  message: string;
  action?: string;

  products?: Product[];

  recommendedProduct?: Product | null;

  showChooseButton?: boolean;
  showMoreInfoButton?: boolean;
  showResearchAgainButton?: boolean;
  showCompareButton?: boolean;
  showSelectButton?: boolean;

  showAddToCartButton?: boolean;
  showCheckoutButton?: boolean;
};

type ChatMessage = {
  id: number;
  sender: "user" | "ai";
  text: string;

  products?: Product[];

  recommendedProduct?: Product | null;

  showChooseButton?: boolean;
  showMoreInfoButton?: boolean;
  showResearchAgainButton?: boolean;
  showCompareButton?: boolean;
  showSelectButton?: boolean;

  showAddToCartButton?: boolean;
  showCheckoutButton?: boolean;

  action?: string;
};

const USER_ID = 1;

function AIChat() {
  const navigate = useNavigate();

  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 1,
      sender: "ai",
      text: "Hi! 👋 I'm RazorAI. What are you looking for today?",
    },
  ]);

  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);

  const [selectedProductIds, setSelectedProductIds] = useState<number[]>([]);

  const [expandedProductIds, setExpandedProductIds] = useState<number[]>([]);

  // ============================================================
  // CHAT SCROLL REFS
  // ============================================================

  // Main chat messages container.
  // Only this container will scroll.
  const chatMessagesRef = useRef<HTMLDivElement | null>(null);

  // Reference to the typing indicator.
  const typingRef = useRef<HTMLDivElement | null>(null);

  // ============================================================
  // SCROLL ONLY TO TYPING INDICATOR
  // ============================================================
  //
  // When any AI action starts:
  //
  // Button clicked
  //      ↓
  // AI starts loading
  //      ↓
  // Chat scrolls to typing indicator
  //      ↓
  // STOP
  //
  // When AI response arrives:
  // NO automatic scrolling happens.
  // ============================================================

  useEffect(() => {
    if (!loading || !typingRef.current || !chatMessagesRef.current) {
      return;
    }

    requestAnimationFrame(() => {
      const chatBox = chatMessagesRef.current;
      const typingElement = typingRef.current;

      if (!chatBox || !typingElement) {
        return;
      }

      const chatBoxRect = chatBox.getBoundingClientRect();

      const typingRect = typingElement.getBoundingClientRect();

      chatBox.scrollTo({
        top: chatBox.scrollTop + (typingRect.top - chatBoxRect.top) - 30,
        behavior: "smooth",
      });
    });
  }, [loading]);

  // ============================================================
  // SEND MESSAGE TO BACKEND
  // ============================================================

  const sendMessage = async (
    message: string = "",
    action: string = "",
    productIds: number[] = [],
  ) => {
    if (!message.trim() && !action) {
      return;
    }

    // ----------------------------------------------------------
    // ADD USER MESSAGE
    // ----------------------------------------------------------

    if (message.trim()) {
      const userMessage: ChatMessage = {
        id: Date.now(),
        sender: "user",
        text: message,
      };

      setMessages((previous) => [...previous, userMessage]);
    }

    setInput("");

    // Start loading.
    // This makes the typing indicator appear.
    setLoading(true);

    try {
      // ========================================================
      // UPDATED:
      // Backend URL now comes from Vite environment variable.
      // ========================================================

      const response = await fetch(
        `${import.meta.env.VITE_API_URL}/api/ai/chat?userId=${USER_ID}`,
        {
          method: "POST",

          headers: {
            "Content-Type": "application/json",
          },

          body: JSON.stringify({
            message: message,
            action: action,
            productIds: productIds,
          }),
        },
      );

      if (!response.ok) {
        throw new Error(`AI request failed: ${response.status}`);
      }

      const data: AIResponse = await response.json();

      // --------------------------------------------------------
      // ADD AI RESPONSE
      // --------------------------------------------------------

      const aiMessage: ChatMessage = {
        id: Date.now() + 1,

        sender: "ai",

        text: data.message || "I couldn't generate a response.",

        products: data.products || [],

        recommendedProduct: data.recommendedProduct || null,

        showChooseButton: data.showChooseButton,

        showMoreInfoButton: data.showMoreInfoButton,

        showResearchAgainButton: data.showResearchAgainButton,

        showCompareButton: data.showCompareButton,

        showSelectButton: data.showSelectButton,

        showAddToCartButton: data.showAddToCartButton,

        showCheckoutButton: data.showCheckoutButton,

        action: data.action,
      };

      setMessages((previous) => [...previous, aiMessage]);
    } catch (error) {
      console.error("AI CHAT ERROR:", error);

      setMessages((previous) => [
        ...previous,
        {
          id: Date.now() + 2,

          sender: "ai",

          text: "Sorry, I couldn't connect to the AI service. Please make sure the backend is running.",
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  // ============================================================
  // NORMAL SEND
  // ============================================================

  const handleSend = () => {
    if (!input.trim() || loading) {
      return;
    }

    sendMessage(input);
  };

  // ============================================================
  // ADD TO CART
  // ============================================================

  const handleAddToCart = (product: Product) => {
    sendMessage(`Add ${product.name} to my cart`, "ADD_TO_CART", [product.id]);
  };

  // ============================================================
  // CHOOSE ONE
  // ============================================================

  const handleChooseOne = () => {
    sendMessage("", "CHOOSE_ALL");
  };

  // ============================================================
  // CHOOSE FROM VISIBLE
  // ============================================================

  const handleChooseVisible = () => {
    sendMessage("", "CHOOSE_VISIBLE");
  };

  // ============================================================
  // MORE INFORMATION
  // ============================================================

  const handleMoreInfo = () => {
    sendMessage("", "MORE_INFO");
  };

  // ============================================================
  // RESEARCH AGAIN
  // ============================================================

  const handleResearchAgain = () => {
    sendMessage("", "RESEARCH_AGAIN");
  };

  // ============================================================
  // SHOW MORE
  // ============================================================

  const handleShowMore = () => {
    sendMessage("", "SHOW_MORE");
  };

  // ============================================================
  // SELECT PRODUCT
  // ============================================================

  const toggleProductSelection = (productId: number) => {
    setSelectedProductIds((previous) => {
      if (previous.includes(productId)) {
        return previous.filter((id) => id !== productId);
      }

      return [...previous, productId];
    });
  };

  // ============================================================
  // SELECT PRODUCTS
  // ============================================================

  const handleSelectProducts = () => {
    if (selectedProductIds.length === 0) {
      alert("Please select at least one product.");

      return;
    }

    sendMessage("", "SELECT_PRODUCTS", selectedProductIds);

    setSelectedProductIds([]);
  };

  // ============================================================
  // COMPARE
  // ============================================================

  const handleCompare = () => {
    sendMessage("", "COMPARE_SELECTED");
  };

  // ============================================================
  // CHECKOUT
  // ============================================================

  const handleCheckout = async () => {
    try {
      // ========================================================
      // UPDATED:
      // Backend URL now comes from Vite environment variable.
      // ========================================================

      const response = await fetch(
        `${import.meta.env.VITE_API_URL}/api/orders/${USER_ID}`,
        {
          method: "POST",
        },
      );

      if (!response.ok) {
        const errorData = await response.json().catch(() => null);

        throw new Error(
          errorData?.message || `Unable to create order: ${response.status}`,
        );
      }

      const order = await response.json();

      alert(`Order #${order.id} created successfully!`);

      navigate("/orders");
    } catch (error) {
      console.error("Error creating order:", error);

      if (error instanceof Error) {
        alert(error.message);
      } else {
        alert("Unable to create order.");
      }
    }
  };

  // ============================================================
  // FORMAT PRICE
  // ============================================================

  const formatPrice = (price: number) => {
    return `₹${Number(price).toLocaleString("en-IN")}`;
  };

  // ============================================================
  // MORE INFO
  // ============================================================

  const toggleMoreInfo = (productId: number) => {
    setExpandedProductIds((previous) =>
      previous.includes(productId)
        ? previous.filter((id) => id !== productId)
        : [...previous, productId],
    );
  };

  // ============================================================
  // PRODUCT CARD
  // ============================================================

  const renderProductCard = (product: Product) => {
    const selected = selectedProductIds.includes(product.id);

    const expanded = expandedProductIds.includes(product.id);

    return (
      <div
        className={selected ? "ai-product-card selected" : "ai-product-card"}
        key={product.id}
      >
        {/* PRODUCT IMAGE */}

        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="ai-product-image"
          />
        ) : (
          <div className="ai-product-placeholder">🛍️</div>
        )}

        {/* PRODUCT DETAILS */}

        <div className="ai-product-details">
          <h4>{product.name}</h4>

          <p className="ai-product-price">{formatPrice(product.price)}</p>

          {product.description && (
            <p className="ai-product-description">{product.description}</p>
          )}

          {product.stock !== undefined && (
            <p className="ai-product-stock">
              {product.stock > 0
                ? `${product.stock} available`
                : "Out of stock"}
            </p>
          )}

          {/* MORE INFO */}

          <button
            type="button"
            className="ai-product-more-info"
            onClick={() => toggleMoreInfo(product.id)}
          >
            {expanded ? "▲ Hide More Info" : "ℹ️ More Info"}
          </button>

          {/* EXPANDED INFORMATION */}

          {expanded && (
            <div className="ai-product-expanded-info">
              <div className="ai-info-row">
                <strong>Product ID:</strong>

                <span>#{product.id}</span>
              </div>

              <div className="ai-info-row">
                <strong>Price:</strong>

                <span>{formatPrice(product.price)}</span>
              </div>

              {product.stock !== undefined && (
                <div className="ai-info-row">
                  <strong>Availability:</strong>

                  <span>
                    {product.stock > 0
                      ? `${product.stock} units in stock`
                      : "Currently out of stock"}
                  </span>
                </div>
              )}

              {product.description && (
                <div className="ai-info-section">
                  <strong>Description</strong>

                  <p>{product.description}</p>
                </div>
              )}

              {product.keywords && (
                <div className="ai-info-section">
                  <strong>Features / Keywords</strong>

                  <p>{product.keywords}</p>
                </div>
              )}
            </div>
          )}

          {/* PRODUCT ACTIONS */}

          <div className="ai-product-actions">
            {/* SELECT */}

            <button
              className={
                selected ? "ai-select-button selected" : "ai-select-button"
              }
              onClick={() => toggleProductSelection(product.id)}
              disabled={loading}
            >
              {selected ? "✓ Selected" : "☐ Select"}
            </button>

            {/* ADD TO CART */}

            <button
              className="ai-add-button"
              onClick={() => handleAddToCart(product)}
              disabled={loading || product.stock === 0}
            >
              🛒 Add to Cart
            </button>
          </div>
        </div>
      </div>
    );
  };

  // ============================================================
  // MAIN UI
  // ============================================================

  return (
    <div className="ai-chat-wrapper">
      {/* HEADER */}

      <div className="ai-chat-header">
        <div>
          <h2>🤖 RazorAI</h2>

          <p>Your AI Shopping Assistant</p>
        </div>

        <div className="ai-online">
          <span></span>
          Online
        </div>
      </div>

      {/* ========================================================
          CHAT MESSAGES
          
          ONLY THIS BOX SCROLLS.
      ======================================================== */}

      <div className="ai-chat-messages" ref={chatMessagesRef}>
        {messages.map((message) => (
          <div
            key={message.id}
            className={
              message.sender === "user"
                ? "ai-message-row user-row"
                : "ai-message-row"
            }
          >
            {/* AI AVATAR */}

            {message.sender === "ai" && <div className="ai-avatar">🤖</div>}

            {/* MESSAGE */}

            <div
              className={
                message.sender === "user"
                  ? "ai-message user-message"
                  : "ai-message"
              }
            >
              <p>{message.text}</p>

              {/* RECOMMENDED PRODUCT */}

              {message.recommendedProduct &&
                !message.products?.some(
                  (product) => product.id === message.recommendedProduct?.id,
                ) && (
                  <div className="ai-recommended-product">
                    {renderProductCard(message.recommendedProduct)}
                  </div>
                )}

              {/* HORIZONTAL PRODUCT SLIDER */}

              {message.products && message.products.length > 0 && (
                <div className="ai-products-slider">
                  {message.products.map((product) =>
                    renderProductCard(product),
                  )}
                </div>
              )}

              {/* ACTION BUTTONS */}

              {message.sender === "ai" && (
                <div className="ai-action-buttons">
                  {/* CHOOSE ONE */}

                  {message.showChooseButton && (
                    <button
                      className="ai-action-button choose"
                      onClick={handleChooseOne}
                      disabled={loading}
                    >
                      ⭐ Choose One for Me
                    </button>
                  )}

                  {/* CHOOSE FROM VISIBLE */}

                  {message.showChooseButton &&
                    message.products &&
                    message.products.length > 1 && (
                      <button
                        className="ai-action-button"
                        onClick={handleChooseVisible}
                        disabled={loading}
                      >
                        🎯 Choose From These
                      </button>
                    )}

                  {/* MORE INFORMATION */}

                  {message.showMoreInfoButton && (
                    <button
                      className="ai-action-button"
                      onClick={handleMoreInfo}
                      disabled={loading}
                    >
                      ℹ️ More Information
                    </button>
                  )}

                  {/* RESEARCH AGAIN */}

                  {message.showResearchAgainButton && (
                    <button
                      className="ai-action-button research"
                      onClick={handleResearchAgain}
                      disabled={loading}
                    >
                      🔍 Research Again
                    </button>
                  )}

                  {/* COMPARE */}

                  {message.showCompareButton && (
                    <button
                      className="ai-action-button compare"
                      onClick={handleCompare}
                      disabled={loading}
                    >
                      ⚖️ Compare & Rate
                    </button>
                  )}

                  {/* SELECT */}

                  {message.showSelectButton && (
                    <button
                      className="ai-action-button select"
                      onClick={handleSelectProducts}
                      disabled={loading || selectedProductIds.length === 0}
                    >
                      ☑️ Select Products
                    </button>
                  )}

                  {/* SHOW MORE */}

                  {message.action === "SHOW_PRODUCTS" &&
                    message.products &&
                    message.products.length === 5 && (
                      <button
                        className="ai-action-button"
                        onClick={handleShowMore}
                        disabled={loading}
                      >
                        ➕ Show More Products
                      </button>
                    )}

                  {/* CART UPDATED */}

                  {message.action === "CART_UPDATED" && (
                    <button
                      className="ai-checkout-button"
                      onClick={handleCheckout}
                      disabled={loading}
                    >
                      💳 Proceed to Checkout
                    </button>
                  )}

                  {/* CHECKOUT */}

                  {message.showCheckoutButton && (
                    <button
                      className="ai-checkout-button"
                      onClick={handleCheckout}
                      disabled={loading}
                    >
                      💳 Checkout
                    </button>
                  )}
                </div>
              )}
            </div>
          </div>
        ))}

        {/* ========================================================
            TYPING INDICATOR
        ======================================================== */}

        {loading && (
          <div className="ai-message-row" ref={typingRef}>
            <div className="ai-avatar">🤖</div>

            <div className="ai-message ai-typing">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        )}
      </div>

      {/* ========================================================
          INPUT AREA
      ======================================================== */}

      <div className="ai-chat-input-area">
        <input
          type="text"
          placeholder="Ask me about products..."
          value={input}
          onChange={(event) => setInput(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              handleSend();
            }
          }}
          disabled={loading}
        />

        <button
          className="ai-send-button"
          onClick={handleSend}
          disabled={loading || !input.trim()}
        >
          ➤
        </button>
      </div>
    </div>
  );
}

export default AIChat;
