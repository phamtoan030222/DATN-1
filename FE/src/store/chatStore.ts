import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export const useChatStore = defineStore('chatStore', () => {
  const BACKEND_URL = 'http://localhost:2345'

  const stompClient = ref<Client | null>(null)

  // Khôi phục lịch sử từ LocalStorage
  const sessions = ref<Record<string, any>>(
    JSON.parse(localStorage.getItem('admin_chat_history') || '{}'),
  )

  // Lưu Session ID mà Admin ĐANG MỞ
  const currentActiveSessionId = ref<string | null>(null)

  watch(sessions, (newVal) => {
    localStorage.setItem('admin_chat_history', JSON.stringify(newVal))
  }, { deep: true })

  const totalUnread = computed(() => {
    let userCount = 0
    for (const key in sessions.value) {
      const session = sessions.value[key]
      if (session.status !== 'AI' && session.status !== 'CLOSED') {
        if (session.status === 'WAITING' || session.unreadCount > 0) {
          userCount += 1
        }
      }
    }
    return userCount
  })

 const handleIncomingMessage = (msg: any) => {
    const sId = msg.sessionId
    if (!sessions.value[sId]) {
      sessions.value[sId] = {
        id: sId,
        customerName: msg.customer ? msg.customer.fullName || msg.customer.name : 'Khách vãng lai',
        messages: [],
        unreadCount: 0,
        lastMessage: '',
        status: 'AI', 
        lastTime: new Date().getTime(),
      }
    }

    sessions.value[sId].messages.push(msg)
    sessions.value[sId].lastTime = new Date().getTime()
    sessions.value[sId].lastMessage = msg.content

    const content = msg.content ? String(msg.content).toLowerCase().trim() : ''

    if ((content === 'gặp nhân viên' || content === 'chat với nhân viên') && content.length < 50) {
      sessions.value[sId].status = 'WAITING'
    }
    if (content.includes('hệ thống đã kết nối') || content.includes('đang chờ nhân viên')) {
      sessions.value[sId].status = 'WAITING'
    }

    if (
      content.includes('phiên hỗ trợ đã kết thúc') || 
      content.includes('trợ lý ai đã quay trở lại') ||
      content.includes('hoặc nếu cần hỗ trợ chuyên sâu hơn') || 
      content.includes('anh/chị cứ nhắn \'gặp nhân viên\'')     
    ) {
      sessions.value[sId].status = 'AI'
      sessions.value[sId].unreadCount = 0 
    }
    if (sessions.value[sId].status !== 'AI') {
      if (currentActiveSessionId.value !== sId) {
        if (content.length < 500 && !content.includes('hệ thống đã kết nối')) {
          sessions.value[sId].unreadCount++
        }
      }
    }
  }

  const connectSocket = () => {
    if (stompClient.value && stompClient.value.connected)
      return

    const socket = new SockJS(`${BACKEND_URL}/ws`)
    stompClient.value = new Client({
      webSocketFactory: () => socket,
      onConnect: () => {
        // eslint-disable-next-line no-console
        console.log(' Đã kết nối Socket Toàn Cầu (Global)!')
        stompClient.value?.subscribe('/topic/admin-messages', (message: any) => {
          const msgBody = JSON.parse(message.body)
          handleIncomingMessage(msgBody)
        })
      },
    })
    stompClient.value.activate()
  }

  return {
    sessions,
    stompClient,
    totalUnread,
    currentActiveSessionId,
    connectSocket,
  }
})
