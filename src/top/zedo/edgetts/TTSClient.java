package top.zedo.edgetts;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import top.zedo.edgetts.info.TTSMetadataInfo;
import top.zedo.edgetts.info.TTSResponseInfo;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletionStage;

public class TTSClient implements Closeable {
    public static final Gson gson = new Gson();
    private static final String EDGE_ORIGIN = "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold";
    private static final String EDGE_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.74 Safari/537.36 Edg/99.0.1150.55";
    private final List<ITTSListener> listenerList = new ArrayList<>();
    private HttpClient httpClient;
    private WebSocket webSocket;
    private boolean isConnected = false;

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
        return "Content-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n" + gson.toJson(root) + "\n";
    }

    public static void main(String[] args) {


        TTSClient client = new TTSClient();
        client.addListener(new ITTSListener() {
            @Override
            public void onGetAudioData(String requestId, String streamId, String contentType, String codec, byte[] audioData) {
                System.out.println("音频流数据: " + audioData.length);
            }

            @Override
            public void onGetAudioDataEnd(String requestId, String streamId) {
                System.out.println("音频流结束");
            }

            @Override
            public void onConnect() {
                System.out.println("连接成功！");
            }

            @Override
            public void onDisconnect() {
                System.out.println("断开连接！");
            }

            @Override
            public void onGetTurnStart(String requestId, TTSResponseInfo info) {
                System.out.println("转换开始: " + info);
            }

            @Override
            public void onGetTurnEnd(String requestId) {
                System.out.println("转换结束");
            }

            @Override
            public void onGetResponse(String requestId, TTSResponseInfo info) {
                System.out.println("获得响应: " + info);
            }

            @Override
            public void onGetAudioMetadata(String requestId, TTSMetadataInfo info) {
                System.out.println("获得音频元数据: " + info);
            }
        });
        client.connect();
        client.speak(TTSCodecs.OPUS, "zh_CN", "zh-CN-XiaoyiNeural", "测试文本，找大神带飞歪歪频道：83082 配电脑买外设淘宝搜“创");
        new Thread(() -> {
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public static Map<String, String> parseHeaders(String response) {
        Map<String, String> headers = new HashMap<>();
        String[] lines = response.split("\r\n");
        for (String line : lines) {
            if (line.isEmpty()) {
                break;
            }
            int colonIndex = line.indexOf(":");
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(key, value);
            }
        }
        return headers;
    }

    public static String parseBody(String response) {
        int splitIndex = response.indexOf("\r\n\r\n");
        if (splitIndex >= 0) {
            return response.substring(splitIndex + 4).trim();
        }
        return "";
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

    /**
     * 连接指tts服务器
     */
    public void connect() {
        if (isConnected) return;
        httpClient = HttpClient.newHttpClient();
        WebSocket.Builder builder = httpClient.newWebSocketBuilder();
        builder.header("Origin", EDGE_ORIGIN);
        builder.header("Pragma", "no-cache");
        builder.header("Cache-Control", "no-cache");
        builder.header("User-Agent", EDGE_UA);
        try {
            webSocket = builder.buildAsync(URI.create("wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=6A5AA1D4EAFF4E9FB37E23D68491D6F4"), new WebSocketListener()).join();
            isConnected = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
     * @param locale       语言地区
     * @param voiceName    发声人名
     * @param content      内容
     * @return RequestId 请求ID
     */
    public String speak(TTSCodecs outputFormat, String locale, String voiceName, String content) {
        return speak(outputFormat, makeSSML(locale, voiceName, content));
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

    /**
     * 增加监听器
     *
     * @param listener 监听器
     * @return 操作状态
     */
    public boolean addListener(ITTSListener listener) {
        return listenerList.add(listener);
    }

    /**
     * 移除监听器
     *
     * @param listener 监听器
     * @return 操作状态
     */
    public boolean removeListener(ITTSListener listener) {
        return listenerList.remove(listener);
    }

    @Override
    public void close() throws IOException {
        if (isConnected) {
            httpClient.close();
        }
    }

    private final class WebSocketListener implements WebSocket.Listener {
        private final List<byte[]> bytesBufferList = new ArrayList<>();
        private final StringBuilder stringBuffer = new StringBuilder();
        private int bytesBufferLength = 0;

        @Override
        public void onOpen(WebSocket webSocket) {
            for (var listener : listenerList)
                listener.onConnect();
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            byte[] all = new byte[data.remaining()];
            data.get(all);
            bytesBufferList.add(all);
            bytesBufferLength += all.length;

            if (last) {
                data = ByteBuffer.allocate(bytesBufferLength);
                for (var buf : bytesBufferList) {
                    data.put(buf);
                }
                data.flip();
                bytesBufferLength = 0;
                bytesBufferList.clear();

                data.order(ByteOrder.BIG_ENDIAN);
                //读取响应头长度
                int len = data.getShort();
                byte[] headBuf = new byte[len];
                data.get(headBuf);
                Map<String, String> headers = parseHeaders(new String(headBuf, StandardCharsets.UTF_8));
                //读取音频数据
                byte[] dataBuf = new byte[data.remaining()];
                data.get(dataBuf);

                String requestId = headers.get("X-RequestId");
                String streamId = headers.get("X-StreamId");
                String contentType = headers.get("Content-Type");
                String codec = headers.get("codec");

                if (contentType == null) {
                    for (var listener : listenerList)
                        listener.onGetAudioDataEnd(requestId, streamId);
                } else {
                    for (var listener : listenerList)
                        listener.onGetAudioData(requestId, streamId, contentType, codec, dataBuf);
                }
            }
            return WebSocket.Listener.super.onBinary(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            stringBuffer.append(data);
            if (last) {
                String str = stringBuffer.toString();
                stringBuffer.setLength(0);

                Map<String, String> headers = parseHeaders(str);
                String body = parseBody(str);
                String requestId = headers.get("X-RequestId");

                switch (headers.get("Path")) {
                    case "turn.start" -> {
                        var info = gson.fromJson(body, TTSResponseInfo.class);
                        for (var listener : listenerList)
                            listener.onGetTurnStart(requestId, info);
                    }
                    case "response" -> {
                        var info = gson.fromJson(body, TTSResponseInfo.class);
                        for (var listener : listenerList)
                            listener.onGetResponse(requestId, info);
                    }
                    case "audio.metadata" -> {
                        var info = gson.fromJson(body, TTSMetadataInfo.class);
                        for (var listener : listenerList)
                            listener.onGetAudioMetadata(requestId, info);
                    }
                    case "turn.end" -> {
                        for (var listener : listenerList)
                            listener.onGetTurnEnd(requestId);
                    }
                }

            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            isConnected = false;
            for (var listener : listenerList)
                listener.onDisconnect();
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            error.printStackTrace();
            System.err.println("连接错误: " + error);
            WebSocket.Listener.super.onError(webSocket, error);
        }
    }
}
