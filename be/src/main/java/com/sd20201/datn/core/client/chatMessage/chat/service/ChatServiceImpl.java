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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
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

    @Transactional
    public String chatWithAi(ChatRequest request) {
        Customer customer = request.getCustomerId() != null ?
                customerRepository.findById(request.getCustomerId()).orElse(null) : null;

        Staff staff = request.getStaffId() != null ?
                staffRepository.findById(request.getStaffId()).orElse(null) : null;

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(request.getSessionId());
        userMsg.setContent(request.getMessage());
        userMsg.setSenderRole("CLIENT");
        userMsg.setCustomer(customer);
        chatMessageRepository.save(userMsg);

        // --- RAG LOGIC ---
        ClientPDProductDetailRequest productReq = new ClientPDProductDetailRequest();
        Pageable pageable = PageRequest.of(0, 20);

        Long currentTime = System.currentTimeMillis();
        List<String> idCurrentDiscounts = discountRepository.getIdByDate(currentTime);

        Page<ClientPDProductDetailResponse> pageResult;

        if (!idCurrentDiscounts.isEmpty()) {
            pageResult = productDetailRepository.getProductDetailsDiscount(pageable, productReq, idCurrentDiscounts);
        } else {
            pageResult = productDetailRepository.getProductDetails(pageable, productReq);
        }

        String productContext = "Hiện tại kho đang chưa cập nhật sản phẩm.";

        if (pageResult != null && pageResult.hasContent()) {
            productContext = pageResult.getContent().stream()
                    .map(p -> {
                        // --- SỬA LỖI TẠI ĐÂY ---
                        // Chuyển Double -> BigDecimal an toàn
                        BigDecimal originalPrice = (p.getPrice() != null)
                                ? BigDecimal.valueOf(p.getPrice())
                                : BigDecimal.ZERO;

                        // Format giá gốc
                        String priceStr = formatMoney(originalPrice);
                        String discountInfo = "";

                        // Tính giá sau giảm
                        if (p.getPercentage() != null && p.getPercentage() > 0) {
                            BigDecimal discountPercent = BigDecimal.valueOf(p.getPercentage());

                            // Giá sau giảm = Giá gốc * (100 - %)/100
                            BigDecimal discountedPrice = originalPrice.multiply(BigDecimal.valueOf(100).subtract(discountPercent))
                                    .divide(BigDecimal.valueOf(100));

                            discountInfo = String.format(" (🔥 ĐANG GIẢM %d%% còn %s)", p.getPercentage(), formatMoney(discountedPrice));
                        }

                        return String.format("- %s (%s): Giá %s%s.\n" +
                                        "  Cấu hình: CPU %s, RAM %s, GPU %s, Ổ cứng %s, Màn %s. SL còn: %d.",
                                p.getProductName(),
                                p.getName(),
                                priceStr,
                                discountInfo,
                                p.getCpu(),
                                p.getRam(),
                                p.getGpu(),
                                p.getHardDrive(),
                                p.getScreenName(),
                                p.getQuantity()
                        );
                    })
                    .collect(Collectors.joining("\n\n"));
        }

        String customerName = (customer != null) ? customer.getName() : "Khách hàng";

        String systemInstruction = """
            VAI TRÒ: Bạn là trợ lý ảo thông minh của cửa hàng MyLapTop
            KHÁCH HÀNG: %s
            
            DỮ LIỆU KHO HÀNG (Dùng để trả lời):
            ------------------------------------------
            %s
            ------------------------------------------
            
            YÊU CẦU:
            1. Trả lời ngắn gọn, thân thiện, xưng hô 'em' và 'anh/chị'.
            2. CHỈ tư vấn sản phẩm có trong danh sách. Không bịa đặt.
            3. Nếu có giảm giá, hãy nhắc khách mua ngay.
            4. Báo giá rõ ràng.
            """.formatted(customerName, productContext);

        String fullPrompt = systemInstruction + "\n\nKHÁCH HỎI: " + request.getMessage();

        String aiResponse = geminiService.callGemini(fullPrompt);

        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(request.getSessionId());
        aiMsg.setContent(aiResponse);
        aiMsg.setSenderRole("AI");
        aiMsg.setCustomer(customer);
        aiMsg.setStaff(staff);
        aiMsg.setIsAiReplied(true);
        chatMessageRepository.save(aiMsg);

        return aiResponse;
    }

    private String formatMoney(BigDecimal price) {
        if (price == null) return "Liên hệ";
        return String.format("%,.0f VNĐ", price);
    }
}