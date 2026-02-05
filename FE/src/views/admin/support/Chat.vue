<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from 'axios';

// --- CẤU HÌNH ---
const BACKEND_URL = 'http://localhost:2345'; 

// --- STATE ---
const stompClient = ref(null);
const sessions = ref({}); 
const currentSessionId = ref(null);
const replyText = ref('');
const chatWindowRef = ref(null);

// Lấy session đang chọn
const currentSession = computed(() => currentSessionId.value ? sessions.value[currentSessionId.value] : null);

// --- KẾT NỐI SOCKET ---
const connectSocket = () => {
  const socket = new SockJS(`${BACKEND_URL}/ws`);
  stompClient.value = new Client({
    webSocketFactory: () => socket,
    onConnect: () => {
      console.log('✅ Đã kết nối Socket Admin!');
      stompClient.value.subscribe('/topic/admin-messages', (message) => {
        const msgBody = JSON.parse(message.body);
        handleIncomingMessage(msgBody);
      });
    },
    onStompError: (frame) => console.error('Lỗi Socket:', frame.headers['message'])
  });
  stompClient.value.activate();
};

// --- 🔥 LOGIC LỌC TIN NHẮN QUAN TRỌNG Ở ĐÂY ---
const handleIncomingMessage = (msg) => {
  const sId = msg.sessionId;

  // 1. AI chat thì bỏ qua luôn, không hiện
  if (msg.senderRole === 'AI') return;

  // 2. Nếu session này chưa từng xuất hiện trong danh sách Admin
  if (!sessions.value[sId]) {
    // Kiểm tra xem đây có phải là lời yêu cầu gặp nhân viên không?
    // (Dựa vào nội dung nút bấm bên Client hoặc tin nhắn Hệ thống)
    const content = msg.content ? msg.content.toLowerCase() : '';
    const isRequestSupport = 
        content.includes('gặp nhân viên') || 
        content.includes('chat với nhân viên') ||
        msg.senderRole === 'SYSTEM'; // Hoặc tin nhắn hệ thống báo chuyển line

    // Nếu KHÔNG PHẢI yêu cầu hỗ trợ -> Bỏ qua (Admin không cần biết khách đang chém gió với AI)
    if (!isRequestSupport) return;

    // Nếu ĐÚNG là yêu cầu -> Tạo mới session để hiện lên sidebar
    sessions.value[sId] = {
      id: sId,
      customerName: msg.customer ? msg.customer.name : 'Khách vãng lai',
      messages: [],
      unreadCount: 0,
      lastMessage: ''
    };
  }

  // 3. Nếu đã lọt vào danh sách (đã qua vòng kiểm duyệt ở trên) -> Thêm tin nhắn
  if (sessions.value[sId]) {
      sessions.value[sId].messages.push(msg);
      sessions.value[sId].lastMessage = msg.content;

      if (currentSessionId.value !== sId) {
        sessions.value[sId].unreadCount++;
      } else {
        scrollToBottom();
      }
  }
};

// --- CÁC HÀM CHỨC NĂNG KHÁC (GIỮ NGUYÊN) ---
const selectSession = (sessionId) => {
  currentSessionId.value = sessionId;
  sessions.value[sessionId].unreadCount = 0;
  scrollToBottom();
};

const sendReply = async () => {
  if (!replyText.value.trim() || !currentSessionId.value) return;
  const msgContent = replyText.value;
  const sId = currentSessionId.value;

  sessions.value[sId].messages.push({
    senderRole: 'STAFF',
    content: msgContent,
    createdDate: new Date().getTime()
  });
  
  replyText.value = '';
  scrollToBottom();

  try {
    await axios.post(`${BACKEND_URL}/api/v1/chat/staff/reply`, {
      sessionId: sId,
      message: msgContent,
      staffId: 'NV001' 
    });
  } catch (error) {
    console.error("Lỗi gửi tin:", error);
  }
};

const formatTime = (timestamp) => {
  if (!timestamp) return '';
  return new Date(timestamp).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
};

const scrollToBottom = () => {
  nextTick(() => {
    if (chatWindowRef.value) {
      chatWindowRef.value.scrollTop = chatWindowRef.value.scrollHeight;
    }
  });
};

onMounted(() => {
  connectSocket();
});
</script>

<template>
  <div class="admin-chat-wrapper">
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>💬 Hỗ trợ khách hàng</h3>
      </div>
      <div class="session-list">
        <div 
          v-for="(session, key) in sessions" 
          :key="key"
          class="session-item"
          :class="{ active: currentSessionId === session.id }"
          @click="selectSession(session.id)"
        >
          <div class="avatar-circle">{{ session.customerName.charAt(0).toUpperCase() }}</div>
          <div class="session-info">
            <div class="name-row">
              <span class="name">{{ session.customerName }}</span>
              <span v-if="session.unreadCount > 0" class="badge">{{ session.unreadCount }}</span>
            </div>
            <div class="preview">{{ session.lastMessage }}</div>
          </div>
        </div>
        
        <div v-if="Object.keys(sessions).length === 0" class="empty-list">
          <p>Hiện chưa có yêu cầu hỗ trợ nào.</p>
          <small>(Tin nhắn sẽ hiện khi khách bấm "Gặp nhân viên")</small>
        </div>
      </div>
    </div>

    <div class="chat-main">
      <div v-if="currentSession" class="chat-content">
        <div class="chat-header">
          <div class="user-details">
            <span class="status-dot"></span>
            <h4>{{ currentSession.customerName }}</h4>
            <small>Session: {{ currentSession.id }}</small>
          </div>
        </div>

        <div class="messages-area" ref="chatWindowRef">
          <div 
            v-for="(msg, index) in currentSession.messages" 
            :key="index"
            class="msg-row"
            :class="{ 
              'msg-client': msg.senderRole === 'CLIENT',
              'msg-staff': msg.senderRole === 'STAFF',
              'msg-system': msg.senderRole === 'SYSTEM'
            }"
          >
            <div class="msg-bubble">
              <div class="role-label" v-if="msg.senderRole !== 'STAFF'">
                {{ msg.senderRole === 'CLIENT' ? 'Khách' : 'Hệ thống' }}
              </div>
              <div class="text" v-html="msg.content.replace(/\n/g, '<br>')"></div>
              <div class="time">{{ formatTime(msg.createdDate) }}</div>
            </div>
          </div>
        </div>

        <div class="input-area">
          <input 
            v-model="replyText" 
            @keyup.enter="sendReply"
            type="text" 
            placeholder="Nhập tin nhắn trả lời..." 
          />
          <button @click="sendReply" class="send-btn">Gửi ✈️</button>
        </div>
      </div>

      <div v-else class="welcome-screen">
        <img src="https://cdn-icons-png.flaticon.com/512/1041/1041916.png" width="100" />
        <h3>Chọn một khách hàng để bắt đầu hỗ trợ</h3>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* (Giữ nguyên CSS của bạn, chỉ thêm 1 chút cho phần empty-list) */
.admin-chat-wrapper { display: flex; height: 85vh; background-color: #fff; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; font-family: 'Segoe UI', sans-serif; }
.sidebar { width: 320px; border-right: 1px solid #e0e0e0; background-color: #f8f9fa; display: flex; flex-direction: column; }
.sidebar-header { padding: 20px; border-bottom: 1px solid #e0e0e0; background-color: #fff; }
.sidebar-header h3 { margin: 0; color: #2c3e50; font-size: 18px; }
.session-list { flex: 1; overflow-y: auto; }
.session-item { display: flex; padding: 15px; cursor: pointer; border-bottom: 1px solid #f1f1f1; transition: background 0.2s; }
.session-item:hover { background-color: #e9ecef; }
.session-item.active { background-color: #e8f5e9; border-left: 4px solid #4caf50; }
.avatar-circle { width: 45px; height: 45px; background-color: #ddd; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 18px; color: #555; margin-right: 12px; }
.session-item.active .avatar-circle { background-color: #4caf50; color: white; }
.session-info { flex: 1; overflow: hidden; }
.name-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 5px; }
.name { font-weight: 600; color: #333; }
.preview { font-size: 13px; color: #777; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.badge { background-color: #ff5252; color: white; padding: 2px 6px; border-radius: 10px; font-size: 11px; }
.chat-main { flex: 1; display: flex; flex-direction: column; background-color: #fff; }
.chat-content { display: flex; flex-direction: column; height: 100%; }
.chat-header { padding: 15px 20px; border-bottom: 1px solid #eee; display: flex; align-items: center; background: #fff; box-shadow: 0 2px 5px rgba(0,0,0,0.02); }
.status-dot { width: 10px; height: 10px; background-color: #4caf50; border-radius: 50%; display: inline-block; margin-right: 8px; }
.messages-area { flex: 1; padding: 20px; overflow-y: auto; background-color: #f4f6f8; display: flex; flex-direction: column; gap: 15px; }
.msg-row { display: flex; }
.msg-client { justify-content: flex-start; }
.msg-staff { justify-content: flex-end; }
.msg-system { justify-content: center; }
.msg-bubble { max-width: 70%; padding: 10px 15px; border-radius: 12px; position: relative; box-shadow: 0 1px 2px rgba(0,0,0,0.1); }
.msg-client .msg-bubble { background-color: #fff; border: 1px solid #ddd; border-top-left-radius: 2px; }
.msg-staff .msg-bubble { background-color: #4caf50; color: white; border-top-right-radius: 2px; }
.msg-system .msg-bubble { background-color: transparent; box-shadow: none; color: #888; font-size: 12px; font-style: italic; }
.role-label { font-size: 11px; font-weight: bold; margin-bottom: 4px; color: #666; }
.time { font-size: 10px; margin-top: 5px; opacity: 0.7; text-align: right; }
.input-area { padding: 15px; border-top: 1px solid #eee; display: flex; gap: 10px; background-color: #fff; }
input { flex: 1; padding: 12px; border: 1px solid #ddd; border-radius: 25px; outline: none; font-size: 14px; }
input:focus { border-color: #4caf50; }
.send-btn { background-color: #4caf50; color: white; border: none; padding: 0 20px; border-radius: 25px; cursor: pointer; font-weight: 600; transition: background 0.2s; }
.send-btn:hover { background-color: #43a047; }
.welcome-screen { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #aaa; }
.empty-list { text-align: center; padding: 20px; color: #999; margin-top: 50px; }
</style>