package top.zedo.edgetts;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public class TTSClient implements WebSocket.Listener {






    static String ConvertToAudioFormatWebSocketString(String outputformat) {
        return "Content-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},\"outputFormat\":\"" + outputformat + "\"}}}}";
    }

    static String ConvertToSsmlText(String lang, String voice, String text) {
        return "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xmlns:mstts='https://www.w3.org/2001/mstts' xml:lang='" + lang + "'><voice name='" + voice + "'>" + text + "</voice></speak>";
    }

    static String ConvertToSsmlWebSocketString(String requestId, String lang, String voice, String msg) {
        return "X-RequestId:" + requestId + "\r\nContent-Type:application/ssml+xml\r\nPath:ssml\r\n\r\n" + ConvertToSsmlText(lang, voice, msg);
    }

    public static void main(String[] args) {
        TTSClient client = new TTSClient();
    }

    public TTSClient() {
        var url = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?trustedclienttoken=6A5AA1D4EAFF4E9FB37E23D68491D6F4";
        var Language = "en-US";
        var Voice = "Microsoft Server Speech Text to Speech Voice (zh-CN, XiaoxiaoNeural)";
        var audioOutputFormat = "webm-24khz-16bit-mono-opus";
        var binary_delim = "Path:audio\r\n";

        var msg = "Hello world";
        var sendRequestId = UUID.randomUUID().toString().replaceAll("-", "");


        HttpClient client = HttpClient.newHttpClient();
        WebSocket.Builder builder = client.newWebSocketBuilder();
        WebSocket webSocket = builder.buildAsync(URI.create("wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?trustedclienttoken=6A5AA1D4EAFF4E9FB37E23D68491D6F4"), new WebSocketListener()).join();
        var audioconfig = ConvertToAudioFormatWebSocketString("webm-24khz-16bit-mono-opus");
        webSocket.sendText(audioconfig, false);
        webSocket.sendText(ConvertToSsmlWebSocketString(sendRequestId, Language, Voice, msg), false);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class WebSocketListener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("WebSocket opened");
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            System.out.println("受到二进制:" + data.remaining());
            return WebSocket.Listener.super.onBinary(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            System.out.println("Received message: " + data);
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("WebSocket closed with statusCode: " + statusCode + ", reason: " + reason);
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.err.println("Error occurred: " + error.getMessage());
            WebSocket.Listener.super.onError(webSocket, error);
        }
    }

}
