import { Link } from "react-router-dom";

function Navbar() {
  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to="/" className="logo">
          RazorAI
        </Link>

        <div className="nav-links">
          <Link to="/" className="nav-link">
            Home
          </Link>

          <Link to="/products" className="nav-link">
            Products
          </Link>

          <Link to="/cart" className="nav-link">
            🛒 Cart
          </Link>

          <Link to="/orders" className="nav-link">
            📦 Orders
          </Link>
        </div>
      </div>
    </nav>
  );
}

export default Navbar;
