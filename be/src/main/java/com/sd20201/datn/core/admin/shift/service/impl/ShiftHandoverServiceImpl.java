package com.sd20201.datn.core.admin.shift.service.impl;

import com.sd20201.datn.core.admin.hoadon.repository.ADInvoiceRepository; // Import Repository Hóa đơn chuẩn
import com.sd20201.datn.core.admin.shift.model.request.EndShiftRequest;
import com.sd20201.datn.core.admin.shift.model.request.StartShiftRequest;
import com.sd20201.datn.core.admin.shift.model.response.ShiftHandoverResponse;
import com.sd20201.datn.core.admin.shift.repository.AdShiftHandoverRepository;
import com.sd20201.datn.core.admin.shift.service.ShiftHandoverService;
import com.sd20201.datn.core.common.base.PageableRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Account;
import com.sd20201.datn.entity.ShiftHandover;
import com.sd20201.datn.entity.Staff;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.repository.AccountRepository;
import com.sd20201.datn.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ShiftHandoverServiceImpl implements ShiftHandoverService {

    private final AdShiftHandoverRepository shiftRepo;
    private final AccountRepository accountRepo;
    private final StaffRepository staffRepo;
    private final ADInvoiceRepository adInvoiceRepo; // Repository tính tiền & đếm đơn

    public ShiftHandoverServiceImpl(AdShiftHandoverRepository shiftRepo,
                                    @Qualifier("accountRepository") AccountRepository accountRepo,
                                    @Qualifier("staffRepository") StaffRepository staffRepo,
                                    ADInvoiceRepository adInvoiceRepo) {
        this.shiftRepo = shiftRepo;
        this.accountRepo = accountRepo;
        this.staffRepo = staffRepo;
        this.adInvoiceRepo = adInvoiceRepo;
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
            // 1. Tính tổng tiền (Trạng thái HOÀN THÀNH = 4)
            BigDecimal currentRevenue = adInvoiceRepo.sumTotalAmountByShiftId(shift.getId());
            shift.setTotalCashAmount(currentRevenue == null ? BigDecimal.ZERO : currentRevenue);

            // 👇 2. ĐẾM SỐ LƯỢNG HÓA ĐƠN (Trạng thái HOÀN THÀNH = 4)
            Integer totalBills = adInvoiceRepo.countTotalInvoices(shift.getId());
            shift.setTotalBills(totalBills == null ? 0 : totalBills);

            // Có thể save hoặc không (tùy nghiệp vụ), ở đây chỉ cần hiển thị nên không bắt buộc save

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

        // Set mặc định ban đầu là 0
        shift.setTotalCashAmount(BigDecimal.ZERO);
        shift.setRealCashAmount(BigDecimal.ZERO);
        shift.setTotalBills(0); // Số lượng đơn ban đầu = 0

        shift.setStatus(EntityStatus.ACTIVE);

        return new ResponseObject<>(new ShiftHandoverResponse(shiftRepo.save(shift)), HttpStatus.OK, "Bắt đầu ca thành công");
    }

    @Override
    @Transactional
    public ResponseObject<ShiftHandoverResponse> endShift(EndShiftRequest req) {
        ShiftHandover shift = shiftRepo.findById(req.getShiftId()).orElse(null);
        if (shift == null) return new ResponseObject<>(null, HttpStatus.NOT_FOUND, "Không tìm thấy ca");

        // 1. Tính lại tiền lần cuối trước khi chốt
        BigDecimal finalRevenue = adInvoiceRepo.sumTotalAmountByShiftId(shift.getId());
        shift.setTotalCashAmount(finalRevenue == null ? BigDecimal.ZERO : finalRevenue);

        // 👇 2. Đếm lại số đơn lần cuối trước khi chốt
        Integer totalBills = adInvoiceRepo.countTotalInvoices(shift.getId());
        shift.setTotalBills(totalBills == null ? 0 : totalBills);

        shift.setEndTime(LocalDateTime.now());
        shift.setRealCashAmount(req.getRealCash());
        shift.setStatus(EntityStatus.INACTIVE);

        if (req.getNote() != null && !req.getNote().isEmpty()) {
            String oldNote = shift.getNote() == null ? "" : shift.getNote();
            shift.setNote(oldNote + " | " + req.getNote());
        }

        ShiftHandover saved = shiftRepo.save(shift);
        return new ResponseObject<>(new ShiftHandoverResponse(saved), HttpStatus.OK, "Kết ca thành công");
    }

    @Override
    public ResponseObject<Page<ShiftHandoverResponse>> getShiftHistory(PageableRequest request) {
        return null;
    }
}