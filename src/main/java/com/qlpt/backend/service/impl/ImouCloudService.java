package com.qlpt.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ImouCloudService {

    private static final Logger log = LoggerFactory.getLogger(ImouCloudService.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${app.imou.app-id:}")
    private String appId;

    @Value("${app.imou.app-secret:}")
    private String appSecret;

    @Value("${app.imou.api-url:https://openapi-sg.imoulife.com/openapi}")
    private String apiUrl;

    public ImouCloudService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Lấy đường dẫn luồng phát trực tuyến HLS (.m3u8) từ Imou Cloud.
     * Nếu AppId/AppSecret không được cấu hình, dịch vụ tự động hoạt động ở chế độ mô phỏng (Simulation Mode).
     */
    public String getLiveStreamUrl(String serialNumber, String safetyCode) {
        if (appId == null || appId.trim().isEmpty() || appSecret == null || appSecret.trim().isEmpty()) {
            log.warn("Imou AppID hoặc AppSecret chưa được cấu hình. Đang chạy ở CHẾ ĐỘ MÔ PHỎNG.");
            return String.format("https://openapi-sg.imoulife.com/live/HLS/%s_0.m3u8?token=simulated_token", serialNumber);
        }

        try {
            log.info("Đang lấy Access Token từ Imou Cloud...");
            String accessToken = getAccessToken();
            if (accessToken == null) {
                throw new RuntimeException("Không thể lấy Access Token từ Imou Cloud");
            }

            log.info("Đang liên kết thiết bị {} với tài khoản Imou Developer...", serialNumber);
            bindDevice(accessToken, serialNumber, safetyCode);

            log.info("Đang lấy thông tin luồng phát trực tiếp HLS cho thiết bị {}...", serialNumber);
            return fetchLiveStreamUrl(accessToken, serialNumber);
        } catch (Exception e) {
            log.error("Lỗi khi kết nối với Imou Cloud API, tự động fallback về link mô phỏng: ", e);
            return String.format("https://openapi-sg.imoulife.com/live/HLS/%s_0.m3u8?token=simulated_fallback_token", serialNumber);
        }
    }

    private String getAccessToken() throws Exception {
        Map<String, Object> params = new HashMap<>();
        JsonNode response = sendPostRequest("/accessToken", params);
        if (response != null && response.has("result")) {
            JsonNode resultNode = response.get("result");
            String code = resultNode.get("code").asText();
            if ("0".equals(code) && resultNode.has("data")) {
                return resultNode.get("data").get("accessToken").asText();
            } else {
                log.error("Imou accessToken API trả về lỗi: {} - {}", code, resultNode.get("msg").asText());
            }
        }
        return null;
    }

    private void bindDevice(String accessToken, String serialNumber, String safetyCode) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("token", accessToken);
            params.put("deviceId", serialNumber);
            params.put("deviceVerifyCode", safetyCode);

            JsonNode response = sendPostRequest("/bindDevice", params);
            if (response != null && response.has("result")) {
                JsonNode resultNode = response.get("result");
                String code = resultNode.get("code").asText();
                if ("0".equals(code)) {
                    log.info("Liên kết thiết bị thành công hoặc thiết bị đã được liên kết.");
                } else {
                    log.warn("Kết quả liên kết thiết bị (Mã: {}): {}", code, resultNode.get("msg").asText());
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi gọi API bindDevice: ", e);
        }
    }

    private String fetchLiveStreamUrl(String accessToken, String serialNumber) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("token", accessToken);
        params.put("deviceId", serialNumber);
        params.put("channelId", "0");
        params.put("liveType", "1"); // 1: HLS stream

        JsonNode response = sendPostRequest("/getLiveStreamInfo", params);
        if (response != null && response.has("result")) {
            JsonNode resultNode = response.get("result");
            String code = resultNode.get("code").asText();
            if ("0".equals(code) && resultNode.has("data")) {
                JsonNode dataNode = resultNode.get("data");
                if (dataNode.has("streams")) {
                    JsonNode streamsNode = dataNode.get("streams");
                    if (streamsNode.isArray() && streamsNode.size() > 0) {
                        // Trả về hlsUrl từ stream đầu tiên
                        return streamsNode.get(0).get("hlsUrl").asText();
                    }
                }
                if (dataNode.has("hlsUrl")) {
                    return dataNode.get("hlsUrl").asText();
                }
            }
            log.error("Imou getLiveStreamInfo API trả về lỗi: {} - {}", code, resultNode.get("msg").asText());
        }
        throw new RuntimeException("Không tìm thấy thông tin luồng phát HLS trong phản hồi từ Imou API");
    }

    private JsonNode sendPostRequest(String endpoint, Map<String, Object> params) throws Exception {
        long time = System.currentTimeMillis() / 1000;
        String nonce = UUID.randomUUID().toString().replace("-", "");
        
        // Chữ ký: md5("time:" + time + ",nonce:" + nonce + ",appSecret:" + appSecret)
        String signRaw = "time:" + time + ",nonce:" + nonce + ",appSecret:" + appSecret;
        String sign = calculateMd5(signRaw);

        Map<String, Object> systemMap = new HashMap<>();
        systemMap.put("ver", "1.1");
        systemMap.put("appId", appId);
        systemMap.put("sign", sign);
        systemMap.put("time", time);
        systemMap.put("nonce", nonce);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", UUID.randomUUID().toString());
        requestBody.put("system", systemMap);
        requestBody.put("params", params);

        String jsonString = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonString))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            log.error("API call to {} failed with HTTP status code: {}", endpoint, response.statusCode());
            return null;
        }

        return objectMapper.readTree(response.body());
    }

    private String calculateMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tính toán chữ ký MD5 cho Imou Cloud API", e);
        }
    }
}
