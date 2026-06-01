package com.diplom.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProducerDto {
    private Integer id;
    private String name;
    private String contactPerson;
    private String email;
    private String phone;
    private List<TariffDto> tariffs;
}