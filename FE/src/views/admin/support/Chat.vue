<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from 'axios';
import { useAuthStore } from '@/store'; 
import { storeToRefs } from 'pinia';

const authStore = useAuthStore();
const { userInfoDatn } = storeToRefs(authStore);

// --- CẤU HÌNH ---
const BACKEND_URL = 'http://localhost:2345'; 

// --- STATE ---
const stompClient = ref(null);
const sessions = ref({}); 
const currentSessionId = ref(null);
const replyText = ref('');
const chatWindowRef = ref(null);



// 🔥 LỌC SESSION: Chỉ hiện những người đã yêu cầu gặp nhân viên
const visibleSessions = computed(() => {
  const filtered = {};
  for (const key in sessions.value) {
    if (sessions.value[key].needsSupport) {
      filtered[key] = sessions.value[key];
    }
  }
  return filtered;
});

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

// --- 🔥 LOGIC NGHE LÉN & HIỂN THỊ ---
const handleIncomingMessage = (msg) => {
  const sId = msg.sessionId;

  // 1. LUÔN TẠO VÀ LƯU TRỮ LỊCH SỬ NGẦM (Dù đang chat với AI)
  if (!sessions.value[sId]) {
    sessions.value[sId] = {
      id: sId,
      customerName: msg.customer ? msg.customer.name : 'Khách cần hỗ trợ 🆘',
      messages: [],
      unreadCount: 0,
      lastMessage: '',
      needsSupport: false // Mặc định là ẨN
    };
  }

  // Lưu tin nhắn vào lịch sử
  sessions.value[sId].messages.push(msg);
  if (msg.senderRole !== 'SYSTEM') {
      sessions.value[sId].lastMessage = msg.content;
  }

  // 2. KIỂM TRA ĐIỀU KIỆN "BUNG" LỊCH SỬ
  const content = msg.content ? msg.content.toLowerCase() : '';
  const isRequestSupport = 
      content.includes('gặp nhân viên') || 
      content.includes('chat với nhân viên') ||
      msg.senderRole === 'SYSTEM'; 

  if (isRequestSupport) {
      sessions.value[sId].needsSupport = true; // Bật cờ -> Hiện lên danh sách bên trái
  }

  // 3. XỬ LÝ SỐ TIN NHẮN CHƯA ĐỌC
  // Chỉ báo tin mới khi Session đã hiển thị, tin nhắn là của khách, và không phải session đang mở
  if (sessions.value[sId].needsSupport) {
      if (currentSessionId.value !== sId && msg.senderRole === 'CLIENT') {
        sessions.value[sId].unreadCount++;
      } else if (currentSessionId.value === sId) {
        scrollToBottom();
      }
  }
};

const selectSession = (sessionId) => {
  currentSessionId.value = sessionId;
  sessions.value[sessionId].unreadCount = 0;
  scrollToBottom();
};

const acceptSession = async () => {
  if (!currentSessionId.value) return;
  const sId = currentSessionId.value;

  // Lấy thông tin nhân viên từ Store
  const staffName = userInfoDatn.value?.fullName || 'Admin';
  const staffCode = userInfoDatn.value?.userCode || 'NV';
  const staffId = userInfoDatn.value?.userId || 'NV001';

  // Tạo câu thông báo chuẩn
  const msgContent = `Nhân viên ${staffName} (Mã: ${staffCode}) đã tiếp nhận hỗ trợ bạn.`;

  sessions.value[sId].messages.push({
    senderRole: 'STAFF',
    content: msgContent,
    createdDate: new Date().getTime()
  });
  
  sessions.value[sId].needsSupport = false; // Đã tiếp nhận thì có thể tắt cờ cảnh báo (tuỳ ý)
  scrollToBottom();

  try {
    // Gọi API lưu tin nhắn
    await axios.post(`${BACKEND_URL}/api/v1/chat/staff/reply`, {
      sessionId: sId,
      message: msgContent,
      staffId: staffId 
    });
  } catch (error) {
    console.error("Lỗi gửi tin:", error);
  }
};

// Sửa lại hàm sendReply để lấy ID nhân viên thật
const sendReply = async () => {
  if (!replyText.value.trim() || !currentSessionId.value) return;
  const msgContent = replyText.value;
  const sId = currentSessionId.value;
  const staffId = userInfoDatn.value?.userId || 'NV001'; // Lấy ID thật

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
      staffId: staffId 
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
          v-for="(session, key) in visibleSessions" 
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
        
        <div v-if="Object.keys(visibleSessions).length === 0" class="empty-list">
          <p>Hiện chưa có yêu cầu hỗ trợ nào.</p>
          <small>(Sẽ tự động hiển thị khi khách bấm "Gặp nhân viên")</small>
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
              'msg-system': msg.senderRole === 'SYSTEM',
              'msg-ai': msg.senderRole === 'AI' 
            }"
          >
            <div class="msg-bubble">
              <div class="role-label" v-if="msg.senderRole !== 'STAFF'">
                {{ msg.senderRole === 'CLIENT' ? 'Khách' : (msg.senderRole === 'AI' ? 'Bot 🤖' : 'Hệ thống') }}
              </div>
              <div class="text" v-html="msg.content.replace(/\n/g, '<br>')"></div>
              <div class="time">{{ formatTime(msg.createdDate) }}</div>
            </div>
          </div>
        </div>

        <div class="action-bar" style="padding: 10px 24px; background: #fff; border-top: 1px solid #eee;">
            <button @click="acceptSession" class="accept-btn">
              👋 Gửi thông báo Tiếp nhận hỗ trợ
            </button>
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
/* (Giữ nguyên CSS cũ của bạn, mình chỉ bổ sung thêm class cho AI) */
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
.msg-ai { justify-content: flex-start; }
.msg-system { justify-content: center; width: 100%; }
.msg-bubble { max-width: 70%; padding: 10px 15px; border-radius: 12px; position: relative; box-shadow: 0 1px 2px rgba(0,0,0,0.1); }

/* Màu sắc các loại bong bóng chat */
.msg-client .msg-bubble { background-color: #fff; border: 1px solid #ddd; border-top-left-radius: 2px; }
.msg-staff .msg-bubble { background-color: #4caf50; color: white; border-top-right-radius: 2px; }
.msg-ai .msg-bubble { background-color: #e3f2fd; border: 1px solid #bbdefb; border-top-left-radius: 2px; }
.msg-system .msg-bubble { background-color: transparent; box-shadow: none; color: #888; font-size: 12px; font-style: italic; text-align: center; }

.role-label { font-size: 11px; font-weight: bold; margin-bottom: 4px; color: #666; }
.msg-staff .role-label { color: #e8f5e9; }
.time { font-size: 10px; margin-top: 5px; opacity: 0.7; text-align: right; }
.input-area { padding: 15px; border-top: 1px solid #eee; display: flex; gap: 10px; background-color: #fff; }
input { flex: 1; padding: 12px; border: 1px solid #ddd; border-radius: 25px; outline: none; font-size: 14px; }
input:focus { border-color: #4caf50; }
.send-btn { background-color: #4caf50; color: white; border: none; padding: 0 20px; border-radius: 25px; cursor: pointer; font-weight: 600; transition: background 0.2s; }
.send-btn:hover { background-color: #43a047; }
.welcome-screen { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #aaa; }
.empty-list { text-align: center; padding: 20px; color: #999; margin-top: 50px; }
.accept-btn {
  background-color: #e0f2fe;
  color: #0284c7;
  border: 1px solid #bae6fd;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.accept-btn:hover { background-color: #bae6fd; }
</style>