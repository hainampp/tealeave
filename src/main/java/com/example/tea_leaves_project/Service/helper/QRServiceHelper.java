package com.example.tea_leaves_project.service.helper;

import com.example.tea_leaves_project.exception.ApiException;
import com.example.tea_leaves_project.exception.CodeResponse;
import com.example.tea_leaves_project.entity.Package;
import com.example.tea_leaves_project.entity.TypeTea;
import com.example.tea_leaves_project.entity.Users;
import com.example.tea_leaves_project.entity.Warehouse;
import com.example.tea_leaves_project.Payload.Request.PackageRequest;
import com.example.tea_leaves_project.Payload.Response.QrResponse;
import com.example.tea_leaves_project.repository.PackageRepository;
import com.example.tea_leaves_project.repository.TypeTeaRespository;
import com.example.tea_leaves_project.repository.UserRepository;
import com.example.tea_leaves_project.repository.WarehouseRepository;
import com.example.tea_leaves_project.service.imp.WarehouseServiceImp;
import com.example.tea_leaves_project.constant.QRTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class QRServiceHelper {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final TypeTeaRespository teaRepository;
    private final PackageRepository packageRepository;

    public String pack(String email, PackageRequest request){
        log.info("Generating QR Code with request: {}, email: {}", request, email);
        Users user = userRepository.findUserByEmail(email);
        if (user == null) {
            throw ApiException.ErrBadCredentials().build();
        }
        Warehouse warehouse = warehouseRepository.findByWarehouseid(request.getWarehouseid());
        TypeTea typeTea = teaRepository.findByTypeteaid(request.getTypeteaid());
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Package p = Package.builder()
                .user(user)
                .warehouse(warehouse)
                .createdtime(timestamp)
                .typetea(typeTea)
                .util("Kg")
                .status("Chưa cân")
                .build();
        Package pack = packageRepository.save(p);

        StringBuilder qrcode = new StringBuilder();
        String packageId = String.valueOf(p.getPackageid());
        if (StringUtils.isNotEmpty(packageId)){
            qrcode.append(QRTag.PACKAGE_ID)
                    .append(padLeft(packageId.length()))
                    .append(packageId);
        }

        String userId = String.valueOf(user.getUserid());
        if (StringUtils.isNotEmpty(userId)){
            qrcode.append(QRTag.USER_ID)
                    .append(padLeft(userId.length()))
                    .append(userId);
        }

        String warehouseId = String.valueOf(warehouse.getWarehouseid());
        if (StringUtils.isNotEmpty(warehouseId)){
            qrcode.append(QRTag.WAREHOUSE_ID)
                    .append(padLeft(warehouseId.length()))
                    .append(warehouseId);
        }

        String teaCode = typeTea.getTeacode();
        if (StringUtils.isNotEmpty(teaCode)){
            qrcode.append(QRTag.TEA_CODE)
                    .append(padLeft(teaCode.length()))
                    .append(teaCode);
        }

        long time = System.currentTimeMillis();
        String timestampString = String.valueOf(time);
        if (StringUtils.isNotEmpty(timestampString)){
            qrcode.append(QRTag.CREATED_TIME)
                    .append(padLeft(timestampString.length()))
                    .append(timestampString);
        }
        p.setQrcode(qrcode.toString());
        packageRepository.save(p);
        return p.getQrcode();
    }

    // call: unpack(qrCode, new QrResponse())
    public QrResponse unpack(String qrCode, QrResponse qrResponse) {
        try {
            log.info("Unpacking QR Code: {}", qrCode);

            // Kiểm tra null hoặc rỗng
            if (qrCode == null || qrCode.isEmpty()) {
                log.error("QR Code is null or empty");
                qrResponse.setMessage("QR Code không hợp lệ: rỗng hoặc null");
                return qrResponse;
            }

            // Kiểm tra độ dài QR code
            if (qrCode.length() < 4) {
                log.error("QR Code too short: {}", qrCode);
                qrResponse.setMessage("QR Code không hợp lệ: độ dài không đủ");
                return qrResponse;
            }

            // Trích xuất header và kiểm tra định dạng
            String header = qrCode.substring(0, 4);
            String tag = header.substring(0, 2);
            String size = header.substring(2);

            // Kiểm tra size có phải là số nguyên không
            int sizeValue;
            try {
                sizeValue = Integer.parseInt(size);
            } catch (NumberFormatException e) {
                log.error("Invalid size format in QR code: {}", size);
                qrResponse.setMessage("QR Code không hợp lệ: định dạng kích thước không đúng");
                return qrResponse;
            }

            // Kiểm tra độ dài QR code có đủ chứa data không
            if (qrCode.length() < 4 + sizeValue) {
                log.error("QR Code data section too short. Expected {} bytes, got {}",
                        sizeValue, qrCode.length() - 4);
                qrResponse.setMessage("QR Code không hợp lệ: phần dữ liệu không đủ độ dài");
                return qrResponse;
            }

            // Trích xuất phần body
            String body = qrCode.substring(4, 4 + sizeValue);

            // Xử lý dựa trên tag
            try {
                switch (tag) {
                    case QRTag.PACKAGE_ID:
                        try {
                            qrResponse.setPackageid(Long.parseLong(body));
                        } catch (NumberFormatException e) {
                            log.error("Invalid package ID format: {}", body);
                            qrResponse.setMessage("Định dạng ID gói hàng không hợp lệ");
                        }
                        break;
                    case QRTag.USER_ID:
                        try {
                            qrResponse.setUserid(Long.parseLong(body));
                        } catch (NumberFormatException e) {
                            log.error("Invalid user ID format: {}", body);
                            qrResponse.setMessage("Định dạng ID người dùng không hợp lệ");
                        }
                        break;
                    case QRTag.WAREHOUSE_ID:
                        try {
                            qrResponse.setWarehouseid(Long.parseLong(body));
                        } catch (NumberFormatException e) {
                            log.error("Invalid warehouse ID format: {}", body);
                            qrResponse.setMessage("Định dạng ID kho hàng không hợp lệ");
                        }
                        break;
                    case QRTag.TEA_CODE:
                        qrResponse.setTeacode(body);
                        break;
                    case QRTag.CREATED_TIME:
                        try {
                            long time = Long.parseLong(body);
                            Timestamp timestamp = new Timestamp(time);
                            qrResponse.setCreatetime(timestamp);
                        } catch (Exception ex) {
                            log.error("Parse created date exception: ", ex);
                            qrResponse.setMessage("Lỗi khi phân tích thời gian tạo");
                        }
                        break;
                    default:
                        log.warn("Unknown tag: {}", tag);
                        break;
                }

                // Xử lý phần QR code còn lại (đệ quy)
                if (4 + sizeValue < qrCode.length()) {
                    String subData = qrCode.substring(4 + sizeValue);
                    unpack(subData, qrResponse);
                }
            } catch (Exception e) {
                // Bắt tất cả các ngoại lệ khác có thể xảy ra trong quá trình xử lý
                log.error("Error processing QR code data: {}", e.getMessage(), e);
                if (qrResponse.getMessage() == null) {
                    qrResponse.setMessage("Lỗi khi xử lý QR Code");
                }
            }
        } catch (Exception e) {
            // Bắt tất cả các ngoại lệ có thể xảy ra
            log.error("Unexpected error unpacking QR code: {}", e.getMessage(), e);
            qrResponse.setMessage("Lỗi không xác định khi xử lý QR Code");
        }

        return qrResponse;
    }

    private String padLeft(int length){
        if (length > 99) {
            throw ApiException.ErrInvalidArgument()
                    .code(CodeResponse.INVALID_LENGTH)
                    .build();
        }
        return StringUtils.leftPad(String.valueOf(length), 2, "0");
    }
}
