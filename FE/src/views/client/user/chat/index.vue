<script setup>
import { ref, nextTick } from 'vue';
import axios from 'axios';

// --- LOGIC CHAT ---
const isOpen = ref(false); 
const messages = ref([
  { sender: 'AI', content: 'Xin chào! Tôi có thể giúp gì cho bạn về Laptop?' }
]);
const userMessage = ref('');
const isLoading = ref(false);
const messagesContainer = ref(null);
const sessionId = ref('session-' + Math.random().toString(36).substr(2, 9));

// --- 1. HÀM XỬ LÝ ĐỊNH DẠNG (MỚI) ---
const formatMessage = (text) => {
  if (!text) return '';
  // Chuyển đổi xuống dòng và in đậm
  return text
    .replace(/\n/g, '<br>')                   // \n thành xuống dòng
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>'); // **text** thành in đậm
};

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  });
};

const sendMessage = async () => {
  if (!userMessage.value.trim()) return;

  const text = userMessage.value;
  messages.value.push({ sender: 'USER', content: text });
  userMessage.value = '';
  isLoading.value = true;
  scrollToBottom();

  try {
    const response = await axios.post('http://localhost:2345/api/v1/chat/ask', {
      sessionId: sessionId.value,
      message: text
    });
    messages.value.push({ sender: 'AI', content: response.data });
  } catch (error) {
    console.error(error);
    messages.value.push({ sender: 'AI', content: 'Lỗi kết nối Server 😢' });
  } finally {
    isLoading.value = false;
    scrollToBottom();
  }
};

const toggleChat = () => {
  isOpen.value = !isOpen.value;
  if (isOpen.value) scrollToBottom();
};
</script>

<template>
  <div class="chat-widget-wrapper">
    <transition name="slide-fade">
      <div v-if="isOpen" class="chat-container">
        <div class="chat-header">
          <h3>🤖 Trợ lý MyLaptop</h3>
          <button class="close-btn" @click="toggleChat">✖</button>
        </div>

        <div class="messages-box" ref="messagesContainer">
          <div 
            v-for="(msg, index) in messages" 
            :key="index" 
            :class="['message', msg.sender === 'USER' ? 'my-message' : 'ai-message']"
          >
            <div class="bubble" v-html="formatMessage(msg.content)"></div>
          </div>
          
          <div v-if="isLoading" class="message ai-message">
            <div class="bubble loading">
              <span>.</span><span>.</span><span>.</span>
            </div>
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
          <button @click="sendMessage" :disabled="isLoading || !userMessage.trim()">
            ➤
          </button>
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
/* Container bao ngoài để định vị */
.chat-widget-wrapper {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 9999; 
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

/* Nút tròn bật tắt */
.toggle-chat-btn {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background-color: #049d14; /* Giữ nguyên màu xanh của bạn */
  color: white;
  border: none;
  font-size: 30px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
  transition: transform 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 10px;
}
.toggle-chat-btn:hover {
  transform: scale(1.1);
}

/* Khung chat chính */
.chat-container {
  width: 350px;
  height: 500px;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 5px 20px rgba(0,0,0,0.2);
  display: flex;
  flex-direction: column;
  border: 1px solid #eee;
  overflow: hidden;
}

/* Header */
.chat-header {
  padding: 15px;
  background-color: #049d14; /* Giữ nguyên màu xanh */
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chat-header h3 {
  margin: 0;
  font-size: 16px;
}
.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 18px;
  cursor: pointer;
}

/* Phần tin nhắn */
.messages-box {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
  background-color: #f9f9f9;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message { display: flex; width: 100%; }
.my-message { justify-content: flex-end; }
.ai-message { justify-content: flex-start; }

.bubble {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.5; /* Tăng khoảng cách dòng một chút cho dễ đọc */
  word-wrap: break-word;
}
.my-message .bubble {
  background-color: #049d14; /* Giữ nguyên màu xanh */
  color: white;
  border-bottom-right-radius: 2px;
}
.ai-message .bubble {
  background-color: #e4e6eb;
  color: black;
  border-bottom-left-radius: 2px;
}

/* Input area */
.input-area {
  padding: 10px;
  border-top: 1px solid #ddd;
  background-color: white;
  display: flex;
  gap: 8px;
}
input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 20px;
  outline: none;
}
input:focus { border-color: #049d14; }

.input-area button {
  background-color: #049d14;
  color: white;
  border: none;
  padding: 0 15px;
  border-radius: 20px;
  cursor: pointer;
}
.input-area button:disabled { background-color: #ccc; }

/* Hiệu ứng trượt */
.slide-fade-enter-active, .slide-fade-leave-active {
  transition: all 0.3s ease;
}
.slide-fade-enter-from, .slide-fade-leave-to {
  transform: translateY(20px);
  opacity: 0;
}

/* Animation loading */
.loading span {
  animation: blink 1.4s infinite both;
  font-size: 20px;
  line-height: 10px;
}
.loading span:nth-child(2) { animation-delay: 0.2s; }
.loading span:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink { 0% { opacity: 0.2; } 20% { opacity: 1; } 100% { opacity: 0.2; } }

:deep(strong) {
  font-weight: bold;
  color: #000; 
}
</style>