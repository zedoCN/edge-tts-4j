package top.zedo.edgetts;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public class TTSClient implements WebSocket.Listener {
    public static final Gson gson = new Gson();
    private static final String EDGE_ORIGIN = "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold";
    private static final String EDGE_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.74 Safari/537.36 Edg/99.0.1150.55";

    private static String makeAudioFormat(TTSCodecs outputFormat) {
        JsonObject root = new JsonObject();
        JsonObject content = new JsonObject();
        JsonObject audio = new JsonObject();
        JsonObject synthesis = new JsonObject();
        JsonObject metadataoptions = new JsonObject();
        metadataoptions.addProperty("sentenceBoundaryEnabled", false);
        metadataoptions.addProperty("wordBoundaryEnabled", true);
        audio.addProperty("outputFormat", outputFormat.getId());
        audio.add("metadataoptions", metadataoptions);
        content.add("synthesis", synthesis);
        synthesis.add("audio", audio);
        root.add("context", content);
        System.out.println(gson.toJson(root));
        return "Content-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n" + gson.toJson(root) + "\n";
    }

    public String makeSSML(String loacle, String voiceName, String content) {
        return makeSSML(loacle, voiceName, "+0Hz", "+0%", "+0%", content);
    }

    public String makeSSML(String locale, String voiceName, String voicePitch, String voiceRate, String voiceVolume, String content) {
        StringWriter stringWriter = new StringWriter();
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(stringWriter);

            xmlStreamWriter.writeStartDocument();
            xmlStreamWriter.writeStartElement("speak");
            xmlStreamWriter.writeAttribute("version", "1.0");
            xmlStreamWriter.writeAttribute("xmlns", "http://www.w3.org/2001/10/synthesis");
            xmlStreamWriter.writeAttribute("xml:lang", locale);

            xmlStreamWriter.writeStartElement("voice");
            xmlStreamWriter.writeAttribute("name", voiceName);

            xmlStreamWriter.writeStartElement("prosody");
            xmlStreamWriter.writeAttribute("pitch", voicePitch);
            xmlStreamWriter.writeAttribute("rate", voiceRate);
            xmlStreamWriter.writeAttribute("volume", voiceVolume);
            xmlStreamWriter.writeCharacters(content);
            xmlStreamWriter.writeEndElement(); // prosody

            xmlStreamWriter.writeEndElement(); // voice
            xmlStreamWriter.writeEndElement(); // speak

            xmlStreamWriter.writeEndDocument();
            xmlStreamWriter.flush();
            xmlStreamWriter.close();

        } catch (XMLStreamException e) {
            return null;
        }
        return stringWriter.getBuffer().toString();
    }

    private String ssmlHeadersPlusData(String requestId, String ssml) {
        return "X-RequestId:" + requestId + "\r\n" + "Content-Type:application/ssml+xml\r\n" + "Path:ssml\r\n\r\n" + ssml;
    }

    public static void main(String[] args) {
        TTSClient client = new TTSClient();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    HttpClient client;
    WebSocket webSocket;

    /**
     * 连接指tts服务器
     */
    public void connect() {
        client = HttpClient.newHttpClient();
        WebSocket.Builder builder = client.newWebSocketBuilder();
        builder.header("Origin", EDGE_ORIGIN);
        builder.header("Pragma", "no-cache");
        builder.header("Cache-Control", "no-cache");
        builder.header("User-Agent", EDGE_UA);
        webSocket = builder.buildAsync(URI.create("wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=6A5AA1D4EAFF4E9FB37E23D68491D6F4"), new WebSocketListener()).join();
    }

    /**
     * 说话
     *
     * @param outputFormat 输出格式
     * @param locale       语言地区
     * @param voiceName    发声人名
     * @param pitch        语调
     * @param rate         语速
     * @param volume       音量
     * @param content      内容
     * @return RequestId 请求ID
     */
    public String speak(TTSCodecs outputFormat, String locale, String voiceName, String pitch, String rate, String volume, String content) {
        return speak(outputFormat, makeSSML(locale, voiceName, pitch, rate, volume, content));
    }

    /**
     * 说话
     *
     * @param outputFormat 输出格式
     * @param ssml         Speech Synthesis Markup Language  基于XML的标记语言
     * @return RequestId 请求ID
     */
    public String speak(TTSCodecs outputFormat, String ssml) {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        webSocket.sendText(makeAudioFormat(outputFormat), true);
        webSocket.sendText(ssmlHeadersPlusData(uuid, ssml), true);
        return uuid;
    }

    private final class WebSocketListener implements WebSocket.Listener {


        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("连接成功");
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            data.order(ByteOrder.BIG_ENDIAN);
            //读取响应头长度
            int len = data.getShort();
            byte[] headBuf = new byte[len];
            data.get(headBuf);
            String header = new String(headBuf, StandardCharsets.UTF_8);
            System.out.println("音频数据：" + header);
            //读取音频数据
            byte[] dataBuf = new byte[data.remaining()];
            data.get(dataBuf);
            return WebSocket.Listener.super.onBinary(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            //System.out.println("Received message: " + data);
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("连接丢失");
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.err.println("连接错误: " + error.getMessage());
            WebSocket.Listener.super.onError(webSocket, error);
        }
    }

}
