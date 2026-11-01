/** @type {import('tailwindcss').Config} */

/**
 * Tailwind configuration.
 *
 * Tailwind is a utility-first CSS framework: instead of writing a `.hero-title` class in a
 * separate stylesheet, you compose `text-4xl font-bold tracking-tight` directly on the element.
 * Two concrete benefits on a project like this:
 *   1. No dead CSS. The build scans the files listed in `content` and ships only the classes
 *      actually used, so the production stylesheet is a few kilobytes.
 *   2. No naming debates and no cascade bugs - the styles live with the markup they style.
 *
 * The `theme.extend` block below is the important part: defining brand colours here means a
 * rebrand is one file, not a find-and-replace across forty components.
 */
export default {
  // If a file is missing from this list, its classes get stripped from the production build
  // and the page renders unstyled. That is the single most common Tailwind setup mistake.
  content: ['./index.html', './src/**/*.{js,jsx}'],

  theme: {
    extend: {
      colors: {
        // Deep navy: trust and reliability, which is what a moving company sells.
        brand: {
          50: '#eef4ff',
          100: '#d9e5ff',
          200: '#bcd2ff',
          300: '#8eb5ff',
          400: '#598cff',
          500: '#3363ff',
          600: '#1b40f5',
          700: '#152fe1',
          800: '#1829b6',
          900: '#1a2a8f',
          950: '#141b57',
        },
        // Amber accent for calls to action - high contrast against navy, and it reads as
        // "truck / logistics / high-visibility" rather than generic SaaS.
        accent: {
          50: '#fffbeb',
          100: '#fef3c7',
          200: '#fde68a',
          300: '#fcd34d',
          400: '#fbbf24',
          500: '#f59e0b',
          600: '#d97706',
          700: '#b45309',
          800: '#92400e',
          900: '#78350f',
        },
      },
      fontFamily: {
        // Loaded from Google Fonts in index.html. The fallback chain matters: if the webfont
        // is slow or blocked, text still renders in a sensible system font instead of a
        // serif default that looks broken.
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
      boxShadow: {
        card: '0 1px 2px rgba(16, 24, 40, 0.04), 0 8px 24px rgba(16, 24, 40, 0.06)',
        lift: '0 12px 32px rgba(16, 24, 40, 0.12)',
      },
      keyframes: {
        'fade-up': {
          '0%': { opacity: '0', transform: 'translateY(12px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
      animation: {
        'fade-up': 'fade-up 0.5s ease-out both',
      },
    },
  },

  plugins: [],
};
