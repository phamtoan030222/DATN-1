package com.sd20201.datn.core.client.customer.service.impl;

import com.sd20201.datn.core.client.customer.repository.ClientCustomerCartRepository;
import com.sd20201.datn.core.client.customer.service.ClientCustomerCartService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Cart;
import com.sd20201.datn.repository.CustomerRepository;
import com.sd20201.datn.utils.UserContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientCustomerCartServiceImpl implements ClientCustomerCartService {

    private final ClientCustomerCartRepository cartRepository;

    private final UserContextHelper userContextHelper;

    private final CustomerRepository customerRepository;

    @Override
    public ResponseObject<?> getCartByCustomer(String idCustomer) {
        return cartRepository.findByCustomerId(idCustomer)
                .map(response -> ResponseObject.successForward(response.getId(), "OKE"))
                .orElseGet(() -> {
                    Cart cart = new Cart();

                    cart.setCustomer(
                            userContextHelper.getCurrentUserId().flatMap(customerRepository::findById)
                                    .orElse(null));
                    cartRepository.save(cart);
                    return ResponseObject.successForward(cart.getId(), "OKE");
                });
    }

}
