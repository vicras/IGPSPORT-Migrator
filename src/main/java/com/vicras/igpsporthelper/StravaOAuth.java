package com.vicras.igpsporthelper;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.restassured.path.json.JsonPath;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

import java.awt.Desktop;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.util.function.Function.identity;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

@Slf4j
public class StravaOAuth {

    private static final String CLIENT_ID = null;
    private static final String CLIENT_SECRET = null;

    private static final int PORT = 8080;
    private static final String REDIRECT_URI = "http://localhost:" + PORT + "/exchange_token";
    private static final String STRAVA_AUTH_TOKEN_URL = "https://www.strava.com/oauth";

    private static final String ACCESS_TOKEN = "access_token";

    public static void main(String[] args) throws Exception {
        // 1. Запускаем локальный сервер
        runServiceForThePostPartOfOAuth();

        // 2. Открываем ссылку на авторизацию
        openStravaAuthPage();
    }

    private static void openStravaAuthPage() throws IOException, URISyntaxException {
        String authUrl = STRAVA_AUTH_TOKEN_URL + "/authorize?client_id=" + CLIENT_ID +
                "&response_type=code" +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&approval_prompt=force" +
                "&scope=activity:write";

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI(authUrl));
        } else {
            System.out.println("Open in browser: " + authUrl);
        }
    }

    private static void runServiceForThePostPartOfOAuth() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/exchange_token", exchange -> {

            URI requestURI = exchange.getRequestURI();
            getCodeParam(requestURI.getQuery())
                    .peek(ignore -> StravaOAuth.sendBackAuthSuccessMessage(exchange))
                    .flatMap(StravaOAuth::exchangeCodeToToken)
                    .peek(token -> log.info("Token: {}", token));
            server.stop(0);
        });
        server.start();
    }

    private static Option<String> exchangeCodeToToken(String code) {
        String body = "client_id=" + CLIENT_ID +
                "&client_secret=" + CLIENT_SECRET +
                "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                "&grant_type=authorization_code";

        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(STRAVA_AUTH_TOKEN_URL + "/token"))
                .header(CONTENT_TYPE, APPLICATION_FORM_URLENCODED_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();


        return Try.of(() -> HttpClient.newHttpClient().send(tokenRequest, HttpResponse.BodyHandlers.ofString()))
                .onSuccess(response -> log.info("TOKEN RESPONSE: {}", response.body()))
                .map(StravaOAuth::getTokenFromResponse)
                .toOption()
                .flatMap(identity());
    }

    private static Option<String> getTokenFromResponse(HttpResponse<String> stringHttpResponse) {
        JsonPath responseJson = JsonPath.from(stringHttpResponse.body());
        return Option.of(responseJson.get(ACCESS_TOKEN));
    }

    private static void sendBackAuthSuccessMessage(HttpExchange exchange) {
        Try.withResources(exchange::getResponseBody)
                .of(os -> {
                    String responseText = "Authorization successful! You can close this window.";
                    exchange.sendResponseHeaders(200, responseText.getBytes().length);
                    os.write(responseText.getBytes());
                    return null;
                }).onFailure(ex -> log.error("Failed to send back auth success message", ex));
    }

    private static Option<String> getCodeParam(String query) {
        return Stream.ofAll(Arrays.stream(query.split("&")))
                .map(param -> param.split("="))
                .find(param -> "code".equals(param[0])) // check by param name
                .map(param -> param[1]); // get the code value
    }
}