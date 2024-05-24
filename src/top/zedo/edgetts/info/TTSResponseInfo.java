package top.zedo.edgetts.info;

public class TTSResponseInfo {
    public Context context;
    public Audio audio;

    public static class Context {
        public String serviceTag;

        @Override
        public String toString() {
            return "Context{" +
                    "serviceTag='" + serviceTag + '\'' +
                    '}';
        }
    }

    public static class Audio {
        public String type;
        public String streamId;

        @Override
        public String toString() {
            return "Audio{" +
                    "type='" + type + '\'' +
                    ", streamId='" + streamId + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "TTSResponseInfo{" +
                "context=" + context +
                ", audio=" + audio +
                '}';
    }
}
