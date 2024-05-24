package top.zedo.edgetts;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class TTSApi {
    public static final HttpClient httpClient = HttpClient.newHttpClient();
    public static final Gson gson = new Gson();


    public static List<Voice> getVoiceList() {
        URI uri;
        HttpRequest request;
        try {
            uri = new URI("https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/voices/list?trustedclienttoken=6A5AA1D4EAFF4E9FB37E23D68491D6F4");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        request = HttpRequest.newBuilder(uri)
                .GET()
                .build();

        HttpResponse<String> httpResponse;
        try {
            httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            TypeToken<List<Voice>> typeToken = new TypeToken<>() {
            };
            return gson.fromJson(httpResponse.body(), typeToken.getType());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
