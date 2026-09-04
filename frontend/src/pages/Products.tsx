import { useEffect, useState } from "react";
import axios from "axios";

interface Category {
  id: number;
  name: string;
}

interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  stock: number;
  imageUrl: string;
  keywords: string;
  category: Category;
}

function Products() {
  const userId = 1;

  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [addingProduct, setAddingProduct] = useState<number | null>(null);

  useEffect(() => {
    fetchProducts();
  }, []);

  const fetchProducts = async () => {
    try {
      const response = await axios.get<Product[]>(
        `${import.meta.env.VITE_API_URL}/api/products`,
      );

      setProducts(response.data);
    } catch (error) {
      console.error("Error fetching products:", error);
    } finally {
      setLoading(false);
    }
  };

  const addToCart = async (productId: number) => {
    try {
      setAddingProduct(productId);

      await axios.post(
        `${import.meta.env.VITE_API_URL}/api/cart/${userId}/items`,
        null,
        {
          params: {
            productId: productId,
            quantity: 1,
          },
        },
      );

      alert("Product added to cart!");
    } catch (error) {
      console.error("Error adding product to cart:", error);

      if (axios.isAxiosError(error)) {
        alert(
          error.response?.data?.message || "Unable to add product to cart.",
        );
      } else {
        alert("Unable to add product to cart.");
      }
    } finally {
      setAddingProduct(null);
    }
  };

  if (loading) {
    return (
      <div className="page-container">
        <h2>Products</h2>
        <p>Loading products...</p>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <p className="section-label">OUR STORE</p>

          <h1>Products</h1>

          <p>Discover products available in the RazorAI store.</p>
        </div>
      </div>

      {products.length === 0 ? (
        <div className="empty-state">
          <h2>No products found</h2>

          <p>Add products to your catalog to see them here.</p>
        </div>
      ) : (
        <div className="product-grid">
          {products.map((product) => (
            <div className="product-card" key={product.id}>
              <div className="product-image">
                {product.imageUrl ? (
                  <img src={product.imageUrl} alt={product.name} />
                ) : (
                  <span>🛍️</span>
                )}
              </div>

              <div className="product-content">
                <span className="product-category">
                  {product.category?.name || "Product"}
                </span>

                <h3>{product.name}</h3>

                <p className="product-description">{product.description}</p>

                <div className="product-bottom">
                  <div>
                    <p className="product-price">
                      ₹{product.price.toLocaleString("en-IN")}
                    </p>

                    <p className="stock">
                      {product.stock > 0
                        ? `${product.stock} available`
                        : "Out of stock"}
                    </p>
                  </div>

                  <button
                    className="add-cart-button"
                    disabled={
                      product.stock === 0 || addingProduct === product.id
                    }
                    onClick={() => addToCart(product.id)}
                  >
                    {addingProduct === product.id
                      ? "Adding..."
                      : product.stock > 0
                        ? "Add to Cart"
                        : "Out of Stock"}
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default Products;
