<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import axios from 'axios'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '@/store'
import { storeToRefs } from 'pinia'
import { NIcon } from 'naive-ui'
import {
  ChatbubbleEllipsesOutline,
  ChatbubbleOutline,
  ChevronDownOutline,
  CloseOutline,
  FlashOutline,
  PeopleOutline,
  PersonOutline,
  SendOutline,
} from '@vicons/ionicons5'

const BACKEND_URL = 'http://localhost:2345'
const authStore = useAuthStore()
const { userInfoDatn } = storeToRefs(authStore)

// --- STATE ---
const isOpen = ref(false)
const userMessage = ref('')
const isLoading = ref(false)
const messagesContainer = ref(null)
const stompClient = ref(null)
const showOptions = ref(true)
const showSuggestions = ref(true)

const chatMode = ref('AI') // 'AI' | 'WAITING' | 'STAFF'

// 🔥 Biến lưu tên nhân viên
const currentStaffName = ref(localStorage.getItem('ml_staff_name') || '')

const headerTitle = computed(() => {
  // Nếu có tên nhân viên thì hiện tên, không thì hiện chữ mặc định
  if (chatMode.value === 'STAFF')
    return currentStaffName.value || 'Nhân viên tư vấn'
  if (chatMode.value === 'WAITING')
    return 'Đang kết nối...'
  return 'Hỗ trợ MyLaptop'
})

const headerSubtitle = computed(() => {
  if (chatMode.value === 'STAFF')
    return 'Đang hỗ trợ bạn'
  if (chatMode.value === 'WAITING')
    return 'Vui lòng chờ trong giây lát'
  return 'Phản hồi ngay lập tức'
})

const messages = ref([
  {
    sender: 'BOT',
    content: 'Xin chào! Cảm ơn bạn đã ghé thăm MyLaptop.\n📞 Hotline CSKH: **0965.237.19**\n\n📌 **HƯỚNG DẪN HỖ TRỢ:**\n- Em là Trợ lý AI, luôn sẵn sàng tư vấn cấu hình và báo giá Laptop 24/7.\n- Bất cứ lúc nào cần hỗ trợ chuyên sâu, anh/chị chỉ cần gõ cú pháp: **"gặp nhân viên"** (hoặc bấm nút bên dưới) để kết nối trực tiếp với tư vấn viên thật nhé!\n\nAnh/chị muốn hệ thống hỗ trợ tự động hay gặp nhân viên luôn ạ?',
    time: new Date(),
  },
])

const sessionId = ref(localStorage.getItem('chat_session_id') || `session-${Math.random().toString(36).substr(2, 9)}`)
localStorage.setItem('chat_session_id', sessionId.value)

async function backToAI() {
  chatMode.value = 'AI'
  // Đã xóa biến isWaitingStaff bị lỗi chưa khai báo
  messages.value.push({ sender: 'SYSTEM', content: 'Bạn đã chủ động ngắt kết nối. Trợ lý AI đã quay lại!' })
  scrollToBottom()

  try {
    await axios.post(`${BACKEND_URL}/api/v1/chat/end-support`, { sessionId: sessionId.value })
  }
  catch (error) {}
}

function connectSocket() {
  const socket = new SockJS(`${BACKEND_URL}/ws`)
  stompClient.value = new Client({
    webSocketFactory: () => socket,
    onConnect: () => {
      stompClient.value.subscribe(`/topic/user/${sessionId.value}`, (message) => {
        const msgBody = JSON.parse(message.body)
        if (msgBody.senderRole === 'STAFF') {
          messages.value.push({ sender: 'STAFF', content: msgBody.content })

          // 🔥 1. TRÍCH XUẤT TÊN NHÂN VIÊN TỪ CÂU CHÀO
          if (msgBody.content.includes('đã tiếp nhận hỗ trợ bạn')) {
            const match = msgBody.content.match(/Nhân viên (.*?) \(Mã:/)
            if (match && match[1]) {
              currentStaffName.value = match[1]
              localStorage.setItem('ml_staff_name', match[1])
            }
            chatMode.value = 'STAFF'
          }
          // 🔥 2. NẾU LÀ CÂU KẾT THÚC CỦA ADMIN -> ĐỔI TRẠNG THÁI VỀ AI
          else if (msgBody.content.includes('Phiên hỗ trợ đã kết thúc')) {
            chatMode.value = 'AI'
            currentStaffName.value = '' // Xóa tên đi
            localStorage.removeItem('ml_staff_name')
          }
          // 🔥 3. TIN NHẮN BÌNH THƯỜNG
          else {
            chatMode.value = 'STAFF'
          }

          isLoading.value = false
          scrollToBottom()
        }
      })
    },
    onStompError: frame => console.error('Lỗi socket:', frame.headers.message),
  })
  stompClient.value.activate()
}

function formatMessage(text) {
  if (!text)
    return ''
  return text
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2" target="_blank" class="chat-link">$1</a>')
}

function formatTime(date) {
  if (!date)
    return ''
  return new Date(date).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

async function handleOptionClick(choice) {
  showOptions.value = false
  if (choice === 'STAFF') {
    userMessage.value = 'gặp nhân viên'
    await sendMessage()
  }
  else {
    messages.value.push({ sender: 'USER', content: 'Tôi muốn hỗ trợ tự động', time: new Date() })
    setTimeout(() => {
      messages.value.push({
        sender: 'BOT',
        content: 'Dạ, tôi là trợ lý ảo của MyLaptop. Anh/chị cần tư vấn laptop hay hỗ trợ đơn hàng, cứ nhắn cho tôi nhé!',
        time: new Date(),
      })
      scrollToBottom()
    }, 500)
  }
}

async function sendMessage() {
  if (!userMessage.value.trim())
    return
  const text = userMessage.value
  messages.value.push({ sender: 'USER', content: text, time: new Date() })
  showOptions.value = false
  userMessage.value = ''
  isLoading.value = true
  scrollToBottom()

  try {
    const response = await axios.post(`${BACKEND_URL}/api/v1/chat/ask`, {
      sessionId: sessionId.value,
      message: text,
      customerId: userInfoDatn.value?.userId || null,
    })

    if (response.data) {
      if (response.data.includes('Đang chờ nhân viên') || response.data.includes('Hệ thống đã kết nối')) {
        chatMode.value = 'WAITING'
        messages.value.push({ sender: 'SYSTEM', content: response.data, time: new Date() })
      }
      else {
        messages.value.push({ sender: 'BOT', content: response.data, time: new Date() })
      }
    }
  }
  catch (error) {
    messages.value.push({ sender: 'SYSTEM', content: 'Không thể kết nối máy chủ. Vui lòng thử lại.', time: new Date() })
  }
  finally {
    isLoading.value = false
    scrollToBottom()
  }
}

async function sendSuggested(text) {
  userMessage.value = text
  await sendMessage()
}

function toggleChat() {
  isOpen.value = !isOpen.value
  if (isOpen.value)
    scrollToBottom()
}

onMounted(() => connectSocket())
onUnmounted(() => {
  if (stompClient.value)
    stompClient.value.deactivate()
})
</script>

<template>
  <div class="mlchat-wrapper">
    <transition name="bounce">
      <button class="mlchat-toggle" :class="{ 'is-open': isOpen }" @click="toggleChat">
        <NIcon size="22">
          <ChevronDownOutline v-if="isOpen" />
          <ChatbubbleOutline v-else />
        </NIcon>
        <span v-if="!isOpen" class="toggle-label">Hỗ trợ</span>
      </button>
    </transition>

    <transition name="panel-slide">
      <div v-if="isOpen" class="mlchat-panel">
        <div class="mlchat-header" :class="{ 'mode-staff': chatMode === 'STAFF', 'mode-waiting': chatMode === 'WAITING' }">
          <div class="header-avatar">
            <div class="avatar-ring">
              <NIcon size="18">
                <PersonOutline />
              </NIcon>
            </div>
            <span class="status-dot" :class="chatMode === 'WAITING' ? 'dot-waiting' : 'dot-online'" />
          </div>
          <div class="header-info">
            <div class="header-title">
              {{ headerTitle }}
            </div>
            <div class="header-subtitle">
              <span class="subtitle-dot" />
              {{ headerSubtitle }}
            </div>
          </div>
          <button class="header-close" @click="toggleChat">
            <NIcon size="16">
              <CloseOutline />
            </NIcon>
          </button>
        </div>

        <div
          v-if="chatMode === 'STAFF' || chatMode === 'WAITING'"
          style="background: #fef2f2; padding: 6px; text-align: center; border-bottom: 1px solid #fee2e2;"
        >
          <button
            style="border: none; background: transparent; color: #ef4444; font-size: 12px; cursor: pointer; font-weight: bold;"
            @click="backToAI"
          >
            ✕ Ngắt kết nối, chat với AI
          </button>
        </div>

        <div ref="messagesContainer" class="mlchat-messages">
          <div v-for="(msg, index) in messages" :key="index" class="msg-entry">
            <div v-if="msg.sender === 'SYSTEM'" class="sys-notice">
              <div class="sys-line" />
              <span>{{ msg.content }}</span>
              <div class="sys-line" />
            </div>

            <div v-else-if="msg.sender === 'USER'" class="msg-row msg-user">
              <div class="msg-col">
                <div class="bubble bubble-user" v-html="formatMessage(msg.content)" />
                <div class="msg-time">
                  {{ formatTime(msg.time) }}
                </div>
              </div>
            </div>

            <div v-else class="msg-row msg-bot">
              <div class="bot-avatar" :class="{ 'bot-avatar--staff': msg.sender === 'STAFF' }">
                <NIcon size="16">
                  <PersonOutline v-if="msg.sender === 'STAFF'" />
                  <ChatbubbleEllipsesOutline v-else />
                </NIcon>
              </div>
              <div class="msg-col">
                <div class="sender-label">
                  {{ msg.sender === 'STAFF' ? (currentStaffName || 'Nhân viên tư vấn') : 'Hỗ trợ MyLaptop' }}
                </div>

                <div class="bubble bubble-bot" v-html="formatMessage(msg.content)" />
                <div class="msg-time">
                  {{ formatTime(msg.time) }}
                </div>

                <div v-if="index === 0 && showOptions" class="choice-group">
                  <button class="choice-btn choice-ai" @click="handleOptionClick('AI')">
                    <NIcon size="15">
                      <FlashOutline />
                    </NIcon>
                    Hỗ trợ tự động
                  </button>
                  <button class="choice-btn choice-staff" @click="handleOptionClick('STAFF')">
                    <NIcon size="15">
                      <PeopleOutline />
                    </NIcon>
                    Gặp nhân viên
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div v-if="isLoading" class="msg-row msg-bot">
            <div class="bot-avatar">
              <NIcon size="16">
                <ChatbubbleEllipsesOutline />
              </NIcon>
            </div>
            <div class="bubble bubble-bot typing-bubble">
              <span class="dot" /><span class="dot" /><span class="dot" />
            </div>
          </div>
        </div>

        <transition name="fade">
          <div v-if="(chatMode === 'AI' || chatMode === 'WAITING') && showSuggestions" class="mlchat-suggestions">
            <div class="sugg-header">
              <span>Câu hỏi thường gặp</span>
              <button class="sugg-close" @click="showSuggestions = false">
                <NIcon size="13">
                  <CloseOutline />
                </NIcon>
              </button>
            </div>
            <div class="sugg-tags">
              <button class="sugg-tag" @click="sendSuggested('Làm thế nào để đặt hàng?')">
                Đặt hàng
              </button>
              <button class="sugg-tag" @click="sendSuggested('Có ưu đãi gì hiện tại không?')">
                Ưu đãi
              </button>
              <button class="sugg-tag" @click="sendSuggested('Laptop hot nhất hiện nay là gì?')">
                Laptop hot
              </button>
            </div>
          </div>
        </transition>

        <div class="mlchat-input">
          <input
            v-model="userMessage"
            placeholder="Nhập câu hỏi của bạn..."
            type="text"
            :disabled="isLoading"
          >
          <button class="send-btn" :disabled="isLoading || !userMessage.trim()" @click="sendMessage">
            <NIcon size="18">
              <SendOutline />
            </NIcon>
          </button>
        </div>

        <div class="mlchat-footer">
          Powered by <strong>MyLaptop</strong>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
/* ===== GIỮ NGUYÊN CSS NHƯ CŨ CỦA BẠN ===== */
.mlchat-wrapper {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
}
.mlchat-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #10b981;
  color: white;
  border: none;
  border-radius: 50px;
  padding: 14px 20px 14px 16px;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(16, 185, 129, 0.4);
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0.01em;
}
.mlchat-toggle:hover { background: #059669; transform: translateY(-2px); box-shadow: 0 8px 28px rgba(16, 185, 129, 0.45); }
.mlchat-toggle.is-open { padding: 14px; border-radius: 50%; }
.toggle-label { white-space: nowrap; }

.mlchat-panel {
  width: 360px;
  height: 520px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.mlchat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #10b981;
  color: white;
  transition: background 0.3s ease;
  flex-shrink: 0;
}
.mlchat-header.mode-staff { background: #f59e0b; }
.mlchat-header.mode-waiting { background: #4b5563; }

.header-avatar { position: relative; flex-shrink: 0; }
.avatar-ring {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid rgba(255,255,255,0.5);
}
.status-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid white;
}
.dot-online { background: #4ade80; }
.dot-waiting { background: #fbbf24; animation: pulse-dot 1.5s infinite; }

.header-info { flex: 1; min-width: 0; }
.header-title { font-size: 14px; font-weight: 700; line-height: 1.2; }
.header-subtitle { font-size: 11px; opacity: 0.85; display: flex; align-items: center; gap: 4px; margin-top: 2px; }
.subtitle-dot { width: 6px; height: 6px; border-radius: 50%; background: #4ade80; flex-shrink: 0; }

.header-close {
  background: rgba(255,255,255,0.2);
  border: none;
  color: white;
  cursor: pointer;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  flex-shrink: 0;
}
.header-close:hover { background: rgba(255,255,255,0.35); }

.mlchat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f9fafb;
  display: flex;
  flex-direction: column;
  gap: 12px;
  scroll-behavior: smooth;
}
.mlchat-messages::-webkit-scrollbar { width: 4px; }
.mlchat-messages::-webkit-scrollbar-thumb { background: #e5e7eb; border-radius: 4px; }

.msg-entry { display: flex; flex-direction: column; }

.sys-notice {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6b7280;
  font-size: 11px;
  font-style: italic;
  margin: 4px 0;
}
.sys-line { flex: 1; height: 1px; background: #e5e7eb; }

.msg-row { display: flex; gap: 8px; align-items: flex-end; }
.msg-user { flex-direction: row-reverse; }
.msg-col { display: flex; flex-direction: column; max-width: 78%; }
.msg-user .msg-col { align-items: flex-end; }

.bot-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #10b981;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-bottom: 2px;
}
.bot-avatar--staff { background: #f59e0b; }

.sender-label {
  font-size: 10px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 3px;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

.bubble {
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 13.5px;
  line-height: 1.55;
  word-break: break-word;
}
.bubble-bot {
  background: #ffffff;
  color: #111827;
  border: 1px solid #e5e7eb;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.bubble-user {
  background: #10b981;
  color: white;
  border-bottom-right-radius: 4px;
}

.msg-time {
  font-size: 10px;
  color: #9ca3af;
  margin-top: 3px;
  padding: 0 2px;
}

.typing-bubble {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  min-width: 52px;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #9ca3af;
  animation: typing 1.3s infinite;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

.choice-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 8px;
}
.choice-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  border-radius: 10px;
  border: 1.5px solid;
  background: white;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}

.choice-ai { border-color: #10b981; color: #10b981; }
.choice-ai:hover { background: #10b981; color: white; transform: translateX(3px); }

.choice-staff { border-color: #f59e0b; color: #92400e; }
.choice-staff:hover { background: #f59e0b; color: white; transform: translateX(3px); }

.mlchat-suggestions {
  padding: 10px 14px;
  background: white;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}
.sugg-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 7px;
  font-size: 11px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.sugg-close {
  background: none;
  border: none;
  color: #9ca3af;
  cursor: pointer;
  padding: 2px;
  display: flex;
  transition: color 0.2s;
}
.sugg-close:hover { color: #ef4444; }
.sugg-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.sugg-tag {
  padding: 5px 10px;
  border-radius: 20px;
  border: 1px solid #d1fae5;
  background: #f0fdf4;
  color: #059669;
  font-size: 11.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}
.sugg-tag:hover { background: #10b981; color: white; border-color: #10b981; }

.mlchat-input {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  background: white;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}
.mlchat-input input {
  flex: 1;
  padding: 9px 14px;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  outline: none;
  font-size: 13.5px;
  background: #f9fafb;
  transition: all 0.2s;
  color: #111827;
  font-family: inherit;
}
.mlchat-input input:focus {
  border-color: #10b981;
  background: white;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}
.mlchat-input input::placeholder { color: #9ca3af; }
.mlchat-input input:disabled { opacity: 0.6; }

.send-btn {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #10b981;
  border: none;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}
.send-btn:hover:not(:disabled) { background: #059669; transform: scale(1.05); }
.send-btn:disabled { background: #d1d5db; cursor: not-allowed; }

.mlchat-footer {
  text-align: center;
  font-size: 10px;
  color: #9ca3af;
  padding: 5px;
  background: white;
  border-top: 1px solid #e5e7eb;
}
.mlchat-footer strong { color: #10b981; }

.panel-slide-enter-active { animation: slideUp 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
.panel-slide-leave-active { animation: slideDown 0.2s ease-in; }
@keyframes slideUp { from { opacity: 0; transform: translateY(20px) scale(0.97); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes slideDown { from { opacity: 1; transform: translateY(0); } to { opacity: 0; transform: translateY(16px); } }

.bounce-enter-active { animation: bounceIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
@keyframes bounceIn { from { opacity: 0; transform: scale(0.6); } to { opacity: 1; transform: scale(1); } }

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@keyframes typing { 0%, 80%, 100% { transform: scale(0.7); opacity: 0.4; } 40% { transform: scale(1); opacity: 1; } }
@keyframes pulse-dot { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }

:deep(.chat-link) {
  color: #059669;
  font-weight: 600;
  text-decoration: none;
  border-bottom: 1px dashed #10b981;
  transition: all 0.2s;
}
:deep(.chat-link:hover) { color: #10b981; border-bottom-style: solid; }
:deep(strong) { font-weight: 700; }
</style>
