package com.ipt.siga_backend.dto;

import com.ipt.siga_backend.entity.CareerStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CareerStatusRequest {
    private CareerStatus status;
}