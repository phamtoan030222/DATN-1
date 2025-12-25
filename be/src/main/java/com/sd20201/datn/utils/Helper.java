package com.sd20201.datn.utils;

import com.sd20201.datn.core.admin.voucher.voucher.model.request.AdVoucherCreateUpdateRequest;
import com.sd20201.datn.core.common.base.PageableRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.Discount;
import com.sd20201.datn.entity.Voucher;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.PaginationConstant;
import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.text.MessageFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Helper {

    public static String appendWildcard(String url) {
        return url + "/**";
    }

    public static Pageable createPageable(PageableRequest request, String defaultSortBy) {
        return PageRequest.of(request.getPage() - 1, request.getSize() == 0 ? PaginationConstant.DEFAULT_SIZE : request.getSize(), Sort.by((Sort.Direction.fromString(request.getOrderBy()) == Sort.Direction.DESC || request.getOrderBy() == null) ? Sort.Direction.DESC : Sort.Direction.ASC, (request.getSortBy() == null || request.getSortBy().isEmpty()) ? defaultSortBy : request.getSortBy()));
    }

    public static Pageable createPageable(PageableRequest request, String defaultSortBy, String defaultOrderBy) {
        int page = request.getPage() - 1;
        int size = request.getSize() == 0 ? PaginationConstant.DEFAULT_SIZE : request.getSize();
        Sort.Direction direction = request.getOrderBy() == null || request.getOrderBy().isEmpty() ? Sort.Direction.fromString(defaultOrderBy) : Sort.Direction.fromString(request.getOrderBy());
        String sortBy = request.getSortBy();
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = defaultSortBy;
        }
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    public static Pageable createPageable(PageableRequest request) {
        return createPageable(request, "createdDate", "DESC");
    }


    public static ResponseEntity<?> createResponseEntity(ResponseObject<?> responseObject) {
        return new ResponseEntity<>(responseObject, responseObject.getStatus());
    }

    public static boolean isValidPhoneNumber(String phoneNumber) {
        String regex = "^0[0-9]{9,10}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(phoneNumber);
        return !matcher.matches();
    }

    public static String replaceManySpaceToOneSpace(String name) {
        // Thay thế tất cả khoảng trắng liên tiếp bằng dấu gạch dưới
        return name.replaceAll("\\s+", " ");
    }

    public static String replaceSpaceToEmpty(String name) {
        // Thay thế tất cả khoảng trắng liên tiếp bằng dấu gạch dưới
        return name.replaceAll("\\s+", "");
    }

    private static final Map<Character, Character> SPECIAL_CHAR_MAP = new HashMap<>();

    static {
        SPECIAL_CHAR_MAP.put('đ', 'd');
        SPECIAL_CHAR_MAP.put('Đ', 'D');
        SPECIAL_CHAR_MAP.put('ơ', 'o');
        SPECIAL_CHAR_MAP.put('Ơ', 'O');
        SPECIAL_CHAR_MAP.put('ớ', 'o');
        SPECIAL_CHAR_MAP.put('ờ', 'o');
        SPECIAL_CHAR_MAP.put('ở', 'o');
        SPECIAL_CHAR_MAP.put('ỡ', 'o');
        SPECIAL_CHAR_MAP.put('ợ', 'o');
        SPECIAL_CHAR_MAP.put('ố', 'o');
        SPECIAL_CHAR_MAP.put('ồ', 'o');
        SPECIAL_CHAR_MAP.put('ổ', 'o');
        SPECIAL_CHAR_MAP.put('ỗ', 'o');
        SPECIAL_CHAR_MAP.put('ộ', 'o');
        SPECIAL_CHAR_MAP.put('ớ', 'o');
        SPECIAL_CHAR_MAP.put('ờ', 'o');
        SPECIAL_CHAR_MAP.put('ở', 'o');
        SPECIAL_CHAR_MAP.put('ỡ', 'o');
        SPECIAL_CHAR_MAP.put('ợ', 'o');
        SPECIAL_CHAR_MAP.put('ă', 'a');
        SPECIAL_CHAR_MAP.put('ắ', 'a');
        SPECIAL_CHAR_MAP.put('ằ', 'a');
        SPECIAL_CHAR_MAP.put('ẵ', 'a');
        SPECIAL_CHAR_MAP.put('ặ', 'a');
        SPECIAL_CHAR_MAP.put('â', 'a');
        SPECIAL_CHAR_MAP.put('ấ', 'a');
        SPECIAL_CHAR_MAP.put('ầ', 'a');
        SPECIAL_CHAR_MAP.put('ẩ', 'a');
        SPECIAL_CHAR_MAP.put('ẫ', 'a');
        SPECIAL_CHAR_MAP.put('ậ', 'a');
        SPECIAL_CHAR_MAP.put('ư', 'u');
        SPECIAL_CHAR_MAP.put('ứ', 'u');
        SPECIAL_CHAR_MAP.put('ừ', 'u');
        SPECIAL_CHAR_MAP.put('ử', 'u');
        SPECIAL_CHAR_MAP.put('ữ', 'u');
        SPECIAL_CHAR_MAP.put('ự', 'u');
        // Thêm các ký tự khác nếu cần
    }

    public static String generateCodeFromName(String name) {
        // Chuyển role name chuỗi thành chữ hoa
        String upperCaseString = name.toUpperCase();

        // Thay thế các ký tự đặc biệt
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : upperCaseString.toCharArray()) {
            if (SPECIAL_CHAR_MAP.containsKey(c)) {
                stringBuilder.append(SPECIAL_CHAR_MAP.get(c));
            } else {
                stringBuilder.append(c);
            }
        }
        String replacedString = stringBuilder.toString();

        // Loại bỏ dấu
        String normalizedString = Normalizer.normalize(replacedString, Normalizer.Form.NFD);
        String withoutAccentString = normalizedString.replaceAll("\\p{M}", "");

        // Thay thế tất cả khoảng trắng liên tiếp bằng dấu gạch dưới
        return withoutAccentString.replaceAll("\\s+", "_");
    }

    public static String generateCodeProductDetail() {
        return "PD" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public static String generateCodeVoucher() {
        return "VC" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public static String generateCodeVoucherDetail() {
        return "VD" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public static String buildEmailContent(Discount discount) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        StringBuilder content = new StringBuilder();
        content.append("Kính chào Quý khách hàng,\n\n");
        content.append("Chúng tôi vui mừng thông báo đến Quý khách về chương trình khuyến mãi đặc biệt:\n\n");
        content.append("🔥 TÊN CHƯƠNG TRÌNH: ").append(discount.getName()).append("\n");
        content.append("💰 MÃ GIẢM GIÁ: ").append(discount.getCode()).append("\n");
        content.append("📊 PHẦN TRĂM GIẢM: ").append(discount.getPercentage()).append("%\n");

        if (discount.getDescription() != null && !discount.getDescription().trim().isEmpty()) {
            content.append("📝 MÔ TẢ: ").append(discount.getDescription()).append("\n");
        }

        if (discount.getStartDate() != null) {
            content.append("⏰ THỜI GIAN BẮT ĐẦU: ").append(dateFormat.format(new Date(discount.getStartDate()))).append("\n");
        }

        if (discount.getEndDate() != null) {
            content.append("⏰ THỜI GIAN KẾT THÚC: ").append(dateFormat.format(new Date(discount.getEndDate()))).append("\n");
        }

        content.append("\n");
        content.append("Hãy nhanh tay sử dụng mã giảm giá để nhận được ưu đãi tốt nhất!\n\n");
        content.append("Cách sử dụng:\n");
        content.append("1. Thêm sản phẩm vào giỏ hàng\n");
        content.append("2. Nhập mã giảm giá: ").append(discount.getCode()).append("\n");
        content.append("3. Áp dụng và hoàn tất thanh toán\n\n");
        content.append("Cảm ơn Quý khách đã tin tưởng và ủng hộ chúng tôi!\n\n");
        content.append("Trân trọng,\n");
        content.append("Đội ngũ [Siu siu siu 5 anh em siu nhân]");

        return content.toString();
    }

    //Helper của Tài
    // 1. Logic Validate ngày tháng
    public static void validateVoucherDateRange(Long start, Long end) throws BadRequestException {
        Long now = DateTimeUtil.getCurrentTimeMillisecondsStamp();

        if (start == null || end == null) {
            throw new BadRequestException("StartDate hoặc EndDate không được để trống!!");
        }
        if (start < now) {
            throw new BadRequestException("Ngày bắt đầu không được nhỏ hơn hiện tại");
        }
        if (end < now) {
            throw new BadRequestException("Ngày kết thúc không được nhỏ hơn hiện tại");
        }
        if (start >= end) {
            throw new BadRequestException("Ngày bắt đầu không được lớn hơn ngày kết thúc!!");
        }
    }

    // 2. Logic Map Request -> Entity
    public static void mapRequestToVoucher(AdVoucherCreateUpdateRequest request, Voucher voucher) {
        voucher.setName(request.getName());
        voucher.setTargetType(request.getTargetType());
        voucher.setTypeVoucher(request.getTypeVoucher());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMaxValue(request.getMaxValue());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setConditions(request.getConditions());
        voucher.setNote(request.getNote());

        long now = DateTimeUtil.getCurrentTimeMillisecondsStamp();
        voucher.setStatus((request.getStartDate() <= now && now <= request.getEndDate()) ? EntityStatus.ACTIVE : EntityStatus.INACTIVE);
    }

    // 3. Logic so sánh thay đổi nội dung (để quyết định gửi mail)
    public static boolean isVoucherContentChanged(Voucher oldVoucher, AdVoucherCreateUpdateRequest newRequest) {
        if (!oldVoucher.getName().equals(newRequest.getName())) return true;
        if (oldVoucher.getTypeVoucher() != newRequest.getTypeVoucher()) return true;
        if (!Objects.equals(oldVoucher.getDiscountValue(), newRequest.getDiscountValue())) return true;
        if (!Objects.equals(oldVoucher.getMaxValue(), newRequest.getMaxValue())) return true;
        if (!Objects.equals(oldVoucher.getConditions(), newRequest.getConditions())) return true;
        if (!Objects.equals(oldVoucher.getStartDate(), newRequest.getStartDate())) return true;
        if (!Objects.equals(oldVoucher.getEndDate(), newRequest.getEndDate())) return true;
        return false;
    }

    // 4. Logic tạo HTML Email (Giúp Service gọn nhất)
    public static String createVoucherEmailBody(Voucher voucher, Customer customer) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(voucher.getStartDate()), ZoneId.systemDefault());
        LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(voucher.getEndDate()), ZoneId.systemDefault());

        String discount = voucher.getTypeVoucher() == TypeVoucher.PERCENTAGE ? voucher.getDiscountValue() + "%" : voucher.getDiscountValue() + " VND";

        String htmlTemplate = """
                    <html>
                      <body style="font-family: Arial, sans-serif; background-color: #f9f9f9; padding: 20px;">
                        <div style="max-width: 600px; margin: auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                          <div style="background: #ff6600; padding: 20px; text-align: center; color: white;">
                            <h1 style="margin: 0;">🎁 Ưu Đãi Đặc Biệt Dành Cho Bạn</h1>
                          </div>
                          <div style="padding: 20px; color: #333;">
                            <p>Xin chào, {6}</p>
                            <p>Chúng tôi gửi tặng bạn một <b>phiếu giảm giá đặc biệt</b>. Hãy sử dụng ngay để nhận ưu đãi hấp dẫn!</p>
                            <table style="width: 100%; border-collapse: collapse; margin-top: 20px;">
                              <tr style="background: #f2f2f2;">
                                <td style="padding: 10px; font-weight: bold;">Mã Voucher</td>
                                <td style="padding: 10px; color: #ff6600; font-size: 18px; font-weight: bold;">{0}</td>
                              </tr>
                              <tr>
                                <td style="padding: 10px; font-weight: bold;">Giá trị giảm</td>
                                <td style="padding: 10px;">{1}</td>
                              </tr>
                              <tr style="background: #f2f2f2;">
                                <td style="padding: 10px; font-weight: bold;">Giảm tối đa</td>
                                <td style="padding: 10px;">{2} VND</td>
                              </tr>
                              <tr>
                                <td style="padding: 10px; font-weight: bold;">Thời gian áp dụng</td>
                                <td style="padding: 10px;">Từ {3} đến {4}</td>
                              </tr>
                              <tr style="background: #f2f2f2;">
                                <td style="padding: 10px; font-weight: bold;">Điều kiện</td>
                                <td style="padding: 10px;">Đơn hàng từ {5} VND</td>
                              </tr>
                            </table>
                            <div style="text-align: center; margin-top: 30px;">
                              <a href="https://your-shop.com"
                                 style="background: #ff6600; color: white; padding: 12px 24px; border-radius: 5px; text-decoration: none; font-weight: bold;">
                                Mua sắm ngay
                              </a>
                            </div>
                          </div>
                          <div style="background: #eee; text-align: center; padding: 15px; font-size: 12px; color: #777;">
                            © 2025 My Laptop. Mọi quyền được bảo lưu.
                          </div>
                        </div>
                      </body>
                    </html>
                """;

        return MessageFormat.format(htmlTemplate, voucher.getCode(), discount, DateTimeUtil.formatMoney(voucher.getMaxValue()), start.format(formatter), end.format(formatter), DateTimeUtil.formatMoney(voucher.getConditions()), customer.getName());
    }
}
