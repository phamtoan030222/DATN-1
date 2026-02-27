package com.sd20201.datn.core.admin.shift.service.impl;

import com.sd20201.datn.core.admin.hoadon.repository.ADInvoiceRepository;
import com.sd20201.datn.core.admin.shift.model.request.EndShiftRequest;
import com.sd20201.datn.core.admin.shift.model.request.ShiftHistoryRequest;
import com.sd20201.datn.core.admin.shift.model.request.StartShiftRequest;
import com.sd20201.datn.core.admin.shift.model.response.ShiftHandoverResponse;
import com.sd20201.datn.core.admin.shift.repository.AdShiftHandoverRepository;
import com.sd20201.datn.core.admin.shift.service.ShiftHandoverService;
import com.sd20201.datn.core.common.base.PageableRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Account;
import com.sd20201.datn.entity.Shift; // Import Entity Shift (Template)
import com.sd20201.datn.entity.ShiftHandover;
import com.sd20201.datn.entity.Staff;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.email.ShiftReportService; // Import Service Gửi Mail
import com.sd20201.datn.repository.AccountRepository;
import com.sd20201.datn.repository.ShiftRepository; // Import Repo của Shift Template (Bạn cần đảm bảo có file này)
import com.sd20201.datn.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShiftHandoverServiceImpl implements ShiftHandoverService {

    private final AdShiftHandoverRepository shiftRepo;
    private final AccountRepository accountRepo;
    private final StaffRepository staffRepo;
    private final ADInvoiceRepository adInvoiceRepo;

    // 👇 1. KHAI BÁO SERVICE MỚI
    private final ShiftReportService shiftReportService;
    private final ShiftRepository shiftTemplateRepo; // Repo để tìm giờ quy định của ca

    public ShiftHandoverServiceImpl(AdShiftHandoverRepository shiftRepo,
                                    @Qualifier("accountRepository") AccountRepository accountRepo,
                                    @Qualifier("staffRepository") StaffRepository staffRepo,
                                    ADInvoiceRepository adInvoiceRepo,
                                    ShiftReportService shiftReportService,
                                    ShiftRepository shiftTemplateRepo) {
        this.shiftRepo = shiftRepo;
        this.accountRepo = accountRepo;
        this.staffRepo = staffRepo;
        this.adInvoiceRepo = adInvoiceRepo;
        this.shiftReportService = shiftReportService;
        this.shiftTemplateRepo = shiftTemplateRepo;
    }

    @Override
    public ResponseObject<ShiftHandoverResponse> getCurrentShift(String accountId) {
        // ... (Logic giữ nguyên không đổi) ...
        Staff staff = staffRepo.findById(accountId).orElse(null);
        if (staff == null || staff.getAccount() == null) {
            return new ResponseObject<>(null, HttpStatus.NO_CONTENT, "Chưa có ca làm việc");
        }

        // Fix: Lấy Account ID từ Staff
        String realAccountId = staff.getAccount().getId();
        ShiftHandover shift = shiftRepo.findOpenShiftByAccountId(realAccountId).orElse(null);

        if (shift != null) {
            // 1. Tính tổng tiền
            BigDecimal currentRevenue = adInvoiceRepo.sumTotalAmountByShiftId(shift.getId());
            shift.setTotalCashAmount(currentRevenue == null ? BigDecimal.ZERO : currentRevenue);

            // 2. Đếm số lượng hóa đơn
            Integer totalBills = adInvoiceRepo.countTotalInvoices(shift.getId());
            shift.setTotalBills(totalBills == null ? 0 : totalBills);

            return new ResponseObject<>(new ShiftHandoverResponse(shift), HttpStatus.OK, "Đang trong ca làm việc");
        }
        return new ResponseObject<>(null, HttpStatus.NO_CONTENT, "Chưa có ca làm việc");
    }

    @Override
    @Transactional
    public ResponseObject<ShiftHandoverResponse> startShift(StartShiftRequest req) {
        // ... (Logic giữ nguyên không đổi) ...
        Staff staff = staffRepo.findById(req.getAccountId()).orElse(null);
        if (staff == null) return new ResponseObject<>(null, HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên");

        Account acc = staff.getAccount();
        if (acc == null) return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Nhân viên chưa có tài khoản");

        if (shiftRepo.findOpenShiftByAccountId(acc.getId()).isPresent())
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Đang trong ca!");

        ShiftHandover shift = new ShiftHandover();
        shift.setAccount(acc);
        shift.setName(req.getName());
        shift.setStartTime(req.getStartTime() != null ? req.getStartTime() : LocalDateTime.now());
        shift.setEndTime(null);
        shift.setInitialCash(req.getInitialCash());
        shift.setNote(req.getNote());
        shift.setTotalCashAmount(BigDecimal.ZERO);
        shift.setRealCashAmount(BigDecimal.ZERO);
        shift.setTotalBills(0);
        shift.setStatus(EntityStatus.ACTIVE);

        return new ResponseObject<>(new ShiftHandoverResponse(shiftRepo.save(shift)), HttpStatus.OK, "Bắt đầu ca thành công");
    }

    @Override
    @Transactional
    public ResponseObject<ShiftHandoverResponse> endShift(EndShiftRequest req) {
        ShiftHandover shift = shiftRepo.findById(req.getShiftId()).orElse(null);
        if (shift == null) return new ResponseObject<>(null, HttpStatus.NOT_FOUND, "Không tìm thấy ca");

        // ✅ FIX 1: Chặn trường hợp spam click hoặc ca đã đóng rồi
        if (shift.getEndTime() != null || shift.getStatus() == EntityStatus.INACTIVE) {
            return new ResponseObject<>(new ShiftHandoverResponse(shift), HttpStatus.OK, "Ca này đã được đóng trước đó");
        }

        // 1. Tính toán lần cuối
        BigDecimal finalRevenue = adInvoiceRepo.sumTotalAmountByShiftId(shift.getId());
        shift.setTotalCashAmount(finalRevenue == null ? BigDecimal.ZERO : finalRevenue);

        Integer totalBills = adInvoiceRepo.countTotalInvoices(shift.getId());
        shift.setTotalBills(totalBills == null ? 0 : totalBills);

        // 2. Cập nhật thông tin đóng ca
        shift.setEndTime(LocalDateTime.now());
        shift.setRealCashAmount(req.getRealCash());
        shift.setStatus(EntityStatus.INACTIVE); // Chuyển trạng thái

        // Xử lý Ghi chú
        if (req.getNote() != null && !req.getNote().isEmpty()) {
            String noteContent = req.getNote();
            if (shift.getNote() != null && !shift.getNote().isEmpty()) {
                shift.setNote(shift.getNote() + " | Kết ca: " + noteContent);
            } else {
                shift.setNote("Kết ca: " + noteContent);
            }
        }

        // ✅ FIX 2: Dùng saveAndFlush để BẮT BUỘC lưu vào DB ngay lập tức
        // Điều này đảm bảo dù phần gửi mail bên dưới có bị lỗi hay chậm, dữ liệu vẫn an toàn.
        ShiftHandover saved = shiftRepo.saveAndFlush(shift);

        // ✅ FIX 3: Gửi mail trong try-catch và xử lý logic tìm Template an toàn hơn
        try {
            String fullName = saved.getName();
            String templateName = fullName;

            // Logic tách tên: "Ca Tối - dungchoctao2k1..." -> Lấy "Ca Tối"
            if (fullName != null && fullName.contains(" - ")) {
                templateName = fullName.split(" - ")[0].trim();
            }

            // Tìm Template (Nếu là Ca Tự Do thì có thể trả về null -> Không sao cả)
            Shift shiftTemplate = null;
            if (templateName != null) {
                shiftTemplate = shiftTemplateRepo.findByName(templateName);
            }

            // Gửi mail (Class ShiftReportService đã có @Async nên sẽ không block)
            String adminEmail = "dungchoctao2001@gmail.com";
            shiftReportService.sendReport(adminEmail, saved, shiftTemplate);

        } catch (Exception e) {
            // Chỉ in log lỗi mail, KHÔNG throw exception để tránh Rollback DB
            System.err.println("⚠️ Đã lưu DB thành công nhưng lỗi gửi mail: " + e.getMessage());
            e.printStackTrace();
        }

        return new ResponseObject<>(new ShiftHandoverResponse(saved), HttpStatus.OK, "Kết ca thành công");
    }

    @Override
    public ResponseObject<ShiftHandoverResponse> getLastClosedShift() {
        Pageable pageable = PageRequest.of(0, 1);
        List<ShiftHandover> list = shiftRepo.findLastClosedShift(pageable);

        // Trường hợp cửa hàng mới tinh, chưa có ca nào
        if (list.isEmpty()) {
            ShiftHandover emptyShift = new ShiftHandover();
            emptyShift.setRealCashAmount(BigDecimal.ZERO);
            return new ResponseObject<>(new ShiftHandoverResponse(emptyShift), HttpStatus.OK, "Chưa có ca trước");
        }

        ShiftHandover lastShift = list.get(0);

        // FIX: Kiểm tra xem ca trước đó có kết thúc trong "HÔM NAY" không
        if (lastShift.getEndTime() != null && lastShift.getEndTime().toLocalDate().isEqual(LocalDate.now())) {
            // Nếu cùng ngày -> Lấy số tiền thực tế ca trước để gán cho ca này
            return new ResponseObject<>(new ShiftHandoverResponse(lastShift), HttpStatus.OK, "Lấy ca trước cùng ngày thành công");
        } else {
            // Nếu là ca của ngày hôm qua hoặc cũ hơn -> Trả về 0 đồng (ca đầu ngày)
            ShiftHandover emptyShift = new ShiftHandover();
            emptyShift.setRealCashAmount(BigDecimal.ZERO);
            return new ResponseObject<>(new ShiftHandoverResponse(emptyShift), HttpStatus.OK, "Ca đầu tiên trong ngày, tiền ca trước = 0");
        }
    }

    @Override
    public ResponseObject<Page<ShiftHandoverResponse>> getShiftHistory(PageableRequest baseRequest, String keyword) {
        // Ép kiểu baseRequest về class con của bạn
        ShiftHistoryRequest request = (ShiftHistoryRequest) baseRequest;
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize());

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        try {
            // Ép string "yyyy-MM-dd" thành thời gian đầu ngày và cuối ngày
            if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                startDateTime = LocalDate.parse(request.getStartDate()).atStartOfDay(); // 00:00:00
            }
            if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                endDateTime = LocalDate.parse(request.getEndDate()).atTime(23, 59, 59); // 23:59:59
            }
        } catch (Exception e) {
            System.out.println("Lỗi parse ngày tháng: " + e.getMessage());
        }

        // Truyền thêm 2 biến thời gian vào repo
        Page<ShiftHandover> pageData = shiftRepo.searchHistory(keyword, startDateTime, endDateTime, pageable);

        Page<ShiftHandoverResponse> responsePage = pageData.map(ShiftHandoverResponse::new);
        return new ResponseObject<>(responsePage, HttpStatus.OK, "Lấy lịch sử thành công");
    }
}