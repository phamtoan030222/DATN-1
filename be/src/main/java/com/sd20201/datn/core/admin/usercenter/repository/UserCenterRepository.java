package com.sd20201.datn.core.admin.usercenter.repository;

import com.sd20201.datn.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCenterRepository extends JpaRepository<Account, String> {
    // Lưu ý: Chữ "String" ở trên giả định ID trong PrimaryEntity của bạn là String.
    // Nếu ID của bạn là Integer hay Long thì nhớ đổi lại cho khớp nhé!

    // Truy vấn thẳng vào bảng Account để lấy thông tin đăng nhập
    Optional<Account> findByUsername(String username);
}