package com.ordering.dto;

import lombok.Data;


import java.util.List;

@Data
public class ComboDTO {

    private Long id;
    private String name;
    private String nameEn;
    private Double price;
    private String description;
    private String status;
    private List<ComboItemDTO> items;
}
