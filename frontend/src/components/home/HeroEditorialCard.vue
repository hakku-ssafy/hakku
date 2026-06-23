<template>
  <article class="ed-card" :class="tone">
    <span class="ed-card__kicker">{{ kicker }}</span>
    <div class="ed-card__caption">
      <h3 class="ed-card__title">{{ title }}</h3>
      <span class="ed-card__sub">{{ sub }}</span>
    </div>
  </article>
</template>

<script setup lang="ts">
interface Props {
  /** 배경 톤 클래스(u-tone-0 ~ u-tone-7). */
  tone: string
  kicker: string
  title: string
  sub: string
}
defineProps<Props>()
</script>

<style scoped>
.ed-card {
  position: relative;
  height: 100%;
  border-radius: var(--hk-radius-md);
  overflow: hidden;
}
.ed-card__kicker {
  position: absolute;
  top: 26px;
  left: 28px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0.2em;
  color: rgba(22, 20, 15, 0.55);
}
.ed-card__caption {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 32px 28px;
  background: linear-gradient(transparent, rgba(22, 20, 15, 0.42));
  color: #fff;
}
.ed-card__title {
  margin: 0 0 10px;
  font-size: 28px;
  line-height: 1.18;
  font-weight: 800;
  letter-spacing: -0.03em;
  white-space: pre-line;
}
.ed-card__sub {
  font-size: 13px;
  font-weight: 600;
  opacity: 0.94;
}
</style>
