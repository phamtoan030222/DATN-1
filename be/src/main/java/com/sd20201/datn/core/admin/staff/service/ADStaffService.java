package com.sd20201.datn.core.admin.staff.service;

import com.sd20201.datn.core.admin.staff.model.request.ADCreateStaff;
import com.sd20201.datn.core.admin.staff.model.request.ADStaffRequest;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface ADStaffService {


    ResponseObject<?> getAllStaff(ADStaffRequest request);

    ResponseObject<?> createStaff(ADCreateStaff request);

    ResponseObject<?> updateStaff(String id, ADCreateStaff request);

    ResponseObject<?> changeStatusStaff(String id);
}
