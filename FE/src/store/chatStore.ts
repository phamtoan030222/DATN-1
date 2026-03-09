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

  // 🔥 BIẾN QUAN TRỌNG: TỔNG SỐ TIN NHẮN CHƯA ĐỌC TOÀN HỆ THỐNG
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

    // 1. Khởi tạo session nếu chưa có
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

    // 2. Thêm tin nhắn mới vào danh sách
    sessions.value[sId].messages.push(msg)
    sessions.value[sId].lastTime = new Date().getTime()
    if (msg.senderRole !== 'SYSTEM') {
      sessions.value[sId].lastMessage = msg.content
    }

    // Chuyển nội dung về chữ thường và xóa khoảng trắng dư thừa 2 đầu
    const content = msg.content ? String(msg.content).toLowerCase().trim() : ''

    let isRequestSupport = false

    // ========================================================
    // 🔥 CHIÊU CUỐI: DÙNG DẤU "===" ĐỂ BẮT CHÍNH XÁC 100%
    // Khách bắt buộc phải nhắn đúng y xì dòng này mới tính
    // ========================================================
    if (content === 'gặp nhân viên' || content === 'chat với nhân viên') {
      isRequestSupport = true
    }

    // Bắt câu thông báo của hệ thống (nếu có)
    if (content === 'hệ thống đã kết nối bạn với nhân viên' || content.includes('đang chờ nhân viên')) {
      isRequestSupport = true
    }

    // ========================================================
    // 🔥 TỰ ĐỘNG LÙI VỀ TRẠNG THÁI 'AI' KHI KẾT THÚC
    // ========================================================
    if (
      content.includes('phiên hỗ trợ đã kết thúc')
      || content.includes('ngắt kết nối')
      || content.includes('trợ lý ai đã quay trở lại')
    ) {
      sessions.value[sId].status = 'AI'
      sessions.value[sId].unreadCount = 0
    }

    // Chuyển sang WAITING nếu có yêu cầu gặp nhân viên hợp lệ
    if (isRequestSupport && (sessions.value[sId].status === 'AI' || sessions.value[sId].status === 'CLOSED')) {
      sessions.value[sId].status = 'WAITING'
    }

    // Tăng số đếm tin nhắn chưa đọc (Chỉ tăng khi KHÁC chế độ AI)
    if (sessions.value[sId].status !== 'AI') {
      if (currentActiveSessionId.value !== sId && (msg.senderRole === 'CLIENT' || msg.senderRole === 'USER')) {
        // Chỉ đếm những tin nhắn có độ dài bình thường của khách, chặn tin spam siêu dài của AI
        if (content.length < 500) {
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
