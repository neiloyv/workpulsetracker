/** @type {import('tailwindcss').Config} */
export default {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        display: ["Sora", "system-ui", "sans-serif"],
        body: ["Manrope", "system-ui", "sans-serif"]
      },
      colors: {
        brand: {
          50: "#f2f1ff",
          100: "#e6e4ff",
          200: "#cdc9ff",
          300: "#aaa2ff",
          400: "#8b7bff",
          500: "#7458ff",
          600: "#6338f2",
          700: "#5329d1",
          800: "#4423a8",
          900: "#391f85",
          950: "#221157"
        }
      },
      boxShadow: {
        glow: "0 0 0 1px rgba(124, 92, 255, 0.15), 0 20px 60px -20px rgba(99, 56, 242, 0.45)",
        card: "0 1px 0 rgba(255,255,255,0.04) inset, 0 20px 40px -24px rgba(0,0,0,0.45)"
      },
      backgroundImage: {
        "grid-fade":
          "radial-gradient(circle at 20% 20%, rgba(124,92,255,0.18), transparent 45%), radial-gradient(circle at 80% 0%, rgba(56,189,248,0.12), transparent 40%)"
      },
      keyframes: {
        "fade-in": {
          "0%": { opacity: "0", transform: "translateY(6px)" },
          "100%": { opacity: "1", transform: "translateY(0)" }
        },
        "fade-in-scale": {
          "0%": { opacity: "0", transform: "scale(0.97)" },
          "100%": { opacity: "1", transform: "scale(1)" }
        }
      },
      animation: {
        "fade-in": "fade-in 0.5s ease-out both",
        "fade-in-scale": "fade-in-scale 0.2s ease-out both"
      }
    }
  },
  plugins: []
};
