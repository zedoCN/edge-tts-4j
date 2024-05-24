package top.zedo.edgetts;

public enum TTSCodecs {
    MP3_48K("audio-24khz-48kbitrate-mono-mp3"),
    MP3_96K("audio-24khz-96kbitrate-mono-mp3"),
    OPUS("webm-24khz-16bit-mono-opus"),
    ;
    private final String id;

    TTSCodecs(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
