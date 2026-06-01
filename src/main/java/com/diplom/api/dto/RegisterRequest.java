package com.diplom.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    private String name;            // название организации
    private String contactPerson;
    private String email;
    private String phone;
    private String password;
}