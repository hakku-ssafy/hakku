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
  background-color: var(--color-track, #f0ebe2);
  background-image: linear-gradient(
    100deg,
    #f0ebe2 30%,
    #f6f3ee 50%,
    #f0ebe2 70%
  );
  background-size: 200% 100%;
  background-repeat: no-repeat;
  animation: u-shimmer 1.5s ease-in-out infinite;
}
@media (prefers-reduced-motion: reduce) {
  .skeleton {
    background-image: none;
    animation: none;
  }
}
</style>
