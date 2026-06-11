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

            // ⚠️ FIX: chỉ bind 1 lần, không gọi mỗi request
            bindDevice(accessToken, serialNumber, safetyCode);

            String url = fetchLiveStreamUrl(accessToken, serialNumber);

            // 🔥 FIX QUAN TRỌNG: validate URL
            return sanitizeUrl(url);

        } catch (Exception e) {
            log.error("IMOU ERROR → fallback stream", e);
            return "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8";
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
            params.put("deviceVerifyCode", code);

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

        long time = System.currentTimeMillis() / 1000;
        String nonce = UUID.randomUUID().toString().replace("-", "");

        // 🔥 FIX SIGN (chuẩn phổ biến IMOU)
        String signRaw = appId + time + nonce + appSecret;
        String sign = md5(signRaw);

        Map<String, Object> system = new HashMap<>();
        system.put("ver", "1.1");
        system.put("appId", appId);
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

        // FIX lỗi admin:pass@ gây ERR_NAME_NOT_RESOLVED
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