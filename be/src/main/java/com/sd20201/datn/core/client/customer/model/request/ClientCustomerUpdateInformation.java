package com.sd20201.datn.core.client.customer.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClientCustomerUpdateInformation {

    String fullName;

    String phoneNumber;

    String email;

}
