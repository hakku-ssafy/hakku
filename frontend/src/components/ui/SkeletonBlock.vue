<template>
  <div class="skeleton rounded-lg" :style="{ height, width }" />
</template>

<script setup lang="ts">
withDefaults(defineProps<{ height?: string; width?: string }>(), {
  height: '1rem',
  width: '100%',
})
</script>

<style scoped>
.skeleton {
  background: linear-gradient(
    100deg,
    var(--color-surface-sunken) 30%,
    color-mix(in srgb, var(--color-accent) 7%, var(--color-surface-soft)) 50%,
    var(--color-surface-sunken) 70%
  );
  background-size: 200% 100%;
  animation: u-shimmer 1.5s ease-in-out infinite;
}
</style>
