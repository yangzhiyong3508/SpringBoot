package com.example.edog.utils;

import com.alibaba.fastjson2.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BaiduAuthService {
    private static final String API_KEY = "sov5I4aEwzZldEDgSyTmvbs6";
    private static final String SECRET_KEY = "EgZj8ypAPXLsDqPC0cyiqfqwN0rjtoGD";

    public static String getAccessToken() throws Exception {
        String url = "https://aip.baidubce.com/oauth/2.0/token" +
                "?grant_type=client_credentials" +
                "&client_id=" + API_KEY +
                "&client_secret=" + SECRET_KEY;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();

        JSONObject json = JSONObject.parseObject(response.toString());
        return json.getString("access_token");
    }
}