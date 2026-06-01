package com.diplom.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class UpdateProducerRequest {
    private String name;
    private String contactPerson;
    private String email;
    private String phone;
    private String password;
    private Boolean isActive;
}