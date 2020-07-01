package mosso;

public interface IncrementalInterface {
    void processAddition(int src, int dst);
    void processDeletion(int src, int dst);
    //void processBatch();
}
