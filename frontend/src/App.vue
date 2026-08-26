
<template>
  <div class="app-container">
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup>
</script>

<style>
@font-face {
  font-family: 'Ogg Medium';
  src: url('https://dcym8fthxf5uu.cloudfront.net/fonts/247a073c-29f5-4a89-aa3a-741020f346fc/OggText-Medium.woff2') format('woff2');
  font-display: swap;
}

:root {
  --font-display: 'Ogg Medium', Georgia, 'Times New Roman', serif;
  --ink: #111411;
  --paper: #fdf1e1;
  --river: #5ea9b1;
  --forest: #153f3a;
  color: var(--paper);
  background: #0b1110;
  font-family: Inter, Satoshi, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  letter-spacing: 0;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  min-height: 100%;
  scroll-behavior: smooth;
  background: #0b1110;
}

body {
  min-height: 100%;
  overflow-x: clip;
  background: #0b1110;
  font-family: inherit;
}

.app-container {
  min-height: 100vh;
  background-color: transparent;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.24s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

button,
input,
textarea,
select {
  font: inherit;
  letter-spacing: 0;
}

button:focus-visible,
a:focus-visible,
input:focus-visible {
  outline: 2px solid #fdf1e1;
  outline-offset: 3px;
}

@media (prefers-reduced-motion: reduce) {
  html { scroll-behavior: auto; }
  *, *::before, *::after { animation-duration: 0.01ms !important; transition-duration: 0.01ms !important; }
}
</style>
