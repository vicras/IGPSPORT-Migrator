package com.vicras.igpsporthelper;


import io.restassured.path.json.JsonPath;
import lombok.SneakyThrows;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.COOKIE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class IgpsportActivityDownloader {

    private static final String IGP_SPORT_ALL_ACTIVITIES_URL = "https://i.igpsport.com/Activity/ActivityList";
    private static final String IGP_SPoRT_ACTIVITY_URL = "https://i.igpsport.com/fit/activity";

    private static final String MY_COOKIES = null;

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final String FOLDER_TO_DOWNLOAD = "routes_mon";


    public static void main(String[] args) throws Exception {
        for (int pageIndex = 1; true; pageIndex++) {
            String getAllRoutes = IGP_SPORT_ALL_ACTIVITIES_URL + "?pageindex=" + pageIndex;

            HttpRequest allRoutes = HttpRequest.newBuilder()
                    .uri(URI.create(getAllRoutes))
                    .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .header(COOKIE, MY_COOKIES)
                    .GET().build();

            HttpResponse<String> loginResponse = client.send(allRoutes, BodyHandlers.ofString());
            String stringBody = loginResponse.body();
            JsonPath fromJson = JsonPath.from(stringBody);

            List<HashMap> items = fromJson.getList("item", HashMap.class);
            String folderName = String.valueOf(pageIndex);
            items.forEach(item -> {
                int rideId = (Integer) item.get("RideId");
                String title = (String) item.get("Title");
                var track = downloadActivityTrack(rideId, title);
                saveToFile(rideId, title, track, Path.of(FOLDER_TO_DOWNLOAD, folderName));
            });
            if (items.isEmpty()) {
                return;
            }
        }
    }

    @SneakyThrows
    private static byte[] downloadActivityTrack(int rideId, String title) {
        String FIT_TYPE_NUMBER = "1";
        String downloadUrl = IGP_SPoRT_ACTIVITY_URL + "?type=" + FIT_TYPE_NUMBER + "&rideid=" + rideId;
        HttpRequest downloadRoute = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .header(COOKIE, MY_COOKIES)
                .GET().build();

        HttpResponse<byte[]> downloadResponse = client.send(downloadRoute, BodyHandlers.ofByteArray());
        return downloadResponse.body();
    }

    @SneakyThrows
    private static void saveToFile(int rideId, String title, byte[] file, Path dir) {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        Path filePath = dir.resolve(rideId + "_title_" + title + ".fit");
        Files.write(filePath, file, StandardOpenOption.CREATE_NEW);
    }
}
