package com.sd20201.datn.core.admin.shift.service;

import com.sd20201.datn.core.admin.shift.model.request.CreateScheduleRequest;
import com.sd20201.datn.core.common.base.ResponseObject;

import java.time.LocalDate;

public interface WorkScheduleService {
    ResponseObject<?> createSchedule(CreateScheduleRequest req);
    ResponseObject<?> getSchedules(LocalDate fromDate, LocalDate toDate);

    void deleteSchedule(String id);
}
