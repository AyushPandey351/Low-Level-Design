package logsystem;

public interface LogSourceRepository {
    void save(LogSource source);

    LogSource find(String serviceId);
}
