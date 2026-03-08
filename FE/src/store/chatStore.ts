import { defineStore } from 'pinia';
import {  computed, ref ,watch } from 'vue';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export const useChatStore = defineStore('chatStore', () => {
  const BACKEND_URL = 'http://localhost:2345';
  
  const stompClient = ref<Client | null>(null);
  
  // Khôi phục lịch sử từ LocalStorage
  const sessions = ref<Record<string, any>>(
    JSON.parse(localStorage.getItem('admin_chat_history') || '{}')
  );

  // Lưu Session ID mà Admin ĐANG MỞ
  const currentActiveSessionId = ref<string | null>(null);

  watch(sessions, (newVal) => {
    localStorage.setItem('admin_chat_history', JSON.stringify(newVal));
  }, { deep: true });

  // 🔥 BIẾN QUAN TRỌNG: TỔNG SỐ TIN NHẮN CHƯA ĐỌC TOÀN HỆ THỐNG
  const totalUnread = computed(() => {
    let userCount = 0;
    for (const key in sessions.value) {
      const session = sessions.value[key];
      // Bỏ qua tab AI và Đã đóng
      if (session.status !== 'AI' && session.status !== 'CLOSED') {
        // Cứ ai đang "Chờ nhận" hoặc "Có tin nhắn chưa đọc" thì đếm là 1 người
        if (session.status === 'WAITING' || session.unreadCount > 0) {
          userCount += 1;
        }
      }
    }
    return userCount;
  });

  const handleIncomingMessage = (msg: any) => {
    const sId = msg.sessionId;

    if (!sessions.value[sId]) {
      sessions.value[sId] = {
        id: sId,
        customerName: msg.customer ? msg.customer.fullName || msg.customer.name : 'Khách vãng lai',
        messages: [],
        unreadCount: 0,
        lastMessage: '',
        status: 'AI',
        lastTime: new Date().getTime()
      };
    }

    sessions.value[sId].messages.push(msg);
    sessions.value[sId].lastTime = new Date().getTime();
    if (msg.senderRole !== 'SYSTEM') {
      sessions.value[sId].lastMessage = msg.content;
    }

  const content = msg.content ? String(msg.content).toLowerCase() : '';
    
    const isRequestSupport = 
      content.includes('gặp nhân viên') || 
      content.includes('chat với nhân viên') || 
      content.includes('hệ thống đã kết nối bạn với nhân viên');
    if (isRequestSupport && sessions.value[sId].status === 'AI') sessions.value[sId].status = 'WAITING';
    if (isRequestSupport && sessions.value[sId].status === 'CLOSED') sessions.value[sId].status = 'WAITING';

    // Xử lý tăng số đếm
    if (sessions.value[sId].status !== 'AI') {
      // Nếu Admin KHÔNG CÓ ĐANG MỞ khung chat của người này thì mới tăng số
      if (currentActiveSessionId.value !== sId && msg.senderRole === 'CLIENT') {
        sessions.value[sId].unreadCount++;
      }
    }
  };

  const connectSocket = () => {
    if (stompClient.value && stompClient.value.connected) return;

    const socket = new SockJS(`${BACKEND_URL}/ws`);
    stompClient.value = new Client({
      webSocketFactory: () => socket,
      onConnect: () => {
        // eslint-disable-next-line no-console
        console.log('✅ Đã kết nối Socket Toàn Cầu (Global)!');
        
        stompClient.value?.subscribe('/topic/admin-messages', (message: any) => {
          const msgBody = JSON.parse(message.body);
          handleIncomingMessage(msgBody); 
        });
      },
    });
    stompClient.value.activate();
  };

  return { 
    sessions, 
    stompClient, 
    totalUnread, 
    currentActiveSessionId, 
    connectSocket 
  };
});