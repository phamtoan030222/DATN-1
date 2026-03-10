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

    private static final Map<String, Boolean> humanChatSessions = new ConcurrentHashMap<>();

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

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(request.getSessionId());
        userMsg.setContent(request.getMessage());
        userMsg.setSenderRole("CLIENT");
        userMsg.setCustomer(customer);
        userMsg.setCreatedDate(System.currentTimeMillis());
        chatMessageRepository.save(userMsg);
        try {
            messagingTemplate.convertAndSend("/topic/admin-messages", userMsg);
        } catch (Exception e) {
            log.error("Lỗi gửi socket admin: {}", e.getMessage());
        }
        if (humanChatSessions.getOrDefault(request.getSessionId(), false)) {
            return "Đang chờ nhân viên phản hồi...";
        }
        String messageContent = request.getMessage().toLowerCase();
        boolean wantHuman = TRIGGER_KEYWORDS.stream().anyMatch(messageContent::contains);

        if (wantHuman) {
            enableHumanSupport(request.getSessionId());
            String response = "Hệ thống đã kết nối bạn với nhân viên tư vấn. Vui lòng chờ trong giây lát...";
            ChatMessage sysMsg = new ChatMessage();
            sysMsg.setSessionId(request.getSessionId());
            sysMsg.setContent(response);
            sysMsg.setSenderRole("SYSTEM");
            sysMsg.setCreatedDate(System.currentTimeMillis());
            chatMessageRepository.save(sysMsg);

            messagingTemplate.convertAndSend("/topic/user/" + request.getSessionId(), sysMsg);
            messagingTemplate.convertAndSend("/topic/admin-messages", sysMsg);

            return response;
        }

        String aiResponse = callGeminiRAG(request, customer);

        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(request.getSessionId());
        aiMsg.setContent(aiResponse);
        aiMsg.setSenderRole("AI");
        aiMsg.setCustomer(customer);
        aiMsg.setIsAiReplied(true);
        aiMsg.setCreatedDate(System.currentTimeMillis());
        chatMessageRepository.save(aiMsg);

        messagingTemplate.convertAndSend("/topic/user/" + request.getSessionId(), aiMsg);

        messagingTemplate.convertAndSend("/topic/admin-messages", aiMsg);

        return aiResponse;
    }


    private String callGeminiRAG(ChatRequest request, Customer customer) {
        ClientPDProductDetailRequest productReq = new ClientPDProductDetailRequest();
        productReq.setMinPrice(0);
        productReq.setMaxPrice(1000000000);

        Pageable pageable = PageRequest.of(0, 25);
        Long currentTime = System.currentTimeMillis();

        List<String> idCurrentDiscounts = discountRepository.getIdByDate(currentTime);
        Page<ClientPDProductDetailResponse> pageResult;

        if (idCurrentDiscounts != null && !idCurrentDiscounts.isEmpty()) {
            pageResult = productDetailRepository.getProductDetailsDiscount(pageable, productReq, currentTime);
        } else {
            pageResult = productDetailRepository.getProductDetails(pageable, productReq);
        }

        StringBuilder sb = new StringBuilder();
        if (pageResult != null && pageResult.hasContent()) {
            for (ClientPDProductDetailResponse p : pageResult.getContent()) {
                BigDecimal originalPrice = (p.getPrice() != null) ? BigDecimal.valueOf(p.getPrice()) : BigDecimal.ZERO;
                String discountNote = "";
                if (p.getPercentage() != null && p.getPercentage() > 0) {
                    BigDecimal discountPercent = BigDecimal.valueOf(p.getPercentage());
                    BigDecimal discountedPrice = originalPrice.multiply(BigDecimal.valueOf(100).subtract(discountPercent))
                            .divide(BigDecimal.valueOf(100));

                    discountNote = String.format(" >>> [SIÊU SALE %d%%] GIÁ CHỈ CÒN: %s",
                            p.getPercentage(), formatMoney(discountedPrice));
                }

                sb.append(String.format("""
                * LAPTOP: [%s (Model: %s)](/product-detail/%s)
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
                        p.getProductName(), p.getName(), p.getId(),
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
        log.info("CONTEST GỬI GEMINI: \n{}", productContext);

        String customerName = (customer != null) ? customer.getName() : "khách hàng";

        String systemInstruction = """
            VAI TRÒ: Bạn là chuyên gia tư vấn Laptop nhiệt tình của MyLaptop.
            KHÁCH HÀNG: %s
            
            DỮ LIỆU KHO HÀNG CỦA BẠN:
            %s
            
            HƯỚNG DẪN TƯ VẤN:
            1. NGOẠI HÌNH: Nếu khách hỏi về vẻ ngoài, hãy dùng thông tin 'Màu sắc' và 'Chất liệu' (ví dụ: vỏ nhôm sang trọng, màu trắng thanh lịch).
            2. ƯU ĐÃI: Luôn ưu tiên giới thiệu các máy có [SIÊU SALE] vì giá đang rất tốt.
            3. CHUYÊN MÔN: Tư vấn máy dựa trên cấu hình (RAM, CPU, GPU). Nếu khách làm đồ họa, ưu tiên máy có GPU mạnh.
            4. PHONG CÁCH: Trả lời thân thiện, chuyên nghiệp, XƯNG 'em' và GỌI khách là 'anh/chị'.
            5. SỰ THẬT: CHỈ tư vấn sản phẩm có trong danh sách. Tuyệt đối không bịa đặt thông tin.
            6. CHỐT SALE: Nếu có giảm giá, hãy khéo léo nhắc khách mua ngay kẻo lỡ.
            7. GIÁ CẢ: Báo giá rõ ràng, định dạng VND.
            8. CHUYỂN TIẾP: Nếu khách hỏi vấn đề ngoài chuyên môn, quá khó, hoặc khiếu nại, hãy chủ động mời khách nhắn: "gặp nhân viên" để được hỗ trợ.
            9. ĐỊNH DẠNG LINK: LUÔN GIỮ NGUYÊN định dạng link Markdown theo mẫu [Tên máy](/product-detail/id) khi nhắc đến bất kỳ tên sản phẩm nào để khách có thể click.
            10. LỜI NHẮC CUỐI CÂU: Ở cuối mỗi câu trả lời, hãy LUÔN luôn thêm một dòng nhắn nhủ thân thiện (dùng in nghiêng). Câu nói sau : "*Hoặc nếu cần hỗ trợ chuyên sâu hơn, anh/chị cứ nhắn 'gặp nhân viên' để được tư vấn kĩ hơn ạ!*"
            """.formatted(customerName, productContext);

        return geminiService.callGemini(systemInstruction + "\n\nCÂU HỎI CỦA KHÁCH: " + request.getMessage());
    }

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