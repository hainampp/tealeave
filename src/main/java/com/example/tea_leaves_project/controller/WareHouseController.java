package com.example.tea_leaves_project.controller;

import com.example.tea_leaves_project.dto.QRScannerData;
import com.example.tea_leaves_project.exception.ApiException;
import com.example.tea_leaves_project.payload.Request.WeighRequest;
import com.example.tea_leaves_project.service.WarehouseService;
import com.example.tea_leaves_project.util.JwtUtilHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

@RestController
@RequestMapping("/warehouse")
public class WareHouseController {
    @Autowired
    JwtUtilHelper jwtUtil;
    @Autowired
    WarehouseService warehouseService;
    private String getTokenFromHeader(WebRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    @GetMapping("/allwarehouse")
    public ResponseEntity<?> getAllWarehouse(WebRequest request){
        String token=getTokenFromHeader(request);
        if(token==null){
            throw ApiException.ErrForbidden().build();
        }
        if(!jwtUtil.verifyToken(token)){
            throw ApiException.ErrForbidden().build();
        }
        return new ResponseEntity<>(warehouseService.getAllWarehouse(), HttpStatus.OK);
    }
    @GetMapping("/allpackage/{warehouseid}")
    public ResponseEntity<?> getallPackageByWarehouse(WebRequest request, @PathVariable long warehouseid) {
        String token=getTokenFromHeader(request);
        if(token==null){
            throw ApiException.ErrForbidden().build();
        }
        if(!jwtUtil.verifyToken(token)){
            throw ApiException.ErrForbidden().build();
        }
        return new ResponseEntity<>(warehouseService.getPackageByWarehouse(warehouseid), HttpStatus.OK);
    }
    @PostMapping("/weigh")
    public  ResponseEntity<?> weighPackage(@RequestBody WeighRequest weighRequest) {
        weighRequest.setWeight(weighRequest.getWeight()/1000);
        return new ResponseEntity<>(warehouseService.Weigh(weighRequest), HttpStatus.OK);
    }
    @PutMapping("/scan")
    public  ResponseEntity<?> scanPackage(@RequestParam String scancode,@RequestBody QRScannerData data) {
        return new ResponseEntity<>(warehouseService.scanQrCode(scancode,data), HttpStatus.OK);
    }


}
