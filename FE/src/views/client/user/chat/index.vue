<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue';
import axios from 'axios';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '@/store';


const BACKEND_URL = 'http://localhost:2345'; 

const {  userInfoDatn } = storeToRefs(useAuthStore());

// --- STATE ---
const isOpen = ref(false); 
const userMessage = ref('');
const isLoading = ref(false);
const messagesContainer = ref(null);
const stompClient = ref(null);
const showOptions = ref(true);
const showSuggestions = ref(true);

// 🔥 TRẠNG THÁI CHAT: 'AI' | 'WAITING' | 'STAFF'
const chatMode = ref('AI'); 

// Tự động đổi tên Header dựa vào trạng thái
const headerTitle = computed(() => {
  if (chatMode.value === 'STAFF') return '👨‍💻 Nhân viên hỗ trợ';
  if (chatMode.value === 'WAITING') return '⏳ Đang kết nối...';
  return '🤖 Trợ lý ảo AI';
});

const messages = ref([
  { 
    sender: 'AI', 
    content: 'Xin chào! Cảm ơn bạn đã ghé thăm MyLaptop.\n📞 Hotline khẩn cấp: **0965.237.19**\n\nBạn muốn hệ thống hỗ trợ tự động hay kết nối trực tiếp với nhân viên tư vấn?' 
  }
]);


const sessionId = ref(localStorage.getItem('chat_session_id') || 'session-' + Math.random().toString(36).substr(2, 9));
localStorage.setItem('chat_session_id', sessionId.value);

// --- 1. KẾT NỐI WEBSOCKET ---
const connectSocket = () => {
  const socket = new SockJS(`${BACKEND_URL}/ws`);
  stompClient.value = new Client({
    webSocketFactory: () => socket,
    onConnect: () => {
      stompClient.value.subscribe(`/topic/user/${sessionId.value}`, (message) => {
        const msgBody = JSON.parse(message.body);
        
        // Nhận tin từ STAFF
        if (msgBody.senderRole === 'STAFF') {
          chatMode.value = 'STAFF'; // Đổi tiêu đề thành Nhân viên
          
          messages.value.push({
            sender: 'STAFF',
            content: msgBody.content
          });
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
    // 🔥 THÊM DÒNG NÀY ĐỂ RENDER LINK SẢN PHẨM 🔥
    .replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2" target="_blank" class="chat-product-link">$1</a>');
};

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  });
};

// --- 2. XỬ LÝ NÚT CHỌN BAN ĐẦU ---
const handleOptionClick = async (choice) => {
  showOptions.value = false; 
  if (choice === 'STAFF') {
    userMessage.value = 'gặp nhân viên'; 
    await sendMessage();
  } else {
    messages.value.push({ sender: 'USER', content: 'Mình muốn chat với Trợ lý AI' });
    setTimeout(() => {
      messages.value.push({ sender: 'AI', content: 'Dạ, em là trợ lý AI đây ạ 🤖. Anh/chị cần tìm mẫu Laptop nào, hoặc cứ nhắn yêu cầu cấu hình để em tư vấn nhé!' });
      scrollToBottom();
    }, 500);
  }
};

// --- 3. GỬI TIN NHẮN CHÍNH ---
const sendMessage = async () => {
  if (!userMessage.value.trim()) return;

  const text = userMessage.value;
  messages.value.push({ sender: 'USER', content: text });
  
  showOptions.value = false; 
  userMessage.value = '';
  isLoading.value = true;
  scrollToBottom();

  try {
    const response = await axios.post(`${BACKEND_URL}/api/v1/chat/ask`, {
      sessionId: sessionId.value,
      message: text
    });

    if (response.data) {
      // Nếu Backend báo đang chờ nhân viên
      if (response.data.includes("Đang chờ nhân viên") || response.data.includes("Hệ thống đã kết nối")) {
         chatMode.value = 'WAITING'; // Đổi trạng thái sang chờ
         // Hiển thị dòng thông báo của hệ thống vào giữa màn hình
         messages.value.push({ sender: 'SYSTEM', content: response.data });
      } else {
         messages.value.push({ sender: 'AI', content: response.data });
      }
    }
  } catch (error) {
    messages.value.push({ sender: 'SYSTEM', content: 'Lỗi kết nối Server 😢' });
  } finally {
    isLoading.value = false;
    scrollToBottom();
  }
};

// Hàm gửi câu hỏi gợi ý
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
  <div class="chat-widget-wrapper">
    <transition name="slide-fade">
      <div v-if="isOpen" class="chat-container">
        
        <div class="chat-header" :class="{ 'header-staff': chatMode === 'STAFF', 'header-waiting': chatMode === 'WAITING' }">
          <h3>{{ headerTitle }}</h3>
          <button class="close-btn" @click="toggleChat">✖</button>
        </div>

        <div class="messages-box" ref="messagesContainer">
          <div 
            v-for="(msg, index) in messages" 
            :key="index" 
            class="message-wrapper"
          >
            <div :class="[
              'message', 
              msg.sender === 'USER' ? 'my-message' : 
              (msg.sender === 'SYSTEM' ? 'system-message' : 'ai-message')
            ]">
              <div v-if="msg.sender === 'STAFF'" class="avatar staff-avatar">👮</div>
              <div v-if="msg.sender === 'AI'" class="avatar ai-avatar">🤖</div>
              
              <div class="bubble" :class="{ 'staff-bubble': msg.sender === 'STAFF', 'system-bubble': msg.sender === 'SYSTEM' }" v-html="formatMessage(msg.content)"></div>
            </div>

            <div v-if="index === 0 && showOptions" class="quick-options">
              <button @click="handleOptionClick('AI')" class="opt-btn ai-btn">🤖 Trợ lý AI hỗ trợ tự động</button>
              <button @click="handleOptionClick('STAFF')" class="opt-btn staff-btn">👨‍💻 Kết nối Nhân viên tư vấn</button>
            </div>
          </div>
          
          <div v-if="isLoading" class="message ai-message">
            <div class="avatar ai-avatar">🤖</div>
            <div class="bubble loading">
              <span>.</span><span>.</span><span>.</span>
            </div>
          </div>
        </div>

      <div class="suggest-box" v-if="(chatMode === 'AI' || chatMode === 'WAITING') && showSuggestions">
          <div class="suggest-header">
            <div class="suggest-title">💡 Câu hỏi gợi ý:</div>
            <button class="close-suggest-btn" @click="showSuggestions = false" title="Ẩn gợi ý">✕</button>
          </div>
          <div class="suggest-tags">
            <button class="suggest-tag" @click="sendSuggested('Làm thế nào để đặt hàng?')">Làm thế nào để đặt hàng?</button>
            <button class="suggest-tag" @click="sendSuggested('Chính sách đổi trả hàng?')">Chính sách đổi trả hàng?</button>
            <button class="suggest-tag" @click="sendSuggested('Phí vận chuyển là bao nhiêu?')">Phí vận chuyển là bao nhiêu?</button>
          </div>
        </div>

        <div class="input-area">
          <input 
            v-model="userMessage" 
            @keyup.enter="sendMessage"
            placeholder="Hỏi gì đi bạn ơi..." 
            type="text" 
            :disabled="isLoading" 
          />
          <button @click="sendMessage" :disabled="isLoading || !userMessage.trim()">➤</button>
        </div>
      </div>
    </transition>

    <button class="toggle-chat-btn" @click="toggleChat">
      <span v-if="!isOpen">💬</span>
      <span v-else>🔻</span>
    </button>
  </div>
</template>

<style scoped>
/* --- ĐỔI MÀU HEADER --- */
.chat-header { padding: 15px; background-color: #049d14; color: white; display: flex; justify-content: space-between; align-items: center; transition: background-color 0.3s; }
.header-staff { background-color: #f39c12; } /* Màu cam khi gặp nhân viên */
.header-waiting { background-color: #6c757d; } /* Màu xám khi chờ */

/* --- SYSTEM MESSAGES --- */
.system-message { justify-content: center; width: 100%; margin: 10px 0; }
.system-bubble { background-color: transparent !important; color: #888 !important; font-size: 12px !important; font-style: italic; text-align: center; box-shadow: none !important; padding: 0 !important; }

/* --- CÁC STYLE CŨ GIỮ NGUYÊN --- */
.message-wrapper { display: flex; flex-direction: column; width: 100%; }
.quick-options { display: flex; flex-direction: column; gap: 8px; margin-top: 8px; margin-left: 45px; max-width: 80%; }
.opt-btn { padding: 10px 14px; border-radius: 15px; border: 1.5px solid #049d14; background-color: white; color: #049d14; font-weight: 600; font-size: 13px; text-align: left; cursor: pointer; transition: all 0.2s ease; box-shadow: 0 2px 5px rgba(0,0,0,0.05); }
.opt-btn:hover { background-color: #049d14; color: white; transform: translateY(-2px); }
.staff-btn { border-color: #f39c12; color: #d35400; }
.staff-btn:hover { background-color: #f39c12; color: white; }
.chat-widget-wrapper { position: fixed; bottom: 20px; right: 20px; z-index: 9999; display: flex; flex-direction: column; align-items: flex-end; }
.toggle-chat-btn { width: 60px; height: 60px; border-radius: 50%; background-color: #049d14; color: white; border: none; font-size: 30px; cursor: pointer; box-shadow: 0 4px 12px rgba(0,0,0,0.3); transition: transform 0.2s; display: flex; align-items: center; justify-content: center; margin-top: 10px; }
.toggle-chat-btn:hover { transform: scale(1.1); }
.chat-container { width: 350px; height: 500px; background-color: #fff; border-radius: 12px; box-shadow: 0 5px 20px rgba(0,0,0,0.2); display: flex; flex-direction: column; border: 1px solid #eee; overflow: hidden; }
.chat-header h3 { margin: 0; font-size: 16px; }
.close-btn { background: none; border: none; color: white; font-size: 18px; cursor: pointer; }
.messages-box { flex: 1; padding: 15px; overflow-y: auto; background-color: #f9f9f9; display: flex; flex-direction: column; gap: 10px; }
.message { display: flex; width: 100%; align-items: flex-end; gap: 5px; }
.my-message { justify-content: flex-end; }
.ai-message { justify-content: flex-start; }
.bubble { max-width: 80%; padding: 10px 14px; border-radius: 18px; font-size: 14px; line-height: 1.5; word-wrap: break-word; }
.my-message .bubble { background-color: #049d14; color: white; border-bottom-right-radius: 2px; }
.ai-message .bubble { background-color: #e4e6eb; color: black; border-bottom-left-radius: 2px; }
.staff-bubble { background-color: #fff3cd !important; border: 1px solid #ffeeba; }
.avatar { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 16px; }
.staff-avatar { background: #ffc107; margin-bottom: 2px;}
.ai-avatar { background: #e3f2fd; margin-right: 5px; border: 1px solid #bbdefb; }
.input-area { padding: 10px; border-top: 1px solid #ddd; background-color: white; display: flex; gap: 8px; }
input { flex: 1; padding: 10px; border: 1px solid #ddd; border-radius: 20px; outline: none; }
input:focus { border-color: #049d14; }
.input-area button { background-color: #049d14; color: white; border: none; padding: 0 15px; border-radius: 20px; cursor: pointer; }
.input-area button:disabled { background-color: #ccc; }
.slide-fade-enter-active, .slide-fade-leave-active { transition: all 0.3s ease; }
.slide-fade-enter-from, .slide-fade-leave-to { transform: translateY(20px); opacity: 0; }
.loading span { animation: blink 1.4s infinite both; font-size: 20px; line-height: 10px; }
.loading span:nth-child(2) { animation-delay: 0.2s; }
.loading span:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink { 0% { opacity: 0.2; } 20% { opacity: 1; } 100% { opacity: 0.2; } }
:deep(strong) { font-weight: bold; color: #000; }

/* Thêm CSS cho link sản phẩm trong chat */
:deep(.chat-product-link) {
  color: #049d14; /* Màu xanh lá chuẩn của dự án */
  font-weight: bold;
  text-decoration: none;
  border-bottom: 1px dashed #049d14;
  transition: all 0.2s ease;
}

:deep(.chat-product-link:hover) {
  color: #d35400; /* Đổi màu cam khi di chuột vào */
  border-bottom: 1px solid #d35400;
}
/* --- CÂU HỎI GỢI Ý --- */
.suggest-box { padding: 10px 16px; background-color: #fff; border-top: 1px solid var(--border-color); }
.suggest-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.suggest-title { font-size: 12px; color: #6b7280; font-weight: 600; margin: 0; }
.close-suggest-btn { background: none; border: none; color: #9ca3af; font-size: 14px; cursor: pointer; padding: 0 4px; transition: color 0.2s ease; line-height: 1; }
.close-suggest-btn:hover { color: #ef4444; }
.suggest-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.suggest-tag { padding: 6px 12px; border-radius: 20px; border: 1px solid #f87171; background-color: #fef2f2; color: #ef4444; font-size: 12px; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.suggest-tag:hover { background-color: #ef4444; color: white; transform: translateY(-1px); }
</style>