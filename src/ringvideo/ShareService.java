package ringvideo;

public interface ShareService {
    String generateShareLink(String videoId, String createdBy);

    boolean validateToken(String token);
}
