package com.example.tea_leaves_project.payload.Request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class WeighRequest {
    private double weight;
    private String bin_code;
}
