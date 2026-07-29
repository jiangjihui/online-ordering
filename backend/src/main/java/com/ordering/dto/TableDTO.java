package com.ordering.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TableDTO {

    private Long id;
    @NotBlank(message = "桌号不能为空")
    private String number;
    @NotBlank(message = "区域不能为空")
    private String area;
    @Min(value = 1, message = "容纳人数至少为1")
    private Integer capacity;
    private String status;
}
