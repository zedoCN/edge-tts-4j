package top.zedo.edgetts;

public class Voice {
    public String Name;
    public String ShortName;
    public String Gender;
    public String Locale;
    public String SuggestedCodec;
    public String FriendlyName;
    public String Status;
    public VoiceTag VoiceTag;

    @Override
    public String toString() {
        return "Voice{" +
                "Name='" + Name + '\'' +
                ", Gender='" + Gender + '\'' +
                ", Locale='" + Locale + '\'' +
                '}';
    }
}
