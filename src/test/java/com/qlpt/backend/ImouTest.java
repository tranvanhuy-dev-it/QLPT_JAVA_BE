package com.qlpt.backend;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.UUID;

public class ImouTest {

    private final String appId = "lcb6166cc4e5cc419d";
    private final String appSecret = "b46821855edd4845a53b7f1dda59f5";
    private final String apiUrl = "https://openapi-sg.easy4ip.com/openapi";

    @Test
    public void testImouApi() throws Exception {
        long time = System.currentTimeMillis() / 1000;
        String nonce = UUID.randomUUID().toString().replace("-", "");
        
        // Let's test the current format
        String signRawCurrent = "time:" + time + ",nonce:" + nonce + ",appSecret:" + appSecret;
        String signCurrent = calculateMd5(signRawCurrent);
        
        // Let's test the proposed format: time,nonce,appSecret
        String signRawProposed = time + "," + nonce + "," + appSecret;
        String signProposed = calculateMd5(signRawProposed);
        
        System.out.println("=== TESTING WITH CURRENT SIGNATURE FORMAT ===");
        callApi(signCurrent, time, nonce);
        
        System.out.println("=== TESTING WITH PROPOSED SIGNATURE FORMAT ===");
        callApi(signProposed, time, nonce);
    }

    private void callApi(String sign, long time, String nonce) throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
                
        String jsonString = "{"
                + "\"id\":\"" + UUID.randomUUID().toString() + "\","
                + "\"system\":{"
                + "  \"ver\":\"1.1\","
                + "  \"appId\":\"" + appId + "\","
                + "  \"sign\":\"" + sign + "\","
                + "  \"time\":" + time + ","
                + "  \"nonce\":\"" + nonce + "\""
                + "},"
                + "\"params\":{}"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/accessToken"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonString))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Response status code: " + response.statusCode());
        System.out.println("Response body: " + response.body());
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
            throw new RuntimeException(e);
        }
    }
}
