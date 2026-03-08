<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue';
import axios from 'axios';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '@/store';
import { storeToRefs } from 'pinia';

const BACKEND_URL = 'http://localhost:2345';
const authStore = useAuthStore();
const { userInfoDatn } = storeToRefs(authStore);

// --- STATE ---
const isOpen = ref(false);
const userMessage = ref('');
const isLoading = ref(false);
const messagesContainer = ref(null);
const stompClient = ref(null);
const showOptions = ref(true);
const showSuggestions = ref(true);

const chatMode = ref('AI'); // 'AI' | 'WAITING' | 'STAFF'

const headerTitle = computed(() => {
  if (chatMode.value === 'STAFF') return 'Nhân viên tư vấn';
  if (chatMode.value === 'WAITING') return 'Đang kết nối...';
  return 'Hỗ trợ MyLaptop';
});

const headerSubtitle = computed(() => {
  if (chatMode.value === 'STAFF') return 'Đang hỗ trợ bạn';
  if (chatMode.value === 'WAITING') return 'Vui lòng chờ trong giây lát';
  return 'Phản hồi ngay lập tức';
});

const messages = ref([
  {
    sender: 'BOT',
content: 'Xin chào! Cảm ơn bạn đã ghé thăm MyLaptop.\n📞 Hotline CSKH: **0965.237.19**\n\n📌 **HƯỚNG DẪN HỖ TRỢ:**\n- Em là Trợ lý AI, luôn sẵn sàng tư vấn cấu hình và báo giá Laptop 24/7.\n- Bất cứ lúc nào cần hỗ trợ chuyên sâu, anh/chị chỉ cần gõ cú pháp: **"gặp nhân viên"** (hoặc bấm nút bên dưới) để kết nối trực tiếp với tư vấn viên thật nhé!\n\nAnh/chị muốn hệ thống hỗ trợ tự động hay gặp nhân viên luôn ạ?',
    time: new Date()
  }
]);

const sessionId = ref(localStorage.getItem('chat_session_id') || 'session-' + Math.random().toString(36).substr(2, 9));
localStorage.setItem('chat_session_id', sessionId.value);

const connectSocket = () => {
  const socket = new SockJS(`${BACKEND_URL}/ws`);
  stompClient.value = new Client({
    webSocketFactory: () => socket,
    onConnect: () => {
      stompClient.value.subscribe(`/topic/user/${sessionId.value}`, (message) => {
        const msgBody = JSON.parse(message.body);
        if (msgBody.senderRole === 'STAFF') {
          chatMode.value = 'STAFF';
          messages.value.push({ sender: 'STAFF', content: msgBody.content, time: new Date() });
          isLoading.value = false;
          scrollToBottom();
        }
      });
    },
    onStompError: (frame) => console.error('Lỗi socket:', frame.headers['message'])
  });
  stompClient.value.activate();
};

const formatMessage = (text) => {
  if (!text) return '';
  return text
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2" target="_blank" class="chat-link">$1</a>');
};

const formatTime = (date) => {
  if (!date) return '';
  return new Date(date).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
};

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  });
};

const handleOptionClick = async (choice) => {
  showOptions.value = false;
  if (choice === 'STAFF') {
    userMessage.value = 'gặp nhân viên';
    await sendMessage();
  } else {
    messages.value.push({ sender: 'USER', content: 'Tôi muốn hỗ trợ tự động', time: new Date() });
    setTimeout(() => {
      messages.value.push({
        sender: 'BOT',
        content: 'Dạ, tôi là trợ lý ảo của MyLaptop. Anh/chị cần tư vấn laptop hay hỗ trợ đơn hàng, cứ nhắn cho tôi nhé!',
        time: new Date()
      });
      scrollToBottom();
    }, 500);
  }
};

const sendMessage = async () => {
  if (!userMessage.value.trim()) return;
  const text = userMessage.value;
  messages.value.push({ sender: 'USER', content: text, time: new Date() });
  showOptions.value = false;
  userMessage.value = '';
  isLoading.value = true;
  scrollToBottom();

  try {
    const response = await axios.post(`${BACKEND_URL}/api/v1/chat/ask`, {
      sessionId: sessionId.value,
      message: text,
      customerId: userInfoDatn.value?.userId || null
    });

    if (response.data) {
      if (response.data.includes('Đang chờ nhân viên') || response.data.includes('Hệ thống đã kết nối')) {
        chatMode.value = 'WAITING';
        messages.value.push({ sender: 'SYSTEM', content: response.data, time: new Date() });
      } else {
        messages.value.push({ sender: 'BOT', content: response.data, time: new Date() });
      }
    }
  } catch (error) {
    messages.value.push({ sender: 'SYSTEM', content: 'Không thể kết nối máy chủ. Vui lòng thử lại.', time: new Date() });
  } finally {
    isLoading.value = false;
    scrollToBottom();
  }
};

const sendSuggested = async (text) => {
  userMessage.value = text;
  await sendMessage();
};

const toggleChat = () => {
  isOpen.value = !isOpen.value;
  if (isOpen.value) scrollToBottom();
};

onMounted(() => connectSocket());
onUnmounted(() => { if (stompClient.value) stompClient.value.deactivate(); });
</script>

<template>
  <div class="mlchat-wrapper">
    <!-- TOGGLE BUTTON -->
    <transition name="bounce">
      <button class="mlchat-toggle" @click="toggleChat" :class="{ 'is-open': isOpen }">
        <span class="toggle-icon">
          <svg v-if="!isOpen" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </span>
        <span v-if="!isOpen" class="toggle-label">Hỗ trợ</span>
      </button>
    </transition>

    <!-- CHAT PANEL -->
    <transition name="panel-slide">
      <div v-if="isOpen" class="mlchat-panel">

        <!-- HEADER -->
        <div class="mlchat-header" :class="{ 'mode-staff': chatMode === 'STAFF', 'mode-waiting': chatMode === 'WAITING' }">
          <div class="header-avatar">
            <div class="avatar-ring">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v2.4h19.2v-2.4c0-3.2-6.4-4.8-9.6-4.8z"/>
              </svg>
            </div>
            <span class="status-dot" :class="chatMode === 'WAITING' ? 'dot-waiting' : 'dot-online'"></span>
          </div>
          <div class="header-info">
            <div class="header-title">{{ headerTitle }}</div>
            <div class="header-subtitle">
              <span class="subtitle-dot"></span>
              {{ headerSubtitle }}
            </div>
          </div>
          <button class="header-close" @click="toggleChat">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>

        <!-- MESSAGES -->
        <div class="mlchat-messages" ref="messagesContainer">
          <div v-for="(msg, index) in messages" :key="index" class="msg-entry">

            <!-- SYSTEM NOTICE -->
            <div v-if="msg.sender === 'SYSTEM'" class="sys-notice">
              <div class="sys-line"></div>
              <span>{{ msg.content }}</span>
              <div class="sys-line"></div>
            </div>

            <!-- USER MESSAGE -->
            <div v-else-if="msg.sender === 'USER'" class="msg-row msg-user">
              <div class="msg-col">
                <div class="bubble bubble-user" v-html="formatMessage(msg.content)"></div>
                <div class="msg-time">{{ formatTime(msg.time) }}</div>
              </div>
            </div>

            <!-- BOT / STAFF MESSAGE -->
            <div v-else class="msg-row msg-bot">
              <div class="bot-avatar" :class="{ 'bot-avatar--staff': msg.sender === 'STAFF' }">
                <svg v-if="msg.sender === 'STAFF'" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v2.4h19.2v-2.4c0-3.2-6.4-4.8-9.6-4.8z"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-7 12h-2v-2h2v2zm0-4h-2V6h2v4z"/>
                </svg>
              </div>
              <div class="msg-col">
                <div class="sender-label">{{ msg.sender === 'STAFF' ? 'Nhân viên tư vấn' : 'Hỗ trợ MyLaptop' }}</div>
                <div class="bubble bubble-bot" v-html="formatMessage(msg.content)"></div>
                <div class="msg-time">{{ formatTime(msg.time) }}</div>

                <!-- CHOICE BUTTONS sau tin nhắn đầu -->
                <div v-if="index === 0 && showOptions" class="choice-group">
                  <button class="choice-btn choice-ai" @click="handleOptionClick('AI')">
                    <span class="choice-icon">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="3"/><path d="M12 1v4M12 19v4M4.22 4.22l2.83 2.83M16.95 16.95l2.83 2.83M1 12h4M19 12h4M4.22 19.78l2.83-2.83M16.95 7.05l2.83-2.83"/>
                      </svg>
                    </span>
                    Hỗ trợ tự động
                  </button>
                  <button class="choice-btn choice-staff" @click="handleOptionClick('STAFF')">
                    <span class="choice-icon">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                      </svg>
                    </span>
                    Gặp nhân viên
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- TYPING INDICATOR -->
          <div v-if="isLoading" class="msg-row msg-bot">
            <div class="bot-avatar">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/>
              </svg>
            </div>
            <div class="bubble bubble-bot typing-bubble">
              <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            </div>
          </div>
        </div>

        <!-- QUICK SUGGESTIONS -->
        <transition name="fade">
          <div v-if="(chatMode === 'AI' || chatMode === 'WAITING') && showSuggestions" class="mlchat-suggestions">
            <div class="sugg-header">
              <span>Câu hỏi thường gặp</span>
              <button class="sugg-close" @click="showSuggestions = false">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="12" height="12">
                  <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>
            <div class="sugg-tags">
              <button class="sugg-tag" @click="sendSuggested('Làm thế nào để đặt hàng?')">🛒 Đặt hàng</button>
              <button class="sugg-tag" @click="sendSuggested('Có ưu đãi gì hiện tại không?')">🎁 Ưu đãi</button>
              <button class="sugg-tag" @click="sendSuggested('Laptop hot nhất hiện nay là gì?')">🔥 Laptop hot</button>
            </div>
          </div>
        </transition>

        <!-- INPUT -->
        <div class="mlchat-input">
          <input
            v-model="userMessage"
            @keyup.enter="sendMessage"
            placeholder="Nhập câu hỏi của bạn..."
            type="text"
            :disabled="isLoading"
          />
          <button class="send-btn" @click="sendMessage" :disabled="isLoading || !userMessage.trim()">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
            </svg>
          </button>
        </div>

        <div class="mlchat-footer">Powered by <strong>MyLaptop</strong></div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
/* ===== TOKENS ===== */

/* ===== WRAPPER ===== */
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

/* ===== TOGGLE BUTTON ===== */
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
.toggle-icon { width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; }
.toggle-icon svg { width: 22px; height: 22px; }
.toggle-label { white-space: nowrap; }

/* ===== PANEL ===== */
.mlchat-panel {
  width: 360px;
  height: 520px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: var(--ml-shadow);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

/* ===== HEADER ===== */
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
.avatar-ring svg { width: 20px; height: 20px; fill: white; }
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
.header-close svg { width: 16px; height: 16px; }

/* ===== MESSAGES ===== */
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

/* SYSTEM */
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

/* ROW */
.msg-row { display: flex; gap: 8px; align-items: flex-end; }
.msg-user { flex-direction: row-reverse; }
.msg-col { display: flex; flex-direction: column; max-width: 78%; }
.msg-user .msg-col { align-items: flex-end; }

/* BOT AVATAR */
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
.bot-avatar svg { width: 16px; height: 16px; fill: white; }
.bot-avatar--staff { background: #f59e0b; }

.sender-label {
  font-size: 10px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 3px;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

/* BUBBLES */
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

/* TYPING */
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

/* CHOICE BUTTONS */
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
.choice-icon { width: 16px; height: 16px; display: flex; align-items: center; flex-shrink: 0; }
.choice-icon svg { width: 16px; height: 16px; }

.choice-ai { border-color: #10b981; color: #10b981; }
.choice-ai:hover { background: #10b981; color: white; transform: translateX(3px); }

.choice-staff { border-color: #f59e0b; color: #92400e; }
.choice-staff:hover { background: #f59e0b; color: white; transform: translateX(3px); }

/* ===== SUGGESTIONS ===== */
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

/* ===== INPUT ===== */
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
.send-btn svg { width: 18px; height: 18px; }
.send-btn:hover:not(:disabled) { background: #059669; transform: scale(1.05); }
.send-btn:disabled { background: #d1d5db; cursor: not-allowed; }

/* ===== FOOTER ===== */
.mlchat-footer {
  text-align: center;
  font-size: 10px;
  color: #9ca3af;
  padding: 5px;
  background: white;
  border-top: 1px solid #e5e7eb;
}
.mlchat-footer strong { color: #10b981; }

/* ===== ANIMATIONS ===== */
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

/* ===== INLINE LINKS ===== */
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