package com.sd20201.datn.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
//import udpm.hn.server.infrastructure.security.service.TokenProvider;

@Component
public class UserContextHelper {

//    @Setter(onMethod = @__({@Autowired}))
//    private TokenProvider tokenProvider;
//
//    @Setter(onMethod = @__({@Autowired}))
//    private HttpServletRequest request;
//
//    public String getCurrentUserId() {
//        String jwt = getJwtFromRequest(request);
//        return tokenProvider.getUserIdFormToken(jwt);
//    }
//
//    public String getCurrentUserEmail() {
//        String jwt = getJwtFromRequest(request);
//        return tokenProvider.getEmailFormToken(jwt);
//    }
//
//    private String getJwtFromRequest(HttpServletRequest request) {
//        String bearerToken = request.getHeader("Authorization");
//        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
//            return bearerToken.substring(7);
//        }
//        return null;
//    }

}
