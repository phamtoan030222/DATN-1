package com.sd20201.datn.core.admin.products.harddrive.service;

import com.sd20201.datn.core.admin.products.harddrive.model.request.ADCreateHardDriverRequest;
import com.sd20201.datn.core.admin.products.harddrive.model.request.ADHardDriveRequest;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface ADHardDriveService {

    ResponseObject<?> getAllHardDrives(ADHardDriveRequest request);

    ResponseObject<?> createHardDrive(ADCreateHardDriverRequest request);

    ResponseObject<?> updateHardDriveStatus(String id);

    ResponseObject<?> updateHardDrive(String id, ADCreateHardDriverRequest request);

}
