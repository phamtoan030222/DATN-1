package com.sd20201.datn.core.client.chatMessage.chat.service;

import com.sd20201.datn.core.admin.customer.repository.AdCustomerRepository;
import com.sd20201.datn.core.admin.staff.repository.ADStaffRepository;
import com.sd20201.datn.core.client.chatMessage.chat.model.request.ChatRequest;
import com.sd20201.datn.core.client.chatMessage.chat.repository.AdChatMessageRepository;
import com.sd20201.datn.core.client.products.productdetail.model.request.ClientPDProductDetailRequest;
import com.sd20201.datn.core.client.products.productdetail.model.response.ClientPDProductDetailResponse;
import com.sd20201.datn.core.client.products.productdetail.repository.ClientPDProductDetailDiscountRepository;
import com.sd20201.datn.core.client.products.productdetail.repository.ClientPDProductDetailRepository;
import com.sd20201.datn.entity.ChatMessage;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.Staff;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl {

    private final AdChatMessageRepository chatMessageRepository;
    private final ADStaffRepository staffRepository;
    private final AdCustomerRepository customerRepository;
    private final GeminiService geminiService;
    private final ClientPDProductDetailRepository productDetailRepository;
    private final ClientPDProductDetailDiscountRepository discountRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Map lưu trạng thái: Session nào đang chat với Nhân viên
    private static final Map<String, Boolean> humanChatSessions = new ConcurrentHashMap<>();

    // Danh sách từ khóa để kích hoạt nhân viên
    private static final List<String> TRIGGER_KEYWORDS = Arrays.asList(
            "gặp nhân viên",
            "chat với nhân viên",
            "tư vấn viên",
            "gặp người",
            "chat với người",
            "gặp admin",
            "nhân viên hỗ trợ"
    );

    @Transactional
    public String chatWithAi(ChatRequest request) {
        Customer customer = request.getCustomerId() != null ?
                customerRepository.findById(request.getCustomerId()).orElse(null) : null;

        // 1. LƯU TIN NHẮN KHÁCH HÀNG
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(request.getSessionId());
        userMsg.setContent(request.getMessage());
        userMsg.setSenderRole("CLIENT");
        userMsg.setCustomer(customer);
        userMsg.setCreatedDate(System.currentTimeMillis());
        chatMessageRepository.save(userMsg);

        // 2. GỬI SOCKET CHO ADMIN (Để admin thấy tin nhắn khách đang chat)
        try {
            messagingTemplate.convertAndSend("/topic/admin-messages", userMsg);
        } catch (Exception e) {
            log.error("Lỗi gửi socket admin: {}", e.getMessage());
        }

        // 3. KIỂM TRA: CÓ ĐANG Ở CHẾ ĐỘ GẶP NHÂN VIÊN KHÔNG?
        if (humanChatSessions.getOrDefault(request.getSessionId(), false)) {
            // Nếu đã bật chế độ nhân viên -> AI IM LẶNG
            return "Đang chờ nhân viên phản hồi...";
        }

        // 4. TỰ ĐỘNG PHÁT HIỆN YÊU CẦU GẶP NHÂN VIÊN
        String messageContent = request.getMessage().toLowerCase();
        boolean wantHuman = TRIGGER_KEYWORDS.stream().anyMatch(messageContent::contains);

        if (wantHuman) {
            // Kích hoạt chế độ nhân viên ngay lập tức
            enableHumanSupport(request.getSessionId());

            // Thông báo cho khách biết
            String response = "Hệ thống đã kết nối bạn với nhân viên tư vấn. Vui lòng chờ trong giây lát...";

            // Lưu tin nhắn hệ thống
            ChatMessage sysMsg = new ChatMessage();
            sysMsg.setSessionId(request.getSessionId());
            sysMsg.setContent(response);
            sysMsg.setSenderRole("SYSTEM");
            sysMsg.setCreatedDate(System.currentTimeMillis());
            chatMessageRepository.save(sysMsg);

            // Bắn socket báo cho khách (để hiện tin nhắn này lên)
            messagingTemplate.convertAndSend("/topic/user/" + request.getSessionId(), sysMsg);

            return response;
        }

        // 5. NẾU KHÔNG GẶP NHÂN VIÊN -> GỌI AI (GEMINI RAG)
        String aiResponse = callGeminiRAG(request, customer);

        // Lưu tin nhắn AI
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(request.getSessionId());
        aiMsg.setContent(aiResponse);
        aiMsg.setSenderRole("AI");
        aiMsg.setCustomer(customer);
        aiMsg.setIsAiReplied(true);
        aiMsg.setCreatedDate(System.currentTimeMillis());
        chatMessageRepository.save(aiMsg);

        // Bắn socket về cho khách
        messagingTemplate.convertAndSend("/topic/user/" + request.getSessionId(), aiMsg);

        return aiResponse;
    }

    private String callGeminiRAG(ChatRequest request, Customer customer) {
        ClientPDProductDetailRequest productReq = new ClientPDProductDetailRequest();
        // Đảm bảo lấy được toàn bộ dải giá
        productReq.setMinPrice(0);
        productReq.setMaxPrice(1000000000);

        Pageable pageable = PageRequest.of(0, 25);
        Long currentTime = System.currentTimeMillis();

        List<String> idCurrentDiscounts = discountRepository.getIdByDate(currentTime);
        Page<ClientPDProductDetailResponse> pageResult;

        if (idCurrentDiscounts != null && !idCurrentDiscounts.isEmpty()) {
            pageResult = productDetailRepository.getProductDetailsDiscount(pageable, productReq, idCurrentDiscounts, currentTime);
        } else {
            pageResult = productDetailRepository.getProductDetails(pageable, productReq);
        }

        StringBuilder sb = new StringBuilder();
        if (pageResult != null && pageResult.hasContent()) {
            for (ClientPDProductDetailResponse p : pageResult.getContent()) {
                BigDecimal originalPrice = (p.getPrice() != null) ? BigDecimal.valueOf(p.getPrice()) : BigDecimal.ZERO;

                // 1. Tính toán giá khuyến mãi
                String discountNote = "";
                if (p.getPercentage() != null && p.getPercentage() > 0) {
                    BigDecimal discountPercent = BigDecimal.valueOf(p.getPercentage());
                    BigDecimal discountedPrice = originalPrice.multiply(BigDecimal.valueOf(100).subtract(discountPercent))
                            .divide(BigDecimal.valueOf(100));

                    discountNote = String.format(" >>> [SIÊU SALE %d%%] GIÁ CHỈ CÒN: %s",
                            p.getPercentage(), formatMoney(discountedPrice));
                }

                // 2. Thu thập đầy đủ thuộc tính từ API (Dựa trên ảnh bạn gửi)
                sb.append(String.format("""
                * LAPTOP: %s (Model: %s)
                  - Thương hiệu: %s
                  - Giá niêm yết: %s %s
                  - Màu sắc: %s
                  - Chất liệu vỏ: %s
                  - Cấu hình chi tiết:
                    + CPU: %s
                    + RAM: %s
                    + Card đồ họa (GPU): %s
                    + Ổ cứng: %s
                    + Màn hình: %s
                    + Pin: %s
                    + Hệ điều hành: %s
                  - Kho hàng: Còn %d máy sẵn sàng giao ngay.
                """,
                        p.getProductName(), p.getName(),
                        p.getBrandName(),
                        formatMoney(originalPrice), discountNote,
                        p.getColor(),
                        p.getMaterial(),
                        p.getCpu(), p.getRam(), p.getGpu(), p.getHardDrive(), p.getScreenName(),
                        p.getBatteryName(),
                        p.getOperatingSystemName(),
                        p.getQuantity() != null ? p.getQuantity() : 0));
                sb.append("--------------------------\n");
            }
        }

        String productContext = sb.length() > 0 ? sb.toString() : "Hiện không có sản phẩm nào phù hợp trong kho.";

        // Log để bạn kiểm tra nội dung gửi đi có "màu" chưa
        log.info("CONTEST GỬI GEMINI: \n{}", productContext);

        String customerName = (customer != null) ? customer.getName() : "khách hàng";

        // 3. System Instruction: Dạy AI cách dùng các thuộc tính mới
        String systemInstruction = """
        VAI TRÒ: Bạn là chuyên gia tư vấn Laptop nhiệt tình.
        KHÁCH HÀNG: %s
        
        DỮ LIỆU KHO HÀNG CỦA BẠN:
        %s
        
        HƯỚNG DẪN TƯ VẤN:
        1. NGOẠI HÌNH: Nếu khách hỏi về vẻ ngoài, hãy dùng thông tin 'Màu sắc' và 'Chất liệu' (ví dụ: vỏ nhôm sang trọng, màu trắng thanh lịch).
        2. ƯU ĐÃI: Luôn ưu tiên giới thiệu các máy có [SIÊU SALE] vì giá đang rất tốt.
        3. CHUYÊN MÔN: Tư vấn máy dựa trên cấu hình (RAM, CPU, GPU). Nếu khách làm đồ họa, ưu tiên máy có GPU mạnh.
        4. PHONG CÁCH: Trả lời thân thiện, xưng 'em', gọi 'anh/chị'.
        5. CHỈ tư vấn sản phẩm có trong danh sách. Không bịa đặt.
        6. Nếu có giảm giá, hãy nhắc khách mua ngay.
        7. Báo giá rõ ràng.
        8. Nếu khách hỏi vấn đề quá khó hoặc khiếu nại, hãy bảo khách nhắn cú pháp: "gặp nhân viên" để được hỗ trợ.
        """.formatted(customerName, productContext);

        return geminiService.callGemini(systemInstruction + "\n\nCÂU HỎI CỦA KHÁCH: " + request.getMessage());
    }


    // --- HÀM HỖ TRỢ ---

    public void enableHumanSupport(String sessionId) {
        humanChatSessions.put(sessionId, true);
        log.info("Session {} đã chuyển sang chế độ nhân viên", sessionId);
    }

    public void disableHumanSupport(String sessionId) {
        humanChatSessions.remove(sessionId);
    }

    public void staffReply(String sessionId, String message, String staffId) {
        Staff staff = null;
        if (staffId != null) {
            staff = staffRepository.findById(staffId).orElse(null);
        }

        ChatMessage staffMsg = new ChatMessage();
        staffMsg.setSessionId(sessionId);
        staffMsg.setContent(message);
        staffMsg.setSenderRole("STAFF");
        staffMsg.setStaff(staff);
        staffMsg.setCreatedDate(System.currentTimeMillis());
        chatMessageRepository.save(staffMsg);

        messagingTemplate.convertAndSend("/topic/user/" + sessionId, staffMsg);
    }

    private String formatMoney(BigDecimal price) {
        if (price == null) return "Liên hệ";
        return String.format("%,.0f VNĐ", price);
    }
}