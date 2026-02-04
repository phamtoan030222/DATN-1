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
        Pageable pageable = PageRequest.of(0, 20);

        Long currentTime = System.currentTimeMillis();
        List<String> idCurrentDiscounts = discountRepository.getIdByDate(currentTime);
        Page<ClientPDProductDetailResponse> pageResult;

        if (!idCurrentDiscounts.isEmpty()) {
            pageResult = productDetailRepository.getProductDetailsDiscount(pageable, productReq, idCurrentDiscounts, currentTime);
        } else {
            pageResult = productDetailRepository.getProductDetails(pageable, productReq);
        }

        String productContext = "Hiện tại kho đang chưa cập nhật sản phẩm.";
        if (pageResult != null && pageResult.hasContent()) {
            productContext = pageResult.getContent().stream()
                    .map(p -> {
                        BigDecimal originalPrice = (p.getPrice() != null) ? BigDecimal.valueOf(p.getPrice()) : BigDecimal.ZERO;
                        String priceStr = formatMoney(originalPrice);
                        String discountInfo = "";
                        if (p.getPercentage() != null && p.getPercentage() > 0) {
                            BigDecimal discountPercent = BigDecimal.valueOf(p.getPercentage());
                            BigDecimal discountedPrice = originalPrice.multiply(BigDecimal.valueOf(100).subtract(discountPercent)).divide(BigDecimal.valueOf(100));
                            discountInfo = String.format(" (🔥 GIẢM %d%% còn %s)", p.getPercentage(), formatMoney(discountedPrice));
                        }
                        return String.format("- %s (%s): Giá %s%s.\n  Cấu hình: CPU %s, RAM %s, GPU %s, Ổ cứng %s, Màn %s. SL còn: %d.",
                                p.getProductName(), p.getName(), priceStr, discountInfo, p.getCpu(), p.getRam(), p.getGpu(), p.getHardDrive(), p.getScreenName(), p.getQuantity());
                    }).collect(Collectors.joining("\n\n"));
        }

        String customerName = (customer != null) ? customer.getName() : "Khách hàng";

        // Thêm hướng dẫn cho AI: Nếu không biết trả lời thì gợi ý gặp nhân viên
        String systemInstruction = """
            VAI TRÒ: Trợ lý ảo Laptop.
            KHÁCH HÀNG: %s
            
            KHO HÀNG:
            ----------------
            %s
            ----------------
            
                YÊU CẦU:
                      1. Trả lời ngắn gọn, thân thiện, xưng hô 'em' và 'anh/chị'.
                      2. CHỈ tư vấn sản phẩm có trong danh sách. Không bịa đặt.
                      3. Nếu có giảm giá, hãy nhắc khách mua ngay.
                      4. Báo giá rõ ràng.
                      5. Nếu khách hỏi vấn đề quá khó hoặc khiếu nại, hãy bảo khách nhắn cú pháp: "gặp nhân viên" để được hỗ trợ.
            """.formatted(customerName, productContext);

        return geminiService.callGemini(systemInstruction + "\n\nKHÁCH HỎI: " + request.getMessage());
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