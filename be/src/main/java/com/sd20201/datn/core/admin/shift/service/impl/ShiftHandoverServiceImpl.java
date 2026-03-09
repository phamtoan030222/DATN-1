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
import com.sd20201.datn.entity.Shift;
import com.sd20201.datn.entity.ShiftHandover;
import com.sd20201.datn.entity.Staff;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.email.ShiftReportService;
import com.sd20201.datn.repository.AccountRepository;
import com.sd20201.datn.repository.LichSuThanhToanRepository;
import com.sd20201.datn.repository.ShiftRepository;
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
    private final ShiftReportService shiftReportService;
    private final ShiftRepository shiftTemplateRepo;
    private final LichSuThanhToanRepository lichSuThanhToanRepository;

    public ShiftHandoverServiceImpl(AdShiftHandoverRepository shiftRepo,
                                    @Qualifier("accountRepository") AccountRepository accountRepo,
                                    @Qualifier("staffRepository") StaffRepository staffRepo,
                                    ADInvoiceRepository adInvoiceRepo,
                                    ShiftReportService shiftReportService,
                                    ShiftRepository shiftTemplateRepo,
                                    LichSuThanhToanRepository lichSuThanhToanRepository) {
        this.shiftRepo = shiftRepo;
        this.accountRepo = accountRepo;
        this.staffRepo = staffRepo;
        this.adInvoiceRepo = adInvoiceRepo;
        this.shiftReportService = shiftReportService;
        this.shiftTemplateRepo = shiftTemplateRepo;
        this.lichSuThanhToanRepository = lichSuThanhToanRepository;
    }

    @Override
    public ResponseObject<ShiftHandoverResponse> getCurrentShift(String accountId) {
        Staff staff = staffRepo.findById(accountId).orElse(null);
        if (staff == null || staff.getAccount() == null) {
            return new ResponseObject<>(null, HttpStatus.NO_CONTENT, "Chưa có ca làm việc");
        }

        String realAccountId = staff.getAccount().getId();
        ShiftHandover shift = shiftRepo.findOpenShiftByAccountId(realAccountId).orElse(null);

        if (shift != null) {
            // 1. TÍNH DOANH THU TIỀN MẶT (Bằng ID Ca làm việc)
            BigDecimal cashRevenue = lichSuThanhToanRepository.sumAmountByShiftIdAndMethod(shift.getId(), "TIEN_MAT");
            cashRevenue = (cashRevenue == null) ? BigDecimal.ZERO : cashRevenue;

            // 2. TÍNH DOANH THU CHUYỂN KHOẢN (Bằng ID Ca làm việc)
            BigDecimal transferRevenue = lichSuThanhToanRepository.sumAmountByShiftIdAndMethod(shift.getId(), "VNPAY");
            transferRevenue = (transferRevenue == null) ? BigDecimal.ZERO : transferRevenue;

            // 3. TỔNG TIỀN MẶT HỆ THỐNG = Đầu ca + Doanh thu tiền mặt
            BigDecimal initialCash = shift.getInitialCash() != null ? shift.getInitialCash() : BigDecimal.ZERO;
            BigDecimal expectedCash = initialCash.add(cashRevenue);

            shift.setTotalCashAmount(expectedCash);
            shift.setTotalTransferAmount(transferRevenue);

            // 4. Tính số lượng hóa đơn
            Integer totalBills = adInvoiceRepo.countTotalInvoices(shift.getId());
            shift.setTotalBills(totalBills == null ? 0 : totalBills);

            return new ResponseObject<>(new ShiftHandoverResponse(shift), HttpStatus.OK, "Đang trong ca làm việc");
        }
        return new ResponseObject<>(null, HttpStatus.NO_CONTENT, "Chưa có ca làm việc");
    }

    @Override
    @Transactional
    public ResponseObject<ShiftHandoverResponse> startShift(StartShiftRequest req) {
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

        if (shift.getEndTime() != null || shift.getStatus() == EntityStatus.INACTIVE) {
            return new ResponseObject<>(new ShiftHandoverResponse(shift), HttpStatus.OK, "Ca này đã được đóng trước đó");
        }

        shift.setEndTime(LocalDateTime.now());

        // 1. TÍNH DOANH THU TIỀN MẶT (Bằng ID Ca làm việc)
        BigDecimal cashRevenue = lichSuThanhToanRepository.sumAmountByShiftIdAndMethod(shift.getId(), "TIEN_MAT");
        cashRevenue = (cashRevenue != null) ? cashRevenue : BigDecimal.ZERO;

        // 2. TÍNH DOANH THU CHUYỂN KHOẢN (Bằng ID Ca làm việc)
        BigDecimal transferRevenue = lichSuThanhToanRepository.sumAmountByShiftIdAndMethod(shift.getId(), "VNPAY");
        transferRevenue = (transferRevenue != null) ? transferRevenue : BigDecimal.ZERO;

        // 3. Cập nhật các con số vào DB
        BigDecimal initialCash = shift.getInitialCash() != null ? shift.getInitialCash() : BigDecimal.ZERO;
        BigDecimal expectedCash = initialCash.add(cashRevenue);

        shift.setTotalCashAmount(expectedCash);
        shift.setTotalTransferAmount(transferRevenue); // Lưu tiền CK
        shift.setRealCashAmount(req.getRealCash());
        shift.setStatus(EntityStatus.INACTIVE);

        // Cập nhật tổng số hóa đơn
        Integer totalBills = adInvoiceRepo.countTotalInvoices(shift.getId());
        shift.setTotalBills(totalBills == null ? 0 : totalBills);

        // Ghi chú
        if (req.getNote() != null && !req.getNote().isEmpty()) {
            shift.setNote((shift.getNote() != null ? shift.getNote() + " | " : "") + "Kết ca: " + req.getNote());
        }

        ShiftHandover saved = shiftRepo.saveAndFlush(shift);

        // Gửi mail trong try-catch
        try {
            String fullName = saved.getName();
            String templateName = fullName != null && fullName.contains(" - ") ? fullName.split(" - ")[0].trim() : fullName;
            Shift shiftTemplate = templateName != null ? shiftTemplateRepo.findByName(templateName) : null;

            shiftReportService.sendReport("dungchoctao2001@gmail.com", saved, shiftTemplate);
        } catch (Exception e) {
            System.err.println("⚠️ Đã lưu DB nhưng lỗi gửi mail: " + e.getMessage());
        }

        return new ResponseObject<>(new ShiftHandoverResponse(saved), HttpStatus.OK, "Kết ca thành công");
    }

    @Override
    public ResponseObject<ShiftHandoverResponse> getLastClosedShift() {
        Pageable pageable = PageRequest.of(0, 1);
        List<ShiftHandover> list = shiftRepo.findLastClosedShift(pageable);

        if (list.isEmpty()) {
            ShiftHandover emptyShift = new ShiftHandover();
            emptyShift.setRealCashAmount(BigDecimal.ZERO);
            return new ResponseObject<>(new ShiftHandoverResponse(emptyShift), HttpStatus.OK, "Chưa có ca trước");
        }

        ShiftHandover lastShift = list.get(0);

        if (lastShift.getEndTime() != null && lastShift.getEndTime().toLocalDate().isEqual(LocalDate.now())) {
            return new ResponseObject<>(new ShiftHandoverResponse(lastShift), HttpStatus.OK, "Lấy ca trước cùng ngày thành công");
        } else {
            ShiftHandover emptyShift = new ShiftHandover();
            emptyShift.setRealCashAmount(BigDecimal.ZERO);
            return new ResponseObject<>(new ShiftHandoverResponse(emptyShift), HttpStatus.OK, "Ca đầu tiên trong ngày, tiền ca trước = 0");
        }
    }

    @Override
    public ResponseObject<Page<ShiftHandoverResponse>> getShiftHistory(PageableRequest baseRequest, String keyword) {
        ShiftHistoryRequest request = (ShiftHistoryRequest) baseRequest;
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize());

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        try {
            if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                startDateTime = LocalDate.parse(request.getStartDate()).atStartOfDay();
            }
            if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                endDateTime = LocalDate.parse(request.getEndDate()).atTime(23, 59, 59);
            }
        } catch (Exception e) {
            System.out.println("Lỗi parse ngày tháng: " + e.getMessage());
        }

        Page<ShiftHandover> pageData = shiftRepo.searchHistory(keyword, startDateTime, endDateTime, pageable);
        Page<ShiftHandoverResponse> responsePage = pageData.map(ShiftHandoverResponse::new);

        return new ResponseObject<>(responsePage, HttpStatus.OK, "Lấy lịch sử thành công");
    }
}