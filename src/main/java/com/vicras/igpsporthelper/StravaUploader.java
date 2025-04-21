package com.vicras.igpsporthelper;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class StravaUploader {
    private static final String ACCESS_TOKEN = null;

    public static void main(String[] args) throws IOException, InterruptedException {
        Path dir = Path.of("routes", "7");
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(f -> f.toString().endsWith(".fit"))
                    .forEach(fitFile -> {
                        try {
                            uploadFileToStrava(fitFile);
                        } catch (Exception e) {
                            System.err.println("Ошибка при загрузке " + fitFile.getFileName() + ": " + e.getMessage());
                        }
                    });
        }
    }

    private static void uploadFileToStrava(Path filePath) throws IOException, InterruptedException {
        System.out.println("Загружаем: " + filePath);

        HttpClient client = HttpClient.newHttpClient();

        // Создание multipart/form-data запроса
        String boundary = "-------------" + UUID.randomUUID();
        HttpRequest.BodyPublisher body = ofMimeMultipartData(filePath, boundary);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.strava.com/api/v3/uploads"))
                .header("Authorization", "Bearer " + ACCESS_TOKEN)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(body)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Ответ Strava: " + response.statusCode() + " " + response.body());
    }

    private static HttpRequest.BodyPublisher ofMimeMultipartData(Path filePath, String boundary) throws IOException {
        var byteArrays = new ArrayList<byte[]>();

        String fieldName = "file";
        String fileName = filePath.getFileName().toString();
        String activityName =fileName.substring(0, fileName.lastIndexOf(".")).split("_title_")[1].trim() ;

        byteArrays.add(("--" + boundary + "\r\n").getBytes());
        byteArrays.add(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n").getBytes());
        byteArrays.add(("Content-Type: application/octet-stream\r\n\r\n").getBytes());
        byteArrays.add(Files.readAllBytes(filePath));
        byteArrays.add(("\r\n").getBytes());

        byteArrays.add(("--" + boundary + "\r\n").getBytes());
        byteArrays.add(("Content-Disposition: form-data; name=\"data_type\"\r\n\r\nfit\r\n").getBytes());

        byteArrays.add(("--" + boundary + "\r\n").getBytes());
        byteArrays.add(("Content-Disposition: form-data; name=\"name\"\r\n\r\n" + activityName + "\r\n").getBytes());

        byteArrays.add(("--" + boundary + "--\r\n").getBytes());

        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }
}
