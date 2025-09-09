package com.sd20201.datn.utils;

import com.sd20201.datn.core.common.base.PageableRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Discount;
import com.sd20201.datn.infrastructure.constant.PaginationConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;


import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Helper {

    public static String appendWildcard(String url) {
        return url + "/**";
    }

    public static Pageable createPageable(PageableRequest request, String defaultSortBy) {
        return PageRequest.of(
                request.getPage() - 1,
                request.getSize() == 0 ? PaginationConstant.DEFAULT_SIZE : request.getSize(),
                Sort.by(
                        (Sort.Direction.fromString(
                                request.getOrderBy()) == Sort.Direction.DESC ||
                                request.getOrderBy() == null
                        ) ? Sort.Direction.DESC : Sort.Direction.ASC,
                        (request.getSortBy() == null
                                || request.getSortBy().isEmpty()
                        ) ? defaultSortBy : request.getSortBy()
                ));
    }

    public static Pageable createPageable(PageableRequest request, String defaultSortBy, String defaultOrderBy) {
        int page = request.getPage() - 1;
        int size = request.getSize() == 0 ? PaginationConstant.DEFAULT_SIZE : request.getSize();
        Sort.Direction direction = request.getOrderBy() == null || request.getOrderBy().isEmpty()
                ? Sort.Direction.fromString(defaultOrderBy)
                : Sort.Direction.fromString(request.getOrderBy());
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
    public static String  buildEmailContent(Discount discount) {
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

}
