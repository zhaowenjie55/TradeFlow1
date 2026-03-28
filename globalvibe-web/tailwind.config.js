/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    './app/components/**/*.{js,vue,ts}',
    './app/layouts/**/*.vue',
    './app/pages/**/*.vue',
    './app/composables/**/*.{js,ts}',
    './app/plugins/**/*.{js,ts}',
    './app/app.vue',
    './app/error.vue',
  ],
  theme: {
    extend: {
      colors: {
        // GlobalVibe 主题色系统
        lime: {
          DEFAULT: '#d7ff5c',
          50: '#f5ffda',
          100: '#e9ffb5',
          200: '#d7ff5c',
          300: '#c4ff29',
          400: '#b1fc00',
          500: '#9de600',
          600: '#78b400',
          700: '#578300',
          800: '#3a5500',
          900: '#212c00',
        },
        cyan: {
          DEFAULT: '#7fe7ff',
          50: '#f0fbff',
          100: '#dff9ff',
          200: '#b9f2ff',
          300: '#7fe7ff',
          400: '#33d8f7',
          500: '#10c5e3',
          600: '#0098ba',
          700: '#006d87',
          800: '#004a5c',
          900: '#002a36',
        },
        violet: {
          DEFAULT: '#7c8bff',
          50: '#f0f1ff',
          100: '#e3e5ff',
          200: '#c9ceff',
          300: '#a8b2ff',
          400: '#7c8bff',
          500: '#555def',
          600: '#3d3fdd',
          700: '#2528a8',
          800: '#1a1b6e',
          900: '#0d0d35',
        },
        // 深色背景色
        surface: {
          1: '#11141B',
          2: '#14161D',
          3: '#151922',
          4: '#0F1117',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        'glow-lime': '0 0 20px rgba(215, 255, 92, 0.3)',
        'glow-cyan': '0 0 20px rgba(127, 231, 255, 0.3)',
        'glow-violet': '0 0 20px rgba(124, 139, 255, 0.3)',
      },
    },
  },
  plugins: [],
}
