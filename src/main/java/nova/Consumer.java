package nova;
public interface Consumer {
    public void receiveFromProducer();
    public String[] normalizeData(String data);
    public void dataPersistance(String data);
}

