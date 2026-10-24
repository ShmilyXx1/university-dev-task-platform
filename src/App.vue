<script setup>
import { onMounted } from 'vue'
import { useUserStore } from './stores/user'
import { initChatWs } from './utils/ws'

const userStore = useUserStore()

// 刷新/冷启动后若有登录态，重建聊天 WebSocket（登录时 setToken 也会触发，initChatWs 内部已做幂等）
onMounted(() => {
  if (userStore.token) initChatWs(userStore.token)
})
</script>

<template>
  <router-view v-slot="{ Component }">
    <transition name="fade-page" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<style>
/* 全局样式已迁移至 style.css */
</style>
