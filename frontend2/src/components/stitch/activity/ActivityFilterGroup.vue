<template>
  <section class="filter-group">
    <div class="filter-group-head">
      <h3>{{ title }}</h3>
      <span v-if="caption">{{ caption }}</span>
    </div>

    <div class="filter-chip-list">
      <button
        v-for="option in options"
        :key="option.value"
        type="button"
        :class="['filter-chip', modelValue === option.value && 'is-active']"
        @click="$emit('update:modelValue', modelValue === option.value ? '' : option.value)"
      >
        <span v-if="option.icon" class="chip-icon">
          <el-icon><component :is="option.icon" /></el-icon>
        </span>
        <span>{{ option.label }}</span>
      </button>
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  caption: {
    type: String,
    default: ''
  },
  modelValue: {
    type: String,
    default: ''
  },
  options: {
    type: Array,
    default: () => []
  }
})

defineEmits(['update:modelValue'])
</script>

<style scoped>
.filter-group {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.filter-group-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.filter-group-head h3 {
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #5a6482;
}

.filter-group-head span {
  color: #8e96b0;
  font-size: 12px;
  font-weight: 600;
}

.filter-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-chip {
  min-height: 42px;
  border: none;
  border-radius: 999px;
  padding: 0 16px;
  background: #eef2f7;
  color: #48516c;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-chip:hover {
  background: #e1e8f5;
  color: var(--cv-primary);
}

.filter-chip.is-active {
  color: #fff;
  background: linear-gradient(135deg, var(--cv-primary), #18b6ff);
  box-shadow: 0 14px 28px rgba(13, 71, 217, 0.18);
}

.chip-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
</style>
