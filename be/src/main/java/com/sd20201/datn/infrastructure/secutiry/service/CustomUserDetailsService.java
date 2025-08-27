package com.sd20201.datn.infrastructure.secutiry.service;

import com.sd20201.datn.entity.Staff;
import com.sd20201.datn.infrastructure.secutiry.repository.AuthRoleRepository;
import com.sd20201.datn.infrastructure.secutiry.repository.AuthStaffRepository;
import com.sd20201.datn.infrastructure.secutiry.user.UserPrincipal;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Setter(onMethod = @__({@Autowired}))
    private AuthStaffRepository staffRepository;

    @Setter(onMethod = @__({@Autowired}))
    private AuthRoleRepository roleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<Staff> optionalStaff = staffRepository.findByUsername(username);

        if(username != null && optionalStaff.isPresent()) {
            Staff staff = optionalStaff.get();
            List<String> role = roleRepository.getRoleCodeByUsername(username);

            return UserPrincipal.create(staff, role);
        }

        throw new UsernameNotFoundException("Username not found : " + username);
    }
}
