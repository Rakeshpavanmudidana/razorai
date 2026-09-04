import axios from "axios";
import { useState } from "react";

interface PaymentButtonProps {
  orderId: number;
  onPaymentSuccess: () => void;
}

interface PaymentOrderResponse {
  orderId: number;
  amount: number;
  currency: string;
  razorpayOrderId: string;
  razorpayKey: string;
}

declare global {
  interface Window {
    Razorpay: any;
  }
}

function PaymentButton({ orderId, onPaymentSuccess }: PaymentButtonProps) {
  const [loading, setLoading] = useState(false);

  const loadRazorpayScript = (): Promise<boolean> => {
    return new Promise((resolve) => {
      const existingScript = document.getElementById("razorpay-checkout");

      if (existingScript) {
        resolve(true);
        return;
      }

      const script = document.createElement("script");

      script.id = "razorpay-checkout";

      // Razorpay's official checkout script.
      // This is NOT your backend URL.
      script.src = "https://checkout.razorpay.com/v1/checkout.js";

      script.onload = () => resolve(true);

      script.onerror = () => resolve(false);

      document.body.appendChild(script);
    });
  };

  const handlePayment = async () => {
    try {
      setLoading(true);

      const scriptLoaded = await loadRazorpayScript();

      if (!scriptLoaded) {
        alert("Razorpay Checkout failed to load.");
        return;
      }

      // ========================================================
      // CREATE RAZORPAY ORDER
      // ========================================================
      //
      // BEFORE:
      // http://localhost:8080/api/payment/create/...
      //
      // NOW:
      // VITE_API_URL/api/payment/create/...
      // ========================================================

      const response = await axios.post<PaymentOrderResponse>(
        `${import.meta.env.VITE_API_URL}/api/payment/create/${orderId}`,
      );

      const paymentData = response.data;

      // ========================================================
      // RAZORPAY OPTIONS
      // ========================================================

      const options = {
        key: paymentData.razorpayKey,

        amount: paymentData.amount * 100,

        currency: paymentData.currency,

        name: "RazorAI",

        description: `RazorAI Order #${orderId}`,

        order_id: paymentData.razorpayOrderId,

        // ======================================================
        // PAYMENT SUCCESS
        // ======================================================

        handler: async function (response: any) {
          console.log("Payment successful:", response);

          try {
            // ==================================================
            // VERIFY PAYMENT
            // ==================================================
            //
            // BEFORE:
            // http://localhost:8080/api/payment/verify
            //
            // NOW:
            // VITE_API_URL/api/payment/verify
            // ==================================================

            const verifyResponse = await axios.post(
              `${import.meta.env.VITE_API_URL}/api/payment/verify`,
              null,
              {
                params: {
                  orderId: orderId,

                  razorpayOrderId: response.razorpay_order_id,

                  razorpayPaymentId: response.razorpay_payment_id,

                  razorpaySignature: response.razorpay_signature,
                },
              },
            );

            console.log("Verification response:", verifyResponse.data);

            if (verifyResponse.data.success) {
              alert("Payment verified successfully!");

              // Tell App.tsx to refresh the order.
              onPaymentSuccess();
            } else {
              alert("Payment verification failed.");
            }
          } catch (error) {
            console.error("Verification error:", error);

            alert("Payment verification failed.");
          }
        },

        // ======================================================
        // PREFILL
        // ======================================================

        prefill: {
          name: "RazorAI Customer",
        },

        // ======================================================
        // RAZORPAY THEME
        // ======================================================

        theme: {
          color: "#3399cc",
        },
      };

      // ========================================================
      // OPEN RAZORPAY
      // ========================================================

      const razorpay = new window.Razorpay(options);

      razorpay.open();
    } catch (error) {
      console.error("FULL PAYMENT ERROR:", error);

      if (axios.isAxiosError(error)) {
        console.error("Status:", error.response?.status);

        console.error("Response:", error.response?.data);

        alert(
          `Payment API Error: ${
            error.response?.data?.message || error.message
          }`,
        );
      } else {
        alert("Payment failed. Check the browser console.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <button onClick={handlePayment} disabled={loading}>
      {loading ? "Processing..." : "Pay Now"}
    </button>
  );
}

export default PaymentButton;
