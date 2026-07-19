package ringvideo;

public interface ShareRepository {
    void save(Share share);

    Share findByToken(String token);
}
