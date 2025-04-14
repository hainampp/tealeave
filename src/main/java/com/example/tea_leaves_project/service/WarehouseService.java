package com.example.tea_leaves_project.service;

import com.example.tea_leaves_project.payload.Response.QrResponse;
import com.example.tea_leaves_project.dto.QRScannerData;
import com.example.tea_leaves_project.dto.WarehouseDto;
import com.example.tea_leaves_project.dto.WarehousePackageDto;
import com.example.tea_leaves_project.payload.Request.WeighRequest;
import com.example.tea_leaves_project.payload.ResponseData;

import java.util.List;

public interface WarehouseService {
    List<WarehouseDto> getAllWarehouse();
    WarehousePackageDto getPackageByWarehouse(long warehouseid);
    QrResponse scanQrCode(String scancode, QRScannerData qrScannerData);
    ResponseData Weigh(WeighRequest weighRequest);}
