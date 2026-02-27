<script setup lang="ts">
import { NConfigProvider, NGlobalStyle } from 'naive-ui'
import type { GlobalThemeOverrides } from 'naive-ui'
import NavBar from '@/components/custom/layout/NavBar.vue'
import Footer from '@/components/custom/layout/BottomFooter.vue'
import ChatWidget from '../../views/client/user/chat/index.vue'

// ==================== DOCOMO THEME CONFIGURATION ====================
// Theme colors based on Docomo's brand identity
const themeOverrides: GlobalThemeOverrides = {
  common: {
    // Primary colors - Docomo signature pink/magenta
    primaryColor: '#e6007e', // Docomo pink
    primaryColorHover: '#c4006b', // Darker pink for hover
    primaryColorPressed: '#a30058', // Even darker for pressed
    primaryColorSuppl: '#ff3399', // Lighter pink for supplementary

    // Info colors
    infoColor: '#0099ff',
    infoColorHover: '#0077cc',

    // Success colors
    successColor: '#00a65a',
    successColorHover: '#008d4c',

    // Warning colors
    warningColor: '#ff9900',
    warningColorHover: '#e68a00',

    // Error colors
    errorColor: '#ff4444',
    errorColorHover: '#cc0000',

    // Text colors
    textColorBase: '#333333',
    textColor1: '#212121',
    textColor2: '#555555',
    textColor3: '#888888',

    // Border & Background
    borderColor: '#e8e8e8',
    baseColor: '#ffffff',

    // Typography
    fontFamily: '"Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    fontFamilyMono: '"SF Mono", Monaco, "Cascadia Code", "Roboto Mono", monospace',

    // Border radius
    borderRadius: '8px',
    borderRadiusSmall: '4px',

    // Box shadow
    boxShadow1: '0 4px 12px rgba(0, 0, 0, 0.05)',
    boxShadow2: '0 8px 24px rgba(0, 0, 0, 0.08)',
    boxShadow3: '0 16px 32px rgba(0, 0, 0, 0.12)',
  },

  // Button styling
  Button: {
    textColor: '#ffffff',
    textColorHover: '#ffffff',
    textColorPressed: '#ffffff',
    textColorFocus: '#ffffff',
    textColorDisabled: '#cccccc',

    border: 'none',
    borderHover: 'none',
    borderPressed: 'none',
    borderFocus: 'none',

    color: '#e6007e',
    colorHover: '#c4006b',
    colorPressed: '#a30058',
    colorFocus: '#c4006b',
    colorDisabled: '#f5f5f5',

    rippleColor: '#ffffff',

    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '500',
    padding: '8px 20px',
    paddingSmall: '6px 16px',
    paddingTiny: '4px 12px',
    paddingLarge: '12px 24px',
  },

  // Card styling
  Card: {
    color: 'rgba(255, 255, 255, 0.95)',
    borderColor: 'rgba(230, 0, 126, 0.1)',
    borderRadius: '16px',
    titleTextColor: '#212121',
    titleFontWeight: '600',
    titleFontSize: '18px',
    padding: '20px',
    boxShadow: '0 4px 20px rgba(0, 0, 0, 0.03)',
  },

  // Input styling
  Input: {
    color: '#ffffff',
    border: '1px solid #e8e8e8',
    borderHover: '1px solid #e6007e',
    borderFocus: '1px solid #e6007e',
    borderRadius: '8px',
    textColor: '#212121',
    placeholderColor: '#bbbbbb',
    caretColor: '#e6007e',
    boxShadowFocus: '0 0 0 2px rgba(230, 0, 126, 0.1)',
  },

  // Select styling
  Select: {
    menuColor: '#ffffff',
    menuBoxShadow: '0 8px 24px rgba(0, 0, 0, 0.08)',
    menuBorderRadius: '8px',
    optionColorActive: 'rgba(230, 0, 126, 0.05)',
    optionColorHover: 'rgba(230, 0, 126, 0.03)',
    optionTextColor: '#212121',
    optionTextColorActive: '#e6007e',
  },

  // Table styling
  DataTable: {
    thColor: '#fafafa',
    thTextColor: '#555555',
    tdColor: '#ffffff',
    tdTextColor: '#333333',
    borderColor: '#f0f0f0',
    borderRadius: '8px',
    thFontWeight: '600',
    thFontSize: '13px',
    tdFontSize: '14px',
  },

  // Tag styling
  Tag: {
    color: 'rgba(230, 0, 126, 0.1)',
    textColor: '#e6007e',
    border: 'none',
    borderRadius: '4px',
    heightMedium: '24px',
    fontSize: '12px',
    padding: '0 8px',
  },

  // Modal styling
  Dialog: {
    color: '#ffffff',
    borderRadius: '20px',
    titleTextColor: '#212121',
    contentTextColor: '#555555',
    boxShadow: '0 24px 48px rgba(0, 0, 0, 0.15)',
  },

  // Menu styling (for NavBar)
  Menu: {
    itemTextColor: '#ffffff',
    itemTextColorHover: '#ffffff',
    itemTextColorActive: '#ffffff',
    itemTextColorChildActive: '#e6007e',
    itemColorActive: 'rgba(255, 255, 255, 0.15)',
    itemColorHover: 'rgba(255, 255, 255, 0.1)',
    borderRadius: '20px',
    fontSize: '15px',
    fontWeight: '500',
    itemHeight: '48px',
  },

  // Divider
  Divider: {
    color: 'rgba(230, 0, 126, 0.1)',
  },

  // Switch
  Switch: {
    railColorActive: '#e6007e',
    railColor: '#dddddd',
    buttonColor: '#ffffff',
    boxShadowFocus: '0 0 0 2px rgba(230, 0, 126, 0.2)',
  },

  // Alert
  Alert: {
    color: '#ffffff',
    border: 'none',
    borderRadius: '12px',
    titleTextColor: '#212121',
    contentTextColor: '#555555',
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.03)',
  },

  // Tabs
  Tabs: {
    tabTextColor: '#888888',
    tabTextColorActive: '#e6007e',
    tabTextColorHover: '#555555',
    tabBorderColor: 'transparent',
    tabBorderColorActive: '#e6007e',
    barColor: '#e6007e',
    fontWeightActive: '600',
  },

  // Pagination
  Pagination: {
    itemColor: 'transparent',
    itemColorHover: 'rgba(230, 0, 126, 0.05)',
    itemColorActive: '#e6007e',
    itemTextColorActive: '#ffffff',
    itemBorder: 'none',
    itemBorderHover: 'none',
    itemBorderActive: 'none',
    itemBorderRadius: '6px',
  },
}

// Font loading optimization
const fontLink = document.createElement('link')
fontLink.rel = 'stylesheet'
fontLink.href = 'https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap'
document.head.appendChild(fontLink)
</script>

<template>
  <NConfigProvider :theme-overrides="themeOverrides">
    <NGlobalStyle />

    <div class="docomo-app">
      <!-- Navigation -->
      <NavBar />

      <!-- Main Content Area -->
      <main class="docomo-main">
        <div class="docomo-container">
          <!-- Glassmorphism Card Container -->
          <div class="docomo-glass-card">
            <!-- Dynamic Page Content -->
            <RouterView />
          </div>
        </div>
      </main>

      <!-- Footer -->
      <Footer />

      <!-- Chat Widget -->
      <ChatWidget />
    </div>
  </NConfigProvider>
</template>

<style scoped>
/* ==================== DOCOMO DESIGN SYSTEM ==================== */

/* 1. Base Layout */
.docomo-app {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  position: relative;

  /* Premium background with subtle gradient */
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

/* 2. Main Content Area */
.docomo-main {
  flex: 1;
  padding: 24px 20px;
  position: relative;
  z-index: 1;
}

/* 3. Container */
.docomo-container {
  max-width: 1440px;
  margin: 0 auto;
  width: 100%;
  height: 100%;
}

/* 4. Premium Glass Card - Docomo Style */
.docomo-glass-card {
  /* Base white with perfect opacity for readability */
  background-color: rgba(255, 255, 255, 0.92);

  /* Large border radius for modern look */
  border-radius: 32px;

  /* Elegant shadow for depth */
  box-shadow:
    0 20px 40px -10px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.3) inset;

  /* Glassmorphism effect */
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);

  /* Subtle border */
  border: 1px solid rgba(255, 255, 255, 0.5);

  /* Inner spacing */
  padding: 24px 32px;

  /* Minimum height */
  min-height: calc(100vh - 180px);

  /* Smooth transitions */
  transition: all 0.3s ease;
}

/* Hover effect for interactive feel */
.docomo-glass-card:hover {
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  background-color: rgba(255, 255, 255, 0.95);
}

/* 5. Docomo Accent Elements */
.docomo-glass-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #e6007e, #ff99cc);
  border-radius: 32px 32px 0 0;
  opacity: 0.8;
}

/* 6. Responsive Design */
@media (max-width: 1200px) {
  .docomo-main {
    padding: 20px 16px;
  }

  .docomo-glass-card {
    padding: 20px 24px;
    border-radius: 24px;
  }
}

@media (max-width: 768px) {
  .docomo-main {
    padding: 16px 12px;
  }

  .docomo-glass-card {
    padding: 16px 20px;
    border-radius: 20px;
    min-height: calc(100vh - 160px);
  }

  .docomo-glass-card::before {
    height: 3px;
  }
}

@media (max-width: 480px) {
  .docomo-main {
    padding: 12px 8px;
  }

  .docomo-glass-card {
    padding: 12px 16px;
    border-radius: 16px;
  }
}

/* 7. Animation for page transitions */
.docomo-glass-card {
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 8. Custom scrollbar for Docomo style */
.docomo-glass-card ::v-deep(.n-scrollbar) {
  scrollbar-width: thin;
  scrollbar-color: #e6007e rgba(230, 0, 126, 0.1);
}

.docomo-glass-card ::v-deep(.n-scrollbar::-webkit-scrollbar) {
  width: 6px;
  height: 6px;
}

.docomo-glass-card ::v-deep(.n-scrollbar::-webkit-scrollbar-track) {
  background: rgba(230, 0, 126, 0.05);
  border-radius: 10px;
}

.docomo-glass-card ::v-deep(.n-scrollbar::-webkit-scrollbar-thumb) {
  background: rgba(230, 0, 126, 0.3);
  border-radius: 10px;
}

.docomo-glass-card ::v-deep(.n-scrollbar::-webkit-scrollbar-thumb:hover) {
  background: #e6007e;
}
</style>

<!-- Global styles to ensure consistency -->
<style>
/* Import Inter font with fallbacks */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

/* Base styles */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  scroll-behavior: smooth;
}

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

/* Docomo brand color utility classes */
.docomo-text-primary {
  color: #e6007e !important;
}

.docomo-bg-primary {
  background-color: #e6007e !important;
}

.docomo-border-primary {
  border-color: #e6007e !important;
}

/* Smooth transitions for interactive elements */
button, a, .n-button, .n-card {
  transition: all 0.2s ease !important;
}

/* Loading state animation */
@keyframes docomo-pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.docomo-loading {
  animation: docomo-pulse 1.5s ease-in-out infinite;
}
</style>
