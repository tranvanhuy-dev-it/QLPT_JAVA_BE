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

    @Value("${app.imou.api-url:https://openapi-sg.easy4ip.com/openapi}")
    private String apiUrl;

    public ImouCloudService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // =========================
    // MAIN API
    // =========================
    public void bindDevice(String serialNumber, String safetyCode) {
        if (isBlank(appId) || isBlank(appSecret)) {
            log.warn("IMOU chưa cấu hình AppID/Secret. Đang chạy ở CHẾ ĐỘ MÔ PHỎNG. Bỏ qua bindDevice.");
            return;
        }

        try {
            log.info("Đang lấy Access Token từ Imou Cloud để liên kết thiết bị...");
            String accessToken = getAccessToken();
            if (accessToken == null) {
                throw new RuntimeException("Không lấy được accessToken để liên kết thiết bị");
            }
            
            log.info("Đang gửi yêu cầu liên kết thiết bị {} với Imou Cloud...", serialNumber);
            Map<String, Object> params = new HashMap<>();
            params.put("token", accessToken);
            params.put("deviceId", serialNumber);
            params.put("code", safetyCode);

            JsonNode res = sendPostRequest("/bindDevice", params);
            if (res != null && res.has("result")) {
                JsonNode resultNode = res.get("result");
                String code = resultNode.get("code").asText();
                if ("0".equals(code)) {
                    log.info("Liên kết thiết bị {} thành công.", serialNumber);
                } else if ("DV1015".equals(code)) {
                    log.info("Thiết bị {} đã được liên kết với tài khoản này từ trước.", serialNumber);
                } else {
                    String msg = resultNode.get("msg").asText();
                    log.error("Liên kết thiết bị {} thất bại: {} - {}", serialNumber, code, msg);
                    throw new RuntimeException("Imou Cloud: " + msg + " (" + code + ")");
                }
            } else {
                throw new RuntimeException("Không nhận được phản hồi từ Imou API");
            }
        } catch (Exception e) {
            log.error("Lỗi khi gọi API bindDevice cho thiết bị {}: ", serialNumber, e);
            throw new RuntimeException("Lỗi liên kết thiết bị Imou: " + e.getMessage(), e);
        }
    }

    public String getLiveStreamUrl(String serialNumber, String safetyCode) {
        if (isBlank(appId) || isBlank(appSecret)) {
            log.warn("IMOU chưa cấu hình → fallback test stream");
            return "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8";
        }

        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                throw new RuntimeException("Không lấy được accessToken");
            }

            String url = fetchLiveStreamUrl(accessToken, serialNumber);
            return sanitizeUrl(url);
        } catch (Exception e) {
            log.error("IMOU ERROR khi lấy live stream URL cho {}: ", serialNumber, e);
            throw new RuntimeException("Lỗi lấy luồng Imou: " + e.getMessage(), e);
        }
    }

    // =========================
    // ACCESS TOKEN
    // =========================
    private String getAccessToken() throws Exception {

        Map<String, Object> params = new HashMap<>();

        JsonNode response = sendPostRequest("/accessToken", params);

        if (isSuccess(response)) {
            return response.get("result")
                    .get("data")
                    .get("accessToken")
                    .asText();
        }

        log.error("AccessToken fail: {}", response);
        return null;
    }

    // =========================
    // BIND DEVICE (FIX LOGIC)
    // =========================
    private void bindDevice(String token, String deviceId, String code) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("token", token);
            params.put("deviceId", deviceId);
            params.put("code", code);

            JsonNode res = sendPostRequest("/bindDevice", params);

            if (!isSuccess(res)) {
                log.warn("BindDevice warning: {}", res);
            }

        } catch (Exception e) {
            log.warn("BindDevice error (ignored): ", e);
        }
    }

    // =========================
    // GET STREAM
    // =========================
    private String fetchLiveStreamUrl(String token, String deviceId) throws Exception {

        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("deviceId", deviceId);
        params.put("channelId", "0");
        params.put("liveType", "1");

        JsonNode res = sendPostRequest("/getLiveStreamInfo", params);

        if (isSuccess(res)) {

            JsonNode data = res.get("result").get("data");

            // case 1: streams[]
            if (data.has("streams") && data.get("streams").isArray()) {
                JsonNode streams = data.get("streams");
                if (streams.size() > 0 && streams.get(0).has("hlsUrl")) {
                    return streams.get(0).get("hlsUrl").asText();
                }
            }

            // case 2: direct hlsUrl
            if (data.has("hlsUrl")) {
                return data.get("hlsUrl").asText();
            }
        }

        log.error("GetLiveStream fail: {}", res);
        throw new RuntimeException("No HLS URL from IMOU");
    }

    // =========================
    // HTTP REQUEST
    // =========================
    private JsonNode sendPostRequest(String endpoint, Map<String, Object> params) throws Exception {
        String cleanAppId = appId != null ? appId.trim() : "";
        String cleanAppSecret = appSecret != null ? appSecret.trim() : "";

        long time = System.currentTimeMillis() / 1000;
        String nonce = UUID.randomUUID().toString().replace("-", "");

        // 🔥 FIX SIGN (chuẩn Imou Cloud API v1.1: time:{time},nonce:{nonce},appSecret:{appSecret})
        String signRaw = "time:" + time + ",nonce:" + nonce + ",appSecret:" + cleanAppSecret;
        String sign = md5(signRaw);

        log.info("Imou Request /{} -> appId length: {}, appSecret length: {}, time: {}, nonce: {}, sign: {}", 
                 endpoint, cleanAppId.length(), cleanAppSecret.length(), time, nonce, sign);

        Map<String, Object> system = new HashMap<>();
        system.put("ver", "1.1");
        system.put("appId", cleanAppId);
        system.put("sign", sign);
        system.put("time", time);
        system.put("nonce", nonce);

        Map<String, Object> body = new HashMap<>();
        body.put("id", UUID.randomUUID().toString());
        body.put("system", system);
        body.put("params", params);

        String json = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("IMOU RESPONSE {} => {}", endpoint, response.body());

        if (response.statusCode() != 200) {
            log.error("HTTP ERROR {} => {}", endpoint, response.statusCode());
            return null;
        }

        return objectMapper.readTree(response.body());
    }

    // =========================
    // HELPERS
    // =========================
    private boolean isSuccess(JsonNode node) {
        return node != null
                && node.has("result")
                && "0".equals(node.get("result").get("code").asText());
    }

    private String sanitizeUrl(String url) {
        if (url == null)
            return null;

        // 1. Ghi đè host chưa phân giải sang host của API gateway hoạt động thực tế
        try {
            URI hlsUri = URI.create(url);
            URI apiUri = URI.create(apiUrl);
            String apiHost = apiUri.getHost();
            if (apiHost != null && !apiHost.isEmpty()) {
                String originalHost = hlsUri.getHost();
                if (originalHost != null && !originalHost.equals(apiHost)) {
                    url = url.replace(originalHost, apiHost);
                    log.info("Ghi đè host stream HLS từ {} thành {}", originalHost, apiHost);
                }
            }
        } catch (Exception e) {
            log.warn("Không thể chuyển đổi host cho stream URL: {}", url, e);
        }

        // 2. FIX lỗi admin:pass@ gây ERR_NAME_NOT_RESOLVED
        return url.replaceAll("https://.*@", "https://");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes("UTF-8"));

            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}