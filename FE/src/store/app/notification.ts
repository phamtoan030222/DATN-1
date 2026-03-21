import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useRouter } from 'vue-router'

const BE_URL = 'http://localhost:2345'

export interface AppNotification {
  id: string
  title: string
  description: string
  relatedId: string
  relatedCode: string
  type: string
  isRead: boolean
  createdAt: number
}

export const useNotificationStore = defineStore('notification', () => {
  const router = useRouter()
  const notifications = ref<AppNotification[]>([])
  let stompClient: Client | null = null

  const unreadCount = computed(() => notifications.value.filter(n => !n.isRead).length)

  const asMessages = computed(() =>
    notifications.value.map((n, idx) => ({
      id: idx + 1,
      title: n.title,
      description: n.description,
      icon: 'ShoppingCartOutline',
      type: 0,
      tagTitle: n.isRead ? undefined : 'Mới',
      tagType: 'success' as const,
      date: new Date(n.createdAt).toLocaleString('vi-VN'),
      isRead: n.isRead,
      _notifId: n.id,
      _relatedId: n.relatedId,
      _relatedCode: n.relatedCode,
    } as any))
  )

  async function fetchNotifications() {
    try {
      const res = await fetch(`${BE_URL}/api/notifications`)
      const data: AppNotification[] = await res.json()
      notifications.value = data
    }
    catch (e) {
      console.error('Lỗi load notifications:', e)
    }
  }

  function connectWebSocket() {
    stompClient = new Client({
      webSocketFactory: () => new SockJS(`${BE_URL}/ws`),
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('WebSocket connected ✅')
        stompClient?.subscribe('/topic/admin-notifications', (message) => {
          const data: AppNotification = JSON.parse(message.body)
          notifications.value.unshift(data)
        })
      },
      onDisconnect: () => console.log('WebSocket disconnected'),
      onStompError: frame => console.error('STOMP error:', frame),
    })
    stompClient.activate()
  }

  function disconnectWebSocket() {
    stompClient?.deactivate()
  }

  async function handleNotificationClick(message: any) {
    const notifId = message._notifId
     const relatedCode = message._relatedCode

    const found = notifications.value.find(n => n.id === notifId)
    if (found)
      found.isRead = true

    try {
      await fetch(`${BE_URL}/api/notifications/${notifId}/read`, { method: 'PUT' })
    }
    catch { /* ignore */ }

    if (relatedCode) {
    router.push(`/orders/detail/${relatedCode}`) 
  }
  }

  async function markAllAsRead() {
    notifications.value.forEach(n => (n.isRead = true))
    try {
      await fetch(`${BE_URL}/api/notifications/read-all`, { method: 'PUT' })
    }
    catch { /* ignore */ }
  }

  return {
    notifications,
    unreadCount,
    asMessages,
    fetchNotifications,
    connectWebSocket,
    disconnectWebSocket,
    handleNotificationClick,
    markAllAsRead,
  }
})