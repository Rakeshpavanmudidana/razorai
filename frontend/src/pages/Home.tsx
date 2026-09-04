import AIChat from "../components/AIChat";

function Home() {
  const scrollToAI = () => {
    document.getElementById("ai-shopping")?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
  };

  const goToProducts = () => {
    window.location.href = "/products";
  };

  return (
    <div className="home-page">
      <section className="hero">
        <div className="hero-content">
          <p className="hero-label">AI-POWERED SHOPPING</p>

          <h1>
            Shop smarter with
            <span> RazorAI</span>
          </h1>

          <p className="hero-description">
            Discover products, get intelligent recommendations, and complete
            your purchase with a simple and secure checkout experience.
          </p>

          <div className="hero-buttons">
            <button className="primary-button" onClick={goToProducts}>
              Browse Products
            </button>

            <button className="secondary-button" onClick={scrollToAI}>
              Explore RazorAI
            </button>
          </div>
        </div>
      </section>

      {/* AI SHOPPING ASSISTANT */}
      <section id="ai-shopping" className="ai-shopping-section">
        <div className="section-heading">
          <p>YOUR AI SHOPPING ASSISTANT</p>
          <h2>Shop with RazorAI</h2>
        </div>

        <AIChat />
      </section>

      <section className="features">
        <div className="section-heading">
          <p>WHY RAZORAI</p>
          <h2>A better way to shop</h2>
        </div>

        <div className="feature-grid">
          <div className="feature-card">
            <div className="feature-icon">🤖</div>

            <h3>Smart Recommendations</h3>

            <p>Get product recommendations based on what you're looking for.</p>
          </div>

          <div className="feature-card">
            <div className="feature-icon">⚡</div>

            <h3>Fast Checkout</h3>

            <p>Move from product selection to payment quickly and easily.</p>
          </div>

          <div className="feature-card">
            <div className="feature-icon">🔒</div>

            <h3>Secure Payments</h3>

            <p>Complete your purchase securely using Razorpay Checkout.</p>
          </div>
        </div>
      </section>
    </div>
  );
}

export default Home;
