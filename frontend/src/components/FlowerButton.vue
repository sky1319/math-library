<template>
  <button class="flower-button" :class="`flower-button--${variant}`" :type="type" :disabled="disabled">
    <span class="flower-button__wrapper">
      <span class="flower-button__text"><slot /></span>
      <span v-for="index in 6" :key="index" class="flower-button__flower" :class="`flower-button__flower--${index}`" aria-hidden="true">
        <i class="flower-button__petal"></i>
        <i class="flower-button__petal flower-button__petal--two"></i>
        <i class="flower-button__petal flower-button__petal--three"></i>
        <i class="flower-button__petal flower-button__petal--four"></i>
      </span>
    </span>
  </button>
</template>

<script setup>
defineProps({
  type: { type: String, default: 'button' },
  variant: { type: String, default: 'regular' },
  disabled: { type: Boolean, default: false }
})
</script>

<style scoped>
.flower-button {
  --flower-width: 12em;
  --flower-height: 4em;
  --flower-wrapper-width: 8em;
  --flower-wrapper-height: 2em;
  --flower-text-size: 17px;
  width: var(--flower-width);
  height: var(--flower-height);
  font-size: 16px !important;
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  color: #000;
  background: transparent;
  cursor: pointer;
  isolation: isolate;
}

.flower-button--nav { --flower-width: 7em; --flower-height: 3em; --flower-wrapper-width: 5.6em; --flower-wrapper-height: 1.9em; --flower-text-size: 14px; }
.flower-button--icon { --flower-width: 3.4em; --flower-height: 3.4em; --flower-wrapper-width: 2.2em; --flower-wrapper-height: 2.2em; --flower-text-size: 19px; }
.flower-button--wide { --flower-width: 13.5em; --flower-height: 4em; --flower-wrapper-width: 10em; --flower-wrapper-height: 2.25em; --flower-text-size: 15px; }
.flower-button--tab { --flower-width: 7.2em; --flower-height: 3.4em; --flower-wrapper-width: 5.4em; --flower-wrapper-height: 2em; --flower-text-size: 13px; }
.flower-button--action { --flower-width: 7.2em; --flower-height: 3.4em; --flower-wrapper-width: 5.4em; --flower-wrapper-height: 2em; --flower-text-size: 14px; }
.flower-button--mini { --flower-width: 4.9em; --flower-height: 2.7em; --flower-wrapper-width: 4.55em; --flower-wrapper-height: 1.8em; --flower-text-size: 12px; }
.flower-button--demo { --flower-width: 100%; --flower-height: 3.2em; --flower-wrapper-width: 88%; --flower-wrapper-height: 2em; --flower-text-size: 11px; }

.flower-button__wrapper {
  position: relative;
  width: var(--flower-wrapper-width);
  height: var(--flower-wrapper-height);
  display: flex;
  align-items: center;
  justify-content: center;
}

.flower-button__text {
  position: relative;
  z-index: 2;
  max-width: 100%;
  padding: 4px 12px;
  overflow: hidden;
  border-radius: 4px;
  color: #000;
  background: rgba(255, 255, 255, .78);
  box-shadow: 0 5px 18px rgba(7, 97, 215, .1);
  font-size: var(--flower-text-size);
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: background .5s ease, box-shadow .5s ease, transform .5s ease;
}

.flower-button--nav .flower-button__text {
  padding-inline: 6px;
}

.flower-button--tab .flower-button__text {
  padding-inline: 4px;
}

.flower-button--mini .flower-button__text {
  box-sizing: border-box;
  width: 100%;
  padding-inline: 5px;
}

.flower-button__flower {
  position: absolute;
  display: grid;
  grid-template-columns: 1em 1em;
  transition: grid-template-columns .8s ease;
}

.flower-button__flower--1 { top: -12px; left: -13px; transform: rotate(5deg); }
.flower-button__flower--2 { bottom: -5px; left: 8px; transform: rotate(35deg); }
.flower-button__flower--3 { bottom: -15px; left: 50%; transform: translateX(-50%) rotate(0deg); }
.flower-button__flower--4 { top: -14px; left: 48%; transform: translateX(-50%) rotate(15deg); }
.flower-button__flower--5 { right: 11px; top: -3px; transform: rotate(25deg); }
.flower-button__flower--6 { right: -15px; bottom: -15px; transform: rotate(30deg); }

.flower-button--mini .flower-button__flower {
  grid-template-columns: .65em .65em;
  opacity: 0;
  transition: grid-template-columns .35s ease, opacity .25s ease;
}

.flower-button--mini .flower-button__flower--1 { top: -5px; left: -4px; }
.flower-button--mini .flower-button__flower--2 { bottom: -3px; left: 7px; }
.flower-button--mini .flower-button__flower--3 { bottom: -6px; }
.flower-button--mini .flower-button__flower--4 { top: -6px; }
.flower-button--mini .flower-button__flower--5 { right: 7px; top: -3px; }
.flower-button--mini .flower-button__flower--6 { right: -4px; bottom: -5px; }

.flower-button__petal {
  width: 1em;
  height: 1em;
  z-index: 0;
  border: .5px solid #96d1ec;
  border-radius: 40% 70% / 7% 90%;
  background: linear-gradient(#07a6d7, #93e0ee);
  transition: width .8s ease, height .8s ease, background .5s ease, border-color .5s ease;
}

.flower-button__petal--two { transform: rotate(90deg); }
.flower-button__petal--three { transform: rotate(270deg); }
.flower-button__petal--four { transform: rotate(180deg); }

.flower-button--mini .flower-button__petal {
  width: .65em;
  height: .65em;
  transition-duration: .35s;
}

.flower-button:hover:not(:disabled) .flower-button__petal,
.flower-button:focus-visible .flower-button__petal {
  border-color: #96b4ec;
  background: linear-gradient(#0761d7, #93bdee);
}

.flower-button:hover:not(:disabled) .flower-button__flower,
.flower-button:focus-visible .flower-button__flower { grid-template-columns: 1.5em 1.5em; }
.flower-button:hover:not(:disabled) .flower-button__petal,
.flower-button:focus-visible .flower-button__petal { width: 1.5em; height: 1.5em; }
.flower-button--mini:hover:not(:disabled) .flower-button__flower,
.flower-button--mini:focus-visible .flower-button__flower {
  grid-template-columns: .85em .85em;
  opacity: .9;
}
.flower-button--mini:hover:not(:disabled) .flower-button__petal,
.flower-button--mini:focus-visible .flower-button__petal {
  width: .85em;
  height: .85em;
}
.flower-button:hover:not(:disabled) .flower-button__text,
.flower-button:focus-visible .flower-button__text { background: rgba(255, 255, 255, .46); box-shadow: 0 8px 24px rgba(7, 97, 215, .18); transform: translateY(-1px); }

.flower-button.active .flower-button__text {
  color: #fdf1e1;
  background: rgba(17, 20, 17, .9);
}

.flower-button:hover:not(:disabled) .flower-button__flower--1,
.flower-button:focus-visible .flower-button__flower--1 { animation: flower-spin-1 15s linear infinite; }
.flower-button:hover:not(:disabled) .flower-button__flower--2,
.flower-button:focus-visible .flower-button__flower--2 { animation: flower-spin-2 13s linear 1s infinite; }
.flower-button:hover:not(:disabled) .flower-button__flower--3,
.flower-button:focus-visible .flower-button__flower--3 { animation: flower-spin-center 16s linear 1s infinite; }
.flower-button:hover:not(:disabled) .flower-button__flower--4,
.flower-button:focus-visible .flower-button__flower--4 { animation: flower-spin-center-offset 17s linear 1s infinite; }
.flower-button:hover:not(:disabled) .flower-button__flower--5,
.flower-button:focus-visible .flower-button__flower--5 { animation: flower-spin-5 20s linear 1s infinite; }
.flower-button:hover:not(:disabled) .flower-button__flower--6,
.flower-button:focus-visible .flower-button__flower--6 { animation: flower-spin-6 15s linear 1s infinite; }

.flower-button:focus-visible { outline: 2px solid #0761d7; outline-offset: 3px; }
.flower-button:disabled { opacity: .38; cursor: not-allowed; filter: grayscale(.6); }

@keyframes flower-spin-1 { to { transform: rotate(365deg); } }
@keyframes flower-spin-2 { to { transform: rotate(-325deg); } }
@keyframes flower-spin-center { to { transform: translateX(-50%) rotate(360deg); } }
@keyframes flower-spin-center-offset { to { transform: translateX(-50%) rotate(375deg); } }
@keyframes flower-spin-5 { to { transform: rotate(-335deg); } }
@keyframes flower-spin-6 { to { transform: rotate(390deg); } }

@media (prefers-reduced-motion: reduce) {
  .flower-button *, .flower-button *::before, .flower-button *::after { animation: none !important; transition-duration: .01ms !important; }
}
</style>
