package com.sd20201.datn.core.client.discounts.discount.service.impl;

import com.sd20201.datn.core.client.discounts.discount.model.request.ClientDiscountRequest;
import com.sd20201.datn.core.client.discounts.discount.model.request.ClientDscountFilterRequest;
import com.sd20201.datn.core.client.discounts.discount.model.request.ClientDiscountUpdateRequest;
import com.sd20201.datn.core.client.discounts.discount.model.request.ClientDiscountValidateRequest;
import com.sd20201.datn.core.client.discounts.discount.model.response.ClientDiscountResponse;
import com.sd20201.datn.core.client.discounts.discount.repository.ClientCustomerDiscountRepository;
import com.sd20201.datn.core.client.discounts.discount.repository.ClientDiscountRepossitory;
import com.sd20201.datn.core.client.discounts.discount.repository.ClientDiscountSearchRepository;
import com.sd20201.datn.core.client.discounts.discount.service.ClientDiscountService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Discount;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClientDiscountServiceImpl implements ClientDiscountService {

    private final ClientDiscountRepossitory adDiscountRepossitory;

    private final ClientDiscountSearchRepository adDiscountSearchRepository;

    private final ClientCustomerDiscountRepository customerRepository;

    // ĐÃ XÓA BIẾN GÂY LỖI CIRCULAR REFERENCE Ở ĐÂY

    @Override
    public ResponseObject<?> getAllDiscounts(ClientDiscountRequest request) {
        Pageable pageable = Helper.createPageable(request, "createdDate");

        return new ResponseObject<>(
                PageableObject.of(adDiscountRepossitory.getAllDiscount(pageable, request.getDiscountName()
                        , request.getDiscountStatus()
                )),
                HttpStatus.OK,
                "Lấy thành công danh sách Đợt giảm giá "
        );

    }

    @Override
    public ResponseObject<?> creatDiscount(ClientDiscountValidateRequest request) {
        List<Discount> discountsCode = adDiscountRepossitory.findAlDiscountByCode(request.getDiscountCode());
        if (!discountsCode.isEmpty()) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Mã đợt giảm giá đã tồn tại",
                    false,
                    "DISCOUNT_CODE_EXISTS"
            );
        }


        Long now = System.currentTimeMillis();
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate() >= request.getEndDate()) {
                return new ResponseObject<>(
                        null,
                        HttpStatus.BAD_REQUEST,
                        "Thời gian bắt đầu phải nhỏ hơn thời gian kết thúc",
                        false,
                        "DISCOUNT_TIME_INVALID"
                );
            }
            if (request.getStartDate() <= now) {
                return new ResponseObject<>(
                        null,
                        HttpStatus.BAD_REQUEST,
                        "Thời gian bắt đầu phải lớn hơn thời gian hiện tại",
                        false,
                        "DISCOUNT_START_INVALID"
                );
            }
        }


        if (request.getPercentage() == null || request.getPercentage().toString().trim().isEmpty()) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Phần trăm giảm giá không được để trống",
                    false,
                    "DISCOUNT_PERCENTAGE_REQUIRED"
            );
        }

        String percentageStr = request.getPercentage().toString();

        if (percentageStr.matches(".*[a-zA-Z].*")) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Phần trăm giảm giá không được chứa chữ cái",
                    false,
                    "DISCOUNT_PERCENTAGE_ALPHA"
            );
        }

        if (!percentageStr.matches("^\\d+(\\.\\d+)?$")) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Phần trăm giảm giá chỉ được nhập số, không được chứa ký tự đặc biệt",
                    false,
                    "DISCOUNT_PERCENTAGE_SPECIAL"
            );
        }

        double percentage = Double.parseDouble(percentageStr);

        Discount discount = new Discount();
        discount.setName(request.getDiscountName());
        discount.setCode(request.getDiscountCode());
        discount.setStartDate(request.getStartDate());
        discount.setEndDate(request.getEndDate());
        discount.setDescription(request.getDescription());
        discount.setPercentage(request.getPercentage());
        discount.setCreatedDate(System.currentTimeMillis());
        discount.setStatus(EntityStatus.ACTIVE);

        Discount savedDiscount = adDiscountRepossitory.save(discount);

        return new ResponseObject<>(
                savedDiscount,
                HttpStatus.OK,
                "Thêm Đợt giảm giá thành công thành công",
                true,
                null
        );
    }

    @Override
    public ResponseObject<?> updateDiscount(String id, ClientDiscountUpdateRequest request) {
        Discount discount = adDiscountRepossitory.findById(id)
                .orElse(null);

        if (discount == null) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Đợt giảm giá không tồn tại",
                    false,
                    "DISCOUNT_NOT_FOUND"
            );
        }

        if (discount.getStatus() != EntityStatus.ACTIVE) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Đợt giảm giá không tồn tại",
                    false,
                    "DISCOUNT_INACTIVE"
            );
        }

        if (!adDiscountRepossitory.findByNameAndNotId(request.getDiscountName(), id).isEmpty()) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Tên đợt giảm giá đã tồn tại",
                    false,
                    "DISCOUNT_NAME_EXISTS"
            );
        }

        if (!adDiscountRepossitory.findByCodeAndNotId(request.getDiscountCode(), id).isEmpty()) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Mã đợt giảm giá đã tồn tại",
                    false,
                    "DISCOUNT_CODE_EXISTS"
            );
        }

        long now = System.currentTimeMillis();

        if (discount.getEndDate() != null && discount.getEndDate() <= now) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Đợt giảm giá đã kết thúc, không thể cập nhật",
                    false,
                    "DISCOUNT_EXPIRED"
            );
        }

        if (discount.getStartDate() != null && discount.getStartDate() <= now) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Đợt giảm giá đang diễn ra, không thể cập nhật",
                    false,
                    "DISCOUNT_RUNNING"
            );
        }

//        List<Discount> overlappingDiscounts = adDiscountRepossitory.findOverlappingDiscounts(
//                request.getStartDate(),
//                request.getEndDate()
//        );
//
//        if (!overlappingDiscounts.isEmpty()) {
//            Discount conflictDiscount = overlappingDiscounts.get(0);
//            String conflictInfo = String.format(
//                    "Thời gian bị trùng với đợt giảm giá '%s' (%s - %s)",
//                    conflictDiscount.getName(),
//                    new Date(conflictDiscount.getStartDate()).toString(),
//                    new Date(conflictDiscount.getEndDate()).toString()
//            );
//
//            return new ResponseObject<>(
//                    null,
//                    HttpStatus.BAD_REQUEST,
//                    "Trong một khoảng thời gian chỉ được có một đợt giảm giá. " + conflictInfo,
//                    false,
//                    "DISCOUNT_TIME_OVERLAPPING"
//            );
//        }

        if (request.getPercentage() == null || request.getPercentage().toString().trim().isEmpty()) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Phần trăm giảm giá không được để trống",
                    false,
                    "DISCOUNT_PERCENTAGE_REQUIRED"
            );
        }

        String percentageStr = request.getPercentage().toString();

        if (percentageStr.matches(".*[a-zA-Z].*")) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Phần trăm giảm giá không được chứa chữ cái",
                    false,
                    "DISCOUNT_PERCENTAGE_ALPHA"
            );
        }

        if (!percentageStr.matches("^\\d+(\\.\\d+)?$")) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Phần trăm giảm giá chỉ được nhập số, không được chứa ký tự đặc biệt",
                    false,
                    "DISCOUNT_PERCENTAGE_SPECIAL"
            );
        }

        double percentage = Double.parseDouble(percentageStr);


        discount.setName(request.getDiscountName());
        discount.setCode(request.getDiscountCode());
        discount.setStartDate(request.getStartDate());
        discount.setEndDate(request.getEndDate());
        discount.setDescription(request.getDescription());
        discount.setPercentage(request.getPercentage());
        discount.setLastModifiedDate(System.currentTimeMillis());
        discount.setStatus(EntityStatus.ACTIVE);

        adDiscountRepossitory.save(discount);

        return new ResponseObject<>(
                discount,
                HttpStatus.OK,
                "Cập nhật đợt giảm giá thành công",
                true,
                null
        );
    }


    @Override
    public ResponseObject<?> endDiscount(String id) {
        Discount discount = adDiscountRepossitory.findById(id).orElse(null);
        if (discount == null || EntityStatus.INACTIVE.equals(discount.getStatus())) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.NOT_FOUND,
                    "Đợt giảm giá không tồn tại",
                    false,
                    "DISCOUNT_NOT_FOUND"
            );
        }

        long now = System.currentTimeMillis();

        if (discount.getEndDate() != null && discount.getEndDate() <= now) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Đợt giảm giá đã kết thúc, không thể làm gì!!!",
                    false,
                    "DISCOUNT_EXPIRED"
            );
        }

        if (discount.getStartDate() != null && discount.getStartDate() > now) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Đợt giảm giá chưa diễn ra, không thể kết thúc",
                    false,
                    "DISCOUNT_NOT_STARTED"
            );
        }

        discount.setEndDate(now);
        discount.setLastModifiedDate(now);
        adDiscountRepossitory.save(discount);

        return new ResponseObject<>(
                discount,
                HttpStatus.OK,
                "Đợt giảm giá đã được kết thúc sớm",
                true,
                null
        );
    }


    @Override
    public ResponseObject<?> startDiscount(String id) {
        Discount discount = adDiscountRepossitory.findById(id).orElse(null);
        if (discount == null || EntityStatus.INACTIVE.equals(discount.getStatus())) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.NOT_FOUND,
                    "Đợt giảm giá không tồn tại",
                    false,
                    "DISCOUNT_NOT_FOUND"
            );
        }

        long now = System.currentTimeMillis();

        if (discount.getEndDate() != null && discount.getEndDate() <= now) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Đợt giảm giá đã kết thúc, không thể làm gì!!!",
                    false,
                    "DISCOUNT_EXPIRED"
            );
        }

        if (discount.getStartDate() != null
                && discount.getStartDate() < now
                && discount.getEndDate() != null
                && discount.getEndDate() > now) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Đợt giảm giá đang diễn ra",
                    false,
                    "DISCOUNT_ALREADY_RUNNING"
            );
        }

        discount.setStartDate(now);
        discount.setLastModifiedDate(now);
        adDiscountRepossitory.save(discount);

        return new ResponseObject<>(
                discount,
                HttpStatus.OK,
                "Đợt giảm giá đã được bắt đầu sớm",
                true,
                null
        );
    }


    @Override
    public ResponseObject<?> deleteDiscount(String id) {
        Discount discount = adDiscountRepossitory.findById(id)
                .orElse(null);

        if (discount == null) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Đợt giảm giá không tồn tại",
                    false,
                    "DISCOUNT_NOT_FOUND"
            );
        }

        long now = System.currentTimeMillis();
        if (discount.getStartDate() != null && discount.getStartDate() <= now) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Đợt giảm giá đã bắt đầu hoặc đã kết thúc, không thể xóa",
                    false,
                    "DISCOUNT_STARTED"
            );
        }

        discount.setStatus(EntityStatus.INACTIVE);
        discount.setLastModifiedDate(now);
        adDiscountRepossitory.save(discount);

        return new ResponseObject<>(
                null,
                HttpStatus.OK,
                "Xóa đợt giảm giá thành công",
                true,
                null
        );
    }

    @Override
    public ResponseObject<?> filterDiscounts(ClientDscountFilterRequest request) {
        Pageable pageable = Helper.createPageable(request, "createdDate");

        return new ResponseObject<>(
                PageableObject.of(adDiscountSearchRepository.filterDiscounts(
                        pageable,
                        request.getDiscountName(),
                        request.getDiscountCode(),
                        request.getDiscountStatus(),
                        request.getPercentage(),
                        request.getDescription(),
                        request.getStartDate(),
                        request.getEndDate()
                )),
                HttpStatus.OK,
                "Lấy thành công danh sách Đợt giảm giá"
        );
    }

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public ResponseObject<?> sendEmailDiscount(String id) {
        try {
            Discount discount = adDiscountRepossitory.findById(id).orElse(null);
            if (discount == null || EntityStatus.INACTIVE.equals(discount.getStatus())) {
                return new ResponseObject<>(
                        null,
                        HttpStatus.NOT_FOUND,
                        "Đợt giảm giá không tồn tại",
                        false,
                        "DISCOUNT_NOT_FOUND"
                );
            }

            long now = System.currentTimeMillis();
            if (discount.getEndDate() != null && discount.getEndDate() <= now) {
                return new ResponseObject<>(
                        null,
                        HttpStatus.BAD_REQUEST,
                        "Đợt giảm giá đã kết thúc, không thể gửi email",
                        false,
                        "DISCOUNT_EXPIRED"
                );
            }

            List<String> customerEmails = customerRepository.findAllActiveCustomerEmails();

            if (customerEmails.isEmpty()) {
                return new ResponseObject<>(
                        null,
                        HttpStatus.BAD_REQUEST,
                        "Không có khách hàng nào để gửi email",
                        false,
                        "NO_CUSTOMERS_FOUND"
                );
            }


            String subject = "🎉 THÔNG BÁO KHUYẾN MÃI ĐẶC BIỆT - " + discount.getName();
            String emailContent = Helper.buildEmailContent(discount);

            int successCount = 0;
            int failCount = 0;

            for (String email : customerEmails) {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(email);
                    message.setSubject(subject);
                    message.setText(emailContent);
                    message.setFrom("smtp.gmail.com");

                    mailSender.send(message);
                    successCount++;

                    System.out.println("Gửi email thành công đến: " + email);

                } catch (Exception e) {
                    failCount++;
                    System.err.println("Lỗi gửi email đến " + email + ": " + e.getMessage());
                }
            }


            String resultMessage = String.format(
                    "Gửi email khuyến mãi thành công! Thành công: %d, Thất bại: %d",
                    successCount, failCount
            );

            Map<String, Object> result = new HashMap<>();
            result.put("totalEmails", customerEmails.size());
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("discountInfo", discount);

            return new ResponseObject<>(
                    result,
                    HttpStatus.OK,
                    resultMessage,
                    true,
                    null
            );

        } catch (Exception e) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Có lỗi xảy ra khi gửi email: " + e.getMessage(),
                    false,
                    "EMAIL_SEND_ERROR"
            );
        }
    }

    @Override
    public List<ClientDiscountResponse> layDanhSachSaleDangHoatDong() {
        Long currentTime = System.currentTimeMillis();
        return adDiscountRepossitory.getActiveDiscounts(currentTime, EntityStatus.ACTIVE);
    }
}