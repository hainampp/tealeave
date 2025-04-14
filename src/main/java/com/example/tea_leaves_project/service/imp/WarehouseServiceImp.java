package com.example.tea_leaves_project.service.imp;

import com.example.tea_leaves_project.payload.Response.QrResponse;
import com.example.tea_leaves_project.dto.PackageDto;
import com.example.tea_leaves_project.dto.QRScannerData;
import com.example.tea_leaves_project.dto.WarehouseDto;
import com.example.tea_leaves_project.dto.WarehousePackageDto;
import com.example.tea_leaves_project.exception.ApiException;
import com.example.tea_leaves_project.entity.Package;
import com.example.tea_leaves_project.entity.Warehouse;
import com.example.tea_leaves_project.payload.Request.WeighRequest;
import com.example.tea_leaves_project.payload.ResponseData;
import com.example.tea_leaves_project.repository.PackageRepository;
import com.example.tea_leaves_project.repository.WarehouseRepository;
import com.example.tea_leaves_project.service.WarehouseService;
import com.example.tea_leaves_project.service.helper.QRServiceHelper;
import com.example.tea_leaves_project.service.helper.SendSSEHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class WarehouseServiceImp implements WarehouseService {
    @Autowired
    WarehouseRepository warehouseRepository;
    @Autowired
    PackageRepository packageRepository;
    @Autowired
    QRServiceHelper qrServiceHelper;
    @Autowired
    SendSSEHelper sendSSEHelper;
    // tính số bao chè sẵn sàng vận chuyển
    public long calculateTotalPackage(Warehouse warehouse) {
        long sum = 0;
        for (Package p : warehouse.getPackages()) {
            if (p.getStatus().equals("Chờ vận chuyển")) sum++;
        }
        return sum;
    }

    @Override
    public List<WarehouseDto> getAllWarehouse() {
        List<Warehouse> warehouses = warehouseRepository.findAll();
        List<WarehouseDto> warehouseDtos = new ArrayList<>();
        for (Warehouse warehouse : warehouses) {
            WarehouseDto warehouseDto = WarehouseDto.builder()
                    .warehouseid(warehouse.getWarehouseid())
                    .name(warehouse.getName())
                    .address(warehouse.getAddress())
                    .lat(warehouse.getLat())
                    .lon(warehouse.getLon())
                    .totalpackage(calculateTotalPackage(warehouse))
                    .currentcapacity(warehouse.getCurrent_capacity())
                    .totalcapacity(warehouse.getTotal_capacity())
                    .build();
            warehouseDtos.add(warehouseDto);
        }
        return warehouseDtos;
    }

    @Override
    public WarehousePackageDto getPackageByWarehouse(long warehouseid) {
        double current_weigh=0.0;
        Warehouse warehouse = warehouseRepository.findByWarehouseid(warehouseid);

        if (warehouse == null) {
            throw ApiException.ErrDataLoss().build();
        }

        List<Package> plist = packageRepository.findByWarehouse(warehouse);
        List<PackageDto> packageDtoList = new ArrayList<>();

        for (Package p : plist) {

            PackageDto packageDto = PackageDto.builder()
                    .packageId(p.getPackageid())
                    .fullname(p.getUser().getFullname())
                    .warehouse(p.getWarehouse().getName())
                    .createdtime(p.getCreatedtime())
                    .weightime(p.getWeightime())
                    .capacity(p.getCapacity())
                    .unit(p.getUtil())
                    .status(p.getStatus())
                    .teacode(p.getTypetea().getTeacode())
                    .build();
            packageDtoList.add(packageDto);

            current_weigh+=p.getCapacity();
        }
        WarehousePackageDto warehousePackageDto = WarehousePackageDto.builder()
                .warehouseid(warehouseid)
                .name(warehouse.getName())
                .packages(packageDtoList).build();
        // sắp xếp giảm dần
        log.info("{} : {}",warehouse.getName(),current_weigh);
        warehouse.setCurrent_capacity(current_weigh);
        warehouseRepository.save(warehouse);

        Collections.sort(packageDtoList,(p1, p2) ->p1.getPackageId()>p2.getPackageId()? -1 : 1 );

        return warehousePackageDto;
    }
    @Override
    public QrResponse scanQrCode(String scancode, QRScannerData data) {
        QrResponse qrres= new QrResponse();
//        Warehouse warehouse=warehouseRepository.findByScancode(scancode);

        QrResponse qrResponse = qrServiceHelper.unpack(data.getQrCode(),qrres);
        Package p = packageRepository.findByPackageid(qrResponse.getPackageid());

//        if(warehouse==null){
//            log.info("[WarehouseService- scanQrCode] Không tìm thấy Warehouse với {}",scancode);
//            qrResponse.setMessage("Không tồn tại kho");
//            return qrResponse;
//        }

        if (p == null) {
            qrResponse.setMessage("Không tồn tại bao chè theo QR Code");
            return qrResponse;
        }

//        if(!p.getWarehouse().getScancode().equals(scancode)){
//            log.info("[WarehouseService- scanQrCode] Lỗi {} của {} và kho hiện tại( {} ) không trùng nhau ",p.getWarehouse().getName(),p.getPackageid(),warehouse.getName());
//            qrResponse.setMessage("Lỗi kho không trùng nhau");
//            return qrResponse;
//        }

        if (p.getStatus().equals("Chưa cân")) {
            p.setStatus("Đã quét");
            p.setTemperature(data.getTemperature());
            p.setHumidity(data.getHumidity());
            packageRepository.save(p);
            qrResponse.setMessage("Quét thành công");
        }
        if (p.getStatus().equals("Đã quét") || p.getStatus().equals("Chờ vận chuyển")) {
            qrResponse.setMessage("Sản phẩm đã được quét");
        }

        //send notice when scan success
        log.info("send scan");
        sendSSEHelper.notifyQrCodeScanned(p.getUser().getUserid(),p.getUser().getFullname());

        return qrResponse;
    }

    @Override
    public ResponseData Weigh(WeighRequest weighRequest) {
        System.out.println(weighRequest.getWeight() + " " + weighRequest.getBin_code());
        String bin_code = weighRequest.getBin_code();
        ResponseData responseData = new ResponseData();
        ResponseData.resp();
        Warehouse warehouse = warehouseRepository.findByBincode(bin_code);
        if (warehouse == null) {
            responseData.setMessage("Khong ton tai can");
            return responseData;
        }
        if (warehouse == null) {
            throw ApiException.ErrDataLoss().build();
        }
        List<Package> p = packageRepository.findByStatusAndWarehouse("Đã quét", warehouse);
        if (p.size() == 0) {
            responseData.setMessage("Không tìm thấy bao scan gần nhất");
            System.out.println("Không tìm thấy bao scan gần nhất");
            return responseData;
        }
        if (p.size() > 1) {
            for (Package ps : p) {
                ps.setStatus("Chưa cân");
                packageRepository.save(ps);
            }
            Package plast = p.getLast();
            plast.setStatus("Đã quét");
            packageRepository.save(plast);
        }
        Package plast = p.getLast();
        plast.setStatus("Chờ vận chuyển");
        plast.setCapacity(weighRequest.getWeight());
        packageRepository.save(plast);
        warehouse.setCurrent_capacity(warehouse.getCurrent_capacity() + weighRequest.getWeight());
        warehouseRepository.save(warehouse);
        responseData.setMessage("Cân thành công");
        log.info("send weigh");
        // send notice when weigh success
        sendSSEHelper.notifyWeigh(plast.getUser().getUserid(),plast.getUser().getFullname(),weighRequest.getWeight());
        return responseData;
    }
}
