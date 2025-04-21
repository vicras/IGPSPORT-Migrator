package com.vicras.igpsporthelper;

import lombok.SneakyThrows;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Stream;

public class StravaChecker {
    private static final String ACCESS_TOKEN = null;

    @SneakyThrows
    public static void main(String[] args) {
        HttpClient client = HttpClient.newHttpClient();
        String uploadId = "15201629570";
        HttpRequest checkRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://www.strava.com/api/v3/uploads/" + uploadId))
                .header("Authorization", "Bearer " + ACCESS_TOKEN)
                .GET()
                .build();

        HttpResponse<String> checkResponse = client.send(checkRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status check: " + checkResponse.body());
    }
}
