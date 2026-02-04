import axios from "axios";

export const sendMessageToAi = async (content: string , customerId? : string) =>{
    const payload = {
        sessionId: sessionStorage.getItem('chat_id') || 'session-123',
        message: content,
        customerId: customerId || null,
        staffId: "id-nhan-vien-mac-dinh"
    };

    const response = await axios.post('http://localhost:8080/api/v1/chat/ask', payload);
    return response.data;
}