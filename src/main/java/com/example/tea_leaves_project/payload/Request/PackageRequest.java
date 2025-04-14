package com.example.tea_leaves_project.payload.Request;

import lombok.Data;

import java.util.Date;

@Data
public class PackageRequest {
    private long warehouseid;
    private Date createdtime;
    private long typeteaid;
}
