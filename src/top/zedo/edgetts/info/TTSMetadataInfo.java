package top.zedo.edgetts.info;

import java.util.List;

public class TTSMetadataInfo {
    public List<Metadata> Metadata;

    @Override
    public String toString() {
        return "TTSMetadataInfo{" +
                "Metadata=" + Metadata +
                '}';
    }

    public static class Metadata {
        public String Type;
        public Data Data;

        @Override
        public String toString() {
            return "Metadata{" +
                    "Type='" + Type + '\'' +
                    ", Data=" + Data +
                    '}';
        }
    }

    public static class Data {
        public long Offset;
        public long Duration;
        public Text text;

        @Override
        public String toString() {
            return "Data{" +
                    "Offset=" + Offset +
                    ", Duration=" + Duration +
                    ", text=" + text +
                    '}';
        }
    }

    public static class Text {
        public String Text;
        public int Length;
        public String BoundaryType;

        @Override
        public String toString() {
            return "Text{" +
                    "Text='" + Text + '\'' +
                    ", Length=" + Length +
                    ", BoundaryType='" + BoundaryType + '\'' +
                    '}';
        }
    }
}
