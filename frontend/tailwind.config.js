/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        nupi: {
          primary: '#003893',
          'primary-dark': '#002466',
          'primary-light': '#1A5CC8',
          accent: '#DC143C',
          'accent-light': '#FF4D6A',
          success: '#00C853',
          warning: '#FFB300',
          danger: '#FF3D00',
          bg: '#F5F7FA',
          card: '#FFFFFF',
          text: '#1A1A2E',
          'text-secondary': '#6B7280',
          'text-muted': '#9CA3AF',
          border: '#E5E7EB',
          'dark-bg': '#0F1729',
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace']
      },
      animation: {
        'slide-up': 'slideUp 0.3s ease-out',
        'slide-down': 'slideDown 0.3s ease-out',
        'fade-in': 'fadeIn 0.2s ease-out',
        'bounce-in': 'bounceIn 0.5s ease-out',
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'spin-slow': 'spin 2s linear infinite',
      },
      keyframes: {
        slideUp: {
          '0%': { transform: 'translateY(100%)' },
          '100%': { transform: 'translateY(0)' }
        },
        slideDown: {
          '0%': { transform: 'translateY(-100%)' },
          '100%': { transform: 'translateY(0)' }
        },
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' }
        },
        bounceIn: {
          '0%': { transform: 'scale(0.3)', opacity: '0' },
          '50%': { transform: 'scale(1.05)' },
          '70%': { transform: 'scale(0.9)' },
          '100%': { transform: 'scale(1)', opacity: '1' }
        }
      },
      boxShadow: {
        'card': '0 2px 8px rgba(0, 56, 147, 0.08)',
        'card-hover': '0 4px 16px rgba(0, 56, 147, 0.15)',
        'bottom-nav': '0 -2px 12px rgba(0, 0, 0, 0.08)',
        'btn': '0 4px 12px rgba(0, 56, 147, 0.3)',
      },
      borderRadius: {
        'xl': '1rem',
        '2xl': '1.5rem',
        '3xl': '2rem',
      }
    }
  },
  plugins: []
};
