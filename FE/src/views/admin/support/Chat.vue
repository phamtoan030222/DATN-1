<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import axios from 'axios'
import { useAuthStore } from '@/store'
import { storeToRefs } from 'pinia'
import { useChatStore } from '@/store/chatStore' // Import ChatStore vào đây

const BACKEND_URL = 'http://localhost:2345'
const authStore = useAuthStore()
const { userInfoDatn } = storeToRefs(authStore)

// 🔥 1. ĐỒNG BỘ 100% VỚI CHAT STORE (Chìa khóa sửa mọi lỗi)
const chatStore = useChatStore()
const { sessions } = storeToRefs(chatStore) // Lấy sessions từ Store ra dùng chung!

const currentSessionId = ref(null)
const replyText = ref('')
const chatWindowRef = ref(null)
const currentSubTab = ref('WAITING')

const displayedSessions = computed(() => {
  const filtered = {}
  for (const key in sessions.value) {
    if (sessions.value[key].status === currentSubTab.value) {
      filtered[key] = sessions.value[key]
    }
  }
  return filtered
})

const currentSession = computed(() => currentSessionId.value ? sessions.value[currentSessionId.value] : null)

function countByStatus(status) {
  return Object.values(sessions.value).filter(s => s.status === status).length
}

// 🔥 2. KHI ADMIN CLICK XEM CHAT -> BÁO CHO STORE BIẾT ĐỂ NGỪNG NHẢY SỐ ĐỎ
function selectSession(sessionId) {
  currentSessionId.value = sessionId
  chatStore.currentActiveSessionId = sessionId // Đồng bộ ID đang xem lên Store
  sessions.value[sessionId].unreadCount = 0 // Xóa số đỏ ngay lập tức
  scrollToBottom()
}

async function acceptSession() {
  if (!currentSessionId.value)
    return
  const sId = currentSessionId.value
  const staffName = userInfoDatn.value?.fullName || 'Admin'
  const staffCode = userInfoDatn.value?.userCode || 'NV'
  const msgContent = `Nhân viên ${staffName} (Mã: ${staffCode}) đã tiếp nhận hỗ trợ bạn.`

  sessions.value[sId].status = 'ACTIVE'
  await sendSystemAction(sId, msgContent)
  currentSubTab.value = 'ACTIVE'
}

// 🔥 3. KHI ADMIN BẤM KẾT THÚC -> TRỪ SỐ Ở STORE LUÔN
async function closeSession() {
  if (!currentSessionId.value)
    return
  const sId = currentSessionId.value

  const msgContent = `Phiên hỗ trợ đã kết thúc. Trợ lý ảo AI đã quay trở lại để tiếp tục hỗ trợ bạn!`

  sessions.value[sId].status = 'CLOSED'
  sessions.value[sId].unreadCount = 0 // Xóa sạch số đếm ở Store

  await sendSystemAction(sId, msgContent)

  try {
    await axios.post(`${BACKEND_URL}/api/v1/chat/end-support`, {
      sessionId: sId,
    })
  }
  catch (error) {
    console.error('Lỗi khi kết thúc chat:', error)
  }

  currentSessionId.value = null
  chatStore.currentActiveSessionId = null // Bỏ chọn
}

async function sendReply() {
  if (!replyText.value.trim() || !currentSessionId.value)
    return
  const sId = currentSessionId.value
  const msgContent = replyText.value
  await sendSystemAction(sId, msgContent)
  replyText.value = ''
}

async function sendSystemAction(sId, content) {
  const staffId = userInfoDatn.value?.userId || 'NV001'

  sessions.value[sId].messages.push({
    senderRole: 'STAFF',
    content,
    createdDate: new Date().getTime(),
  })
  scrollToBottom()

  try {
    await axios.post(`${BACKEND_URL}/api/v1/chat/staff/reply`, {
      sessionId: sId,
      message: content,
      staffId,
    })
  }
  catch (error) {
    console.error('Lỗi gửi tin:', error)
  }
}

function formatTime(timestamp) {
  if (!timestamp)
    return ''
  const now = Date.now()
  const diff = now - timestamp
  if (diff < 60000)
    return 'Vừa xong'
  if (diff < 3600000)
    return `${Math.floor(diff / 60000)} phút trước`
  return new Date(timestamp).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

function formatMsgTime(timestamp) {
  if (!timestamp)
    return ''
  return new Date(timestamp).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

function scrollToBottom() {
  nextTick(() => {
    if (chatWindowRef.value)
      chatWindowRef.value.scrollTop = chatWindowRef.value.scrollHeight
  })
}
</script>

<template>
  <div class="admin-chat">
    <!-- SIDEBAR -->
    <div class="sidebar">
      <div class="sidebar-head">
        <div class="sidebar-title">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          Quản lý Chat
        </div>

        <div class="tab-bar">
          <button
            class="tab-btn" :class="[{ active: currentSubTab === 'WAITING' }]"
            @click="currentSubTab = 'WAITING'"
          >
            Chờ nhận
            <span v-if="countByStatus('WAITING') > 0" class="badge badge-red">{{ countByStatus('WAITING') }}</span>
          </button>
          <button
            class="tab-btn" :class="[{ active: currentSubTab === 'ACTIVE' }]"
            @click="currentSubTab = 'ACTIVE'"
          >
            Đang xử lý
            <span v-if="countByStatus('ACTIVE') > 0" class="badge badge-green">{{ countByStatus('ACTIVE') }}</span>
          </button>
          <button
            class="tab-btn" :class="[{ active: currentSubTab === 'CLOSED' }]"
            @click="currentSubTab = 'CLOSED'"
          >
            Đã kết thúc
          </button>
        </div>
      </div>

      <div class="session-list">
        <div
          v-for="(session, key) in displayedSessions"
          :key="key"
          class="session-card"
          :class="{ selected: currentSessionId === session.id }"
          @click="selectSession(session.id)"
        >
          <div class="sess-avatar" :class="{ 'sess-avatar--selected': currentSessionId === session.id }">
            {{ session.customerName.charAt(0).toUpperCase() }}
          </div>
          <div class="sess-info">
            <div class="sess-top">
              <span class="sess-name">{{ session.customerName }}</span>
              <span class="sess-time">{{ formatTime(session.lastTime) }}</span>
            </div>
            <div class="sess-bottom">
              <span class="sess-preview">{{ session.lastMessage || 'Chưa có tin nhắn' }}</span>
              <span v-if="session.unreadCount > 0" class="unread-badge">{{ session.unreadCount }}</span>
            </div>
          </div>
        </div>

        <div v-if="Object.keys(displayedSessions).length === 0" class="empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="40" height="40" style="opacity:0.3">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          <p>Không có cuộc trò chuyện nào</p>
        </div>
      </div>
    </div>

    <!-- MAIN CHAT AREA -->
    <div class="chat-main">
      <template v-if="currentSession">
        <!-- CHAT HEADER -->
        <div class="chat-head">
          <div class="chat-head-left">
            <div class="chat-avatar">
              {{ currentSession.customerName.charAt(0).toUpperCase() }}
            </div>
            <div>
              <div class="chat-name">
                {{ currentSession.customerName }}
              </div>
              <div class="chat-status">
                <span v-if="currentSession.status === 'ACTIVE'" class="status-dot dot-green" />
                <span v-if="currentSession.status === 'WAITING'" class="status-dot dot-amber" />
                <span v-if="currentSession.status === 'CLOSED'" class="status-dot dot-gray" />
                <span
                  :class="{
                    'status-label--green': currentSession.status === 'ACTIVE',
                    'status-label--amber': currentSession.status === 'WAITING',
                    'status-label--gray': currentSession.status === 'CLOSED',
                  }"
                >
                  {{ currentSession.status === 'ACTIVE' ? 'Đang xử lý' : currentSession.status === 'WAITING' ? 'Đang chờ tiếp nhận' : 'Đã đóng' }}
                </span>
              </div>
            </div>
          </div>
          <div class="chat-head-meta">
            <span class="msg-count">{{ currentSession.messages.length }} tin nhắn</span>
          </div>
        </div>

        <!-- MESSAGES -->
        <div ref="chatWindowRef" class="messages-area">
          <div
            v-for="(msg, index) in currentSession.messages"
            :key="index"
            class="msg-wrap"
            :class="{
              'is-client': msg.senderRole === 'CLIENT',
              'is-staff': msg.senderRole === 'STAFF',
              'is-system': msg.senderRole === 'SYSTEM',
              'is-ai': msg.senderRole === 'AI',
            }"
          >
            <!-- SYSTEM -->
            <div v-if="msg.senderRole === 'SYSTEM'" class="sys-divider">
              <div class="sys-line" />
              <span class="sys-text">{{ msg.content }}</span>
              <div class="sys-line" />
            </div>

            <!-- CLIENT -->
            <template v-else-if="msg.senderRole === 'CLIENT'">
              <div class="msg-avatar msg-avatar--client">
                 {{ currentSession.customerName.charAt(0).toUpperCase() }}
              </div>
              <div class="msg-body">
                <div class="role-tag">
                   {{ currentSession.customerName }}
                </div>
                <div class="bubble bubble--client" v-html="msg.content.replace(/\n/g, '<br>')" />
                <div class="msg-time">
                  {{ formatMsgTime(msg.createdDate) }}
                </div>
              </div>
            </template>

            <!-- AI -->
            <template v-else-if="msg.senderRole === 'AI'">
              <div class="msg-avatar msg-avatar--ai">
                <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
                  <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z" />
                </svg>
              </div>
              <div class="msg-body">
                <div class="role-tag">
                  Trợ lý AI
                </div>
                <div class="bubble bubble--ai" v-html="msg.content.replace(/\n/g, '<br>')" />
                <div class="msg-time">
                  {{ formatMsgTime(msg.createdDate) }}
                </div>
              </div>
            </template>

            <!-- STAFF -->
            <template v-else-if="msg.senderRole === 'STAFF'">
              <div class="msg-body msg-body--right">
                <div class="role-tag role-tag--right">
                  Nhân viên
                </div>
                <div class="bubble bubble--staff" v-html="msg.content.replace(/\n/g, '<br>')" />
                <div class="msg-time msg-time--right">
                  {{ formatMsgTime(msg.createdDate) }}
                </div>
              </div>
              <div class="msg-avatar msg-avatar--staff">
                <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
                  <path d="M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v2.4h19.2v-2.4c0-3.2-6.4-4.8-9.6-4.8z" />
                </svg>
              </div>
            </template>
          </div>
        </div>

        <!-- INPUT SECTION -->
        <div class="input-section">
          <!-- WAITING: accept button -->
          <div v-if="currentSession.status === 'WAITING'" class="accept-bar">
            <div class="accept-info">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18" style="color:#f59e0b">
                <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              Khách hàng đang chờ được hỗ trợ
            </div>
            <button class="btn-accept" @click="acceptSession">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="16" height="16">
                <polyline points="20 6 9 17 4 12" />
              </svg>
              Tiếp nhận
            </button>
          </div>

          <!-- ACTIVE: reply input -->
          <div v-if="currentSession.status === 'ACTIVE'" class="active-bar">
            <div class="active-top">
              <button class="btn-close-session" @click="closeSession">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                  <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                </svg>
                Kết thúc phiên
              </button>
            </div>
            <div class="active-input">
              <input
                v-model="replyText"
                type="text"
                placeholder="Nhập phản hồi của bạn..."
                @keyup.enter="sendReply"
              >
              <button class="btn-send" :disabled="!replyText.trim()" @click="sendReply">
                <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                  <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
                </svg>
              </button>
            </div>
          </div>

          <!-- CLOSED -->
          <div v-if="currentSession.status === 'CLOSED'" class="closed-bar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
            Phiên chat đã kết thúc — chỉ xem lịch sử
          </div>
        </div>
      </template>

      <!-- EMPTY STATE -->
      <div v-else class="no-session">
        <div class="no-session-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="52" height="52">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
        </div>
        <h3>Chọn cuộc trò chuyện</h3>
        <p>Chọn một phiên từ danh sách bên trái để bắt đầu hỗ trợ</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== LAYOUT ===== */
.admin-chat {
  display: flex;
  height: 88vh;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06);
}

/* ===== SIDEBAR ===== */
.sidebar {
  width: 320px;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  background: #fff;
  flex-shrink: 0;
}

.sidebar-head {
  padding: 18px 16px 0;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 14px;
}

.tab-bar {
  display: flex;
  gap: 2px;
  margin-bottom: -1px;
}

.tab-btn {
  flex: 1;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  padding: 9px 4px;
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  transition: all 0.2s;
  white-space: nowrap;
  font-family: inherit;
}
.tab-btn:hover { color: #111827; }
.tab-btn.active { color: #10b981; border-bottom-color: #10b981; }

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  font-size: 10px;
  font-weight: 700;
  color: white;
}
.badge-red { background: #ef4444; }
.badge-green { background: #10b981; }

/* SESSION LIST */
.session-list {
  flex: 1;
  overflow-y: auto;
}
.session-list::-webkit-scrollbar { width: 4px; }
.session-list::-webkit-scrollbar-thumb { background: #e5e7eb; border-radius: 4px; }

.session-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f3f4f6;
  transition: background 0.15s;
}
.session-card:hover { background: #f9fafb; }
.session-card.selected {
  background: #f0fdf4;
  border-left: 3px solid #10b981;
  padding-left: 13px;
}

.sess-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e5e7eb;
  color: #4b5563;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
  flex-shrink: 0;
  transition: all 0.2s;
}
.sess-avatar--selected {
  background: #10b981;
  color: white;
}

.sess-info { flex: 1; min-width: 0; }
.sess-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 3px; }
.sess-name { font-size: 13.5px; font-weight: 600; color: #111827; }
.sess-time { font-size: 10.5px; color: #9ca3af; white-space: nowrap; }
.sess-bottom { display: flex; justify-content: space-between; align-items: center; }
.sess-preview { font-size: 12px; color: #6b7280; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; flex: 1; }
.unread-badge {
  background: #ef4444;
  color: white;
  font-size: 10px;
  font-weight: 700;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
  margin-left: 6px;
  flex-shrink: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  color: #6b7280;
  gap: 10px;
}
.empty-state p { font-size: 13px; text-align: center; margin: 0; }

/* ===== MAIN AREA ===== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* CHAT HEAD */
.chat-head {
  padding: 14px 20px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  flex-shrink: 0;
}
.chat-head-left { display: flex; align-items: center; gap: 12px; }
.chat-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #d1fae5;
  color: #059669;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
  flex-shrink: 0;
}
.chat-name { font-size: 14px; font-weight: 700; color: #111827; }
.chat-status { display: flex; align-items: center; gap: 5px; margin-top: 2px; }
.status-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.dot-green { background: #10b981; }
.dot-amber { background: #f59e0b; }
.dot-gray { background: #9ca3af; }
.status-label--green { font-size: 11.5px; font-weight: 600; color: #10b981; }
.status-label--amber { font-size: 11.5px; font-weight: 600; color: #f59e0b; }
.status-label--gray { font-size: 11.5px; font-weight: 600; color: #9ca3af; }
.msg-count { font-size: 11.5px; color: #9ca3af; background: #f9fafb; padding: 3px 10px; border-radius: 20px; }
.chat-head-meta { display: flex; align-items: center; gap: 8px; }

/* MESSAGES */
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f9fafb;
  display: flex;
  flex-direction: column;
  gap: 14px;
  scroll-behavior: smooth;
}
.messages-area::-webkit-scrollbar { width: 5px; }
.messages-area::-webkit-scrollbar-thumb { background: #e5e7eb; border-radius: 4px; }

.msg-wrap { display: flex; align-items: flex-end; gap: 8px; }
.msg-wrap.is-system { justify-content: center; }
.msg-wrap.is-staff { flex-direction: row-reverse; }

/* SYSTEM DIVIDER */
.sys-divider {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.sys-line { flex: 1; height: 1px; background: #e5e7eb; }
.sys-text { font-size: 11px; color: #9ca3af; font-style: italic; white-space: nowrap; }

/* AVATARS */
.msg-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  margin-bottom: 2px;
}
.msg-avatar--client { background: #e5e7eb; color: #4b5563; font-size: 13px; }
.msg-avatar--ai { background: #dbeafe; color: #2563eb; }
.msg-avatar--staff { background: #10b981; color: white; }

/* MSG BODY */
.msg-body { display: flex; flex-direction: column; max-width: 66%; }
.msg-body--right { align-items: flex-end; }

.role-tag { font-size: 10px; font-weight: 600; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.04em; margin-bottom: 3px; }
.role-tag--right { text-align: right; }

.bubble {
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 13.5px;
  line-height: 1.55;
  word-break: break-word;
}
.bubble--client {
  background: #fff;
  color: #111827;
  border: 1px solid #e5e7eb;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.bubble--ai {
  background: #eff6ff;
  color: #1e40af;
  border: 1px solid #bfdbfe;
  border-bottom-left-radius: 4px;
}
.bubble--staff {
  background: #10b981;
  color: white;
  border-bottom-right-radius: 4px;
}

.msg-time { font-size: 10px; color: #9ca3af; margin-top: 3px; padding: 0 2px; }
.msg-time--right { text-align: right; }

/* ===== INPUT SECTION ===== */
.input-section {
  border-top: 1px solid #e5e7eb;
  background: #fff;
  flex-shrink: 0;
}

/* ACCEPT BAR */
.accept-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: #fffbeb;
  gap: 12px;
}
.accept-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #92400e;
}
.btn-accept {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #10b981;
  color: white;
  border: none;
  padding: 9px 18px;
  border-radius: 8px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  font-family: inherit;
}
.btn-accept:hover { background: #059669; transform: translateY(-1px); }

/* ACTIVE BAR */
.active-bar { display: flex; flex-direction: column; }
.active-top {
  padding: 8px 16px;
  border-bottom: 1px solid #f3f4f6;
  display: flex;
  justify-content: flex-end;
  background: #fafafa;
}
.btn-close-session {
  display: flex;
  align-items: center;
  gap: 5px;
  background: none;
  border: 1px solid #fca5a5;
  color: #ef4444;
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-close-session:hover { background: #fef2f2; }

.active-input {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
}
.active-input input {
  flex: 1;
  padding: 10px 16px;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  outline: none;
  font-size: 13.5px;
  background: #f9fafb;
  font-family: inherit;
  color: #111827;
  transition: all 0.2s;
}
.active-input input:focus {
  border-color: #10b981;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(16,185,129,0.1);
}
.active-input input::placeholder { color: #9ca3af; }

.btn-send {
  width: 40px;
  height: 40px;
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
.btn-send:hover:not(:disabled) { background: #059669; transform: scale(1.05); }
.btn-send:disabled { background: #d1d5db; cursor: not-allowed; }

/* CLOSED BAR */
.closed-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 16px;
  font-size: 13px;
  color: #9ca3af;
  font-style: italic;
}

/* NO SESSION */
.no-session {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #9ca3af;
  background: #f9fafb;
}
.no-session-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6px;
}
.no-session h3 { margin: 0; font-size: 16px; font-weight: 600; color: #4b5563; }
.no-session p { margin: 0; font-size: 13px; text-align: center; max-width: 240px; }
</style>
