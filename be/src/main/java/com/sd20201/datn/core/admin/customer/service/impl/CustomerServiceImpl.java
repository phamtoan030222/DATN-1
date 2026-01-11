package com.sd20201.datn.core.admin.customer.service.impl;

import com.sd20201.datn.core.admin.customer.model.response.CustomerResponse;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.core.admin.customer.model.request.CustomerCreateUpdateRequest;
import com.sd20201.datn.core.admin.customer.model.request.CustomerRequest;
import com.sd20201.datn.core.admin.customer.repository.AdCustomerRepository;
import com.sd20201.datn.core.admin.customer.service.CustomerService;
import com.sd20201.datn.entity.Account;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.RoleConstant;
import com.sd20201.datn.repository.AccountRepository;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final AdCustomerRepository adCustomerRepository;
    private final AccountRepository accountRepository;

    @Override
    public ResponseObject<?> getAllCustomers(CustomerRequest request) {
        Pageable pageable = Helper.createPageable(request, "createdDate");

        return new ResponseObject<>(
                PageableObject.of(adCustomerRepository.getAllCustomers(
                        pageable,
                        request.getKeyword(),
                        request.getCustomerStatus(),
                        request.getCustomerGender()
                )),
                HttpStatus.OK,
                "Lấy thành công danh sách khách hàng"
        );

    }

    @Override
    @Transactional
    public ResponseObject<?> createCustomer(CustomerCreateUpdateRequest request) {
        // Kiểm tra tên trùng

        List<Customer> customersByPhone = adCustomerRepository.findByExactPhone(request.getCustomerPhone());
        if (!customersByPhone.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST,
                    "Số điện thoại đã tồn tại", false, "CUSTOMER_PHONE_EXISTS");
        }

        // ✅ THÊM: Kiểm tra email trùng (nếu có email)
        if (request.getCustomerEmail() != null && !request.getCustomerEmail().trim().isEmpty()) {
            List<Customer> customersByEmail = adCustomerRepository.findByEmail(request.getCustomerEmail());
            if (!customersByEmail.isEmpty()) {
                return new ResponseObject<>(null, HttpStatus.BAD_REQUEST,
                        "Email đã tồn tại", false, "CUSTOMER_EMAIL_EXISTS");
            }
        }

        // Xử lý account
        Account account = null;
        if (request.getCustomerIdAccount() != null && !request.getCustomerIdAccount().trim().isEmpty()) {
            account = accountRepository.findById(request.getCustomerIdAccount()).orElse(null);
            if (account == null) {
                return new ResponseObject<>(null, HttpStatus.BAD_REQUEST,
                        "Tài khoản không tồn tại", false, "ACCOUNT_NOT_FOUND");
            }
        }
        // Trường hợp 2: Không gửi ID Account -> TỰ ĐỘNG TẠO ACCOUNT MỚI
        else {
            account = new Account();
            // Tự sinh ID (hoặc để DB tự sinh nếu cấu hình @GeneratedValue)
            account.setId(UUID.randomUUID().toString());

            // Lấy SĐT hoặc Email làm username
            String username = request.getCustomerPhone();
            // Hoặc generate: request.getCustomerName() + code...

            // Kiểm tra username đã tồn tại chưa (quan trọng)
            if(accountRepository.findByUsername(username).isPresent()){
                return new ResponseObject<>(null, HttpStatus.BAD_REQUEST,
                        "Tài khoản/SĐT đã tồn tại trong hệ thống", false, "USERNAME_EXISTS");
            }

            account.setUsername(username);
            account.setPassword("123456");
            account.setRoleConstant(RoleConstant.KHACH_HANG);

            // Lưu Account trước
            account = accountRepository.save(account);
        }

        System.out.println("DEBUG CHECK ID: " + account.getId());
        String generatedCode = generateCustomerCode(request.getCustomerName());

        // Tạo customer
        Customer customer = new Customer();
        customer.setAccount(account);
        customer.setCode(generatedCode);
        customer.setName(request.getCustomerName());
        customer.setPhone(request.getCustomerPhone());
        customer.setEmail(request.getCustomerEmail());
        customer.setAvatarUrl(request.getCustomerAvatar());
        customer.setBirthday(request.getCustomerBirthday());
        customer.setGender(request.getCustomerGender());
        customer.setDescription(request.getCustomerDescription());

        customer.setStatus(EntityStatus.ACTIVE);
        customer.setCreatedDate(System.currentTimeMillis());

// Thêm debug này NGAY SAU khi save:
        adCustomerRepository.save(customer);
        adCustomerRepository.flush(); // Force DB write

// Query lại từ DB
        Customer freshCustomer = adCustomerRepository.findById(customer.getId()).orElse(null);

// ✅ Response data - SỬA LOGIC NGƯỢC
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("id", customer.getId());
        responseData.put("customerName", customer.getName());
        responseData.put("customerPhone", customer.getPhone());
        responseData.put("customerEmail", customer.getEmail());
        responseData.put("customerGender", customer.getGender());
        responseData.put("customerBirthday", customer.getBirthday());

// Dùng fresh data cho response
        int actualResponseStatus = freshCustomer.getStatus() == EntityStatus.ACTIVE ? 1 : 0;
        responseData.put("customerStatus", actualResponseStatus);
        responseData.put("customerCode", customer.getCode());
        responseData.put("customerAvatar", customer.getAvatarUrl());
        responseData.put("customerDescription", customer.getDescription());
        responseData.put("customerCreatedDate", customer.getCreatedDate());
        return new ResponseObject<>(responseData, HttpStatus.OK, "Thêm khách hàng thành công", true, null);
    }

    @Override
    public ResponseObject<?> updateCustomer(String id, CustomerCreateUpdateRequest request) {
        Customer customer = adCustomerRepository.findById(id).orElse(null);
        if (customer == null) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy khách hàng với id = " + id,
                    false,
                    "CUSTOMER_NOT_FOUND"
            );
        }

        // ✅ Kiểm tra trùng số điện thoại (ngoại trừ chính mình)
        List<Customer> customersByPhone = adCustomerRepository.findByExactPhone(request.getCustomerPhone());
        if (!customersByPhone.isEmpty() && !customersByPhone.get(0).getId().equals(id)) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Số điện thoại đã tồn tại",
                    false,
                    "CUSTOMER_PHONE_EXISTS"
            );
        }

        // ✅ Kiểm tra trùng email (ngoại trừ chính mình, nếu có email)
        if (request.getCustomerEmail() != null && !request.getCustomerEmail().trim().isEmpty()) {
            List<Customer> customersByEmail = adCustomerRepository.findByEmail(request.getCustomerEmail());
            if (!customersByEmail.isEmpty() && !customersByEmail.get(0).getId().equals(id)) {
                return new ResponseObject<>(
                        null,
                        HttpStatus.BAD_REQUEST,
                        "Email đã tồn tại",
                        false,
                        "CUSTOMER_EMAIL_EXISTS"
                );
            }
        }

        // Xử lý account nếu có customerIdAccount
        if (request.getCustomerIdAccount() != null) {
            Account account = accountRepository.findById(request.getCustomerIdAccount()).orElse(null);
            if (account == null) {
                return new ResponseObject<>(null, HttpStatus.BAD_REQUEST,
                        "Tài khoản không tồn tại", false, "ACCOUNT_NOT_FOUND");
            }
            customer.setAccount(account);
        } else {
            customer.setAccount(null);
        }

        // Cập nhật thông tin khác
        customer.setName(request.getCustomerName());
        customer.setPhone(request.getCustomerPhone());
        customer.setEmail(request.getCustomerEmail());
        if (request.getCustomerAvatar() != null && !request.getCustomerAvatar().trim().isEmpty()) {
            customer.setAvatarUrl(request.getCustomerAvatar().trim());
        } else if (request.getCustomerAvatar() != null && request.getCustomerAvatar().trim().isEmpty()) {
            customer.setAvatarUrl(null); // Clear avatar nếu gửi empty string
        }
        customer.setBirthday(request.getCustomerBirthday());
        customer.setGender(request.getCustomerGender());
        customer.setDescription(request.getCustomerDescription());

        // 🔥 THÊM DÒNG NÀY - Cập nhật status
        if (request.getCustomerStatus() != null) {
            EntityStatus status = request.getCustomerStatus() == 1
                    ? EntityStatus.ACTIVE
                    : EntityStatus.INACTIVE;
            customer.setStatus(status);
        }

        adCustomerRepository.save(customer);

        return new ResponseObject<>(
                customer,
                HttpStatus.OK,
                "Cập nhật khách hàng thành công",
                true,
                null
        );
    }

    @Override
    public ResponseObject<?> deleteCustomer(String id) {
        Customer customer = adCustomerRepository.findById(id).orElse(null);
        if (customer == null) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy khách hàng với id = " + id,
                    false,
                    "CUSTOMER_NOT_FOUND"
            );
        }

        // 🔹 Xóa mềm
        customer.setStatus(EntityStatus.INACTIVE);
        adCustomerRepository.save(customer);

        return new ResponseObject<>(
                null,
                HttpStatus.OK,
                "Xóa khách hàng thành công",
                true,
                null
        );
    }

    @Override
    public ResponseObject<?> getCustomerById(String id) {
        CustomerResponse customer = adCustomerRepository.findCustomerById(id);
        if (customer == null) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy khách hàng với id = " + id,
                    false,
                    "CUSTOMER_NOT_FOUND"
            );
        }

        return new ResponseObject<>(customer, HttpStatus.OK, "Lấy thông tin khách hàng thành công", true, null);
    }

    private String generateCustomerCode(String customerName) {
        if (customerName == null || customerName.trim().isEmpty()) {
            return "customer001";
        }

        // Tạo base code từ tên
        String baseCode = createBaseCodeFromName(customerName);

        // Tìm số thứ tự tiếp theo
        int nextNumber = findNextAvailableNumber(baseCode);

        // Format: baseCode + số 3 chữ số
        return baseCode + String.format("%03d", nextNumber);
    }

    private String createBaseCodeFromName(String customerName) {
        String[] words = customerName.trim().split("\\s+");

        if (words.length == 0) {
            return "customer";
        }

        StringBuilder baseCode = new StringBuilder();

        // Lấy từ cuối cùng (tên) làm phần đầu
        String lastName = removeVietnameseAccents(words[words.length - 1]).toLowerCase();
        baseCode.append(lastName);

        // Lấy chữ cái đầu của các từ trước (họ và tên đệm)
        for (int i = 0; i < words.length - 1; i++) {
            String word = removeVietnameseAccents(words[i]).toLowerCase();
            if (!word.isEmpty()) {
                baseCode.append(word.charAt(0));
            }
        }

        return baseCode.toString();
    }

    private int findNextAvailableNumber(String baseCode) {
        // Tìm tất cả mã khách hàng có cùng base code
        List<String> existingCodes = adCustomerRepository.findCustomerCodesByPattern(baseCode + "%");

        int maxNumber = 0;
        String pattern = "^" + baseCode + "(\\d{3})$";

        for (String code : existingCodes) {
            if (code.matches(pattern)) {
                try {
                    String numberPart = code.substring(baseCode.length());
                    int number = Integer.parseInt(numberPart);
                    maxNumber = Math.max(maxNumber, number);
                } catch (NumberFormatException e) {
                    // Ignore invalid codes
                }
            }
        }

        return maxNumber + 1;
    }

    private String removeVietnameseAccents(String input) {
        if (input == null) return "";

        String[][] accents = {
                {"à", "á", "ạ", "ả", "ã", "â", "ầ", "ấ", "ậ", "ẩ", "ẫ", "ă", "ằ", "ắ", "ặ", "ẳ", "ẵ"},
                {"è", "é", "ẹ", "ẻ", "ẽ", "ê", "ề", "ế", "ệ", "ể", "ễ"},
                {"ì", "í", "ị", "ỉ", "ĩ"},
                {"ò", "ó", "ọ", "ỏ", "õ", "ô", "ồ", "ố", "ộ", "ổ", "ỗ", "ơ", "ờ", "ớ", "ợ", "ở", "ỡ"},
                {"ù", "ú", "ụ", "ủ", "ũ", "ư", "ừ", "ứ", "ự", "ử", "ữ"},
                {"ỳ", "ý", "ỵ", "ỷ", "ỹ"},
                {"đ"},
                {"À", "Á", "Ạ", "Ả", "Ã", "Â", "Ầ", "Ấ", "Ậ", "Ẩ", "Ẫ", "Ă", "Ằ", "Ắ", "Ặ", "Ẳ", "Ẵ"},
                {"È", "É", "Ẹ", "Ẻ", "Ẽ", "Ê", "Ề", "Ế", "Ệ", "Ể", "Ễ"},
                {"Ì", "Í", "Ị", "Ỉ", "Ĩ"},
                {"Ò", "Ó", "Ọ", "Ỏ", "Õ", "Ô", "Ồ", "Ố", "Ộ", "Ổ", "Ỗ", "Ơ", "Ờ", "Ớ", "Ợ", "Ở", "Ỡ"},
                {"Ù", "Ú", "Ụ", "Ủ", "Ũ", "Ư", "Ừ", "Ứ", "Ự", "Ử", "Ữ"},
                {"Ỳ", "Ý", "Ỵ", "Ỷ", "Ỹ"},
                {"Đ"}
        };

        String[] replacements = {"a", "e", "i", "o", "u", "y", "d", "A", "E", "I", "O", "U", "Y", "D"};

        String result = input;
        for (int i = 0; i < accents.length; i++) {
            for (String accent : accents[i]) {
                result = result.replace(accent, replacements[i]);
            }
        }

        return result;
    }

}
