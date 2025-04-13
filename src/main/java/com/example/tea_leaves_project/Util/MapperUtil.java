package com.example.tea_leaves_project.Util;

import com.example.tea_leaves_project.Exception.ApiException;
import com.example.tea_leaves_project.Payload.ResponseData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MapperUtil {

    public static final ObjectMapper mapper = new ObjectMapper();

    public static <T> T parseJson(String json, Class<T> clazz) {
        try {
            // Làm sạch chuỗi JSON trước khi parse
            json = cleanJsonString(json);
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Parse json error: {}", e.getMessage(), e);
            // Ném ApiException với thông báo lỗi chi tiết hơn
            throw ApiException.ErrDataLoss().build();
        }
    }

    /**
     * Làm sạch chuỗi JSON, loại bỏ hoặc thay thế ký tự điều khiển không hợp lệ
     */
    private static String cleanJsonString(String json) {
        if (json == null) {
            return null;
        }

        // Thay thế tất cả ký tự điều khiển không phải dấu cách bằng khoảng trắng
        String cleaned = json.replaceAll("[\\p{Cntrl}&&[^\t\n\r]]", "");

        // Log để debug
        if (!cleaned.equals(json)) {
            log.debug("Cleaned JSON string from control chars: [{}] -> [{}]", json, cleaned);
        }

        return cleaned;
    }

    public static String writeValueAsString(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Write to json error", e);
            return "Write to json error";
        }
    }
}