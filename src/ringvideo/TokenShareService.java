package ringvideo;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class TokenShareService implements ShareService {
    private final ShareRepository shareRepository;
    private final Duration linkValidity;
    private final AtomicInteger idCounter = new AtomicInteger();

    public TokenShareService(ShareRepository shareRepository, Duration linkValidity) {
        this.shareRepository = shareRepository;
        this.linkValidity = linkValidity;
    }

    @Override
    public String generateShareLink(String videoId, String createdBy) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Share share = new Share("SHARE" + idCounter.incrementAndGet(), videoId, token,
                Instant.now().plus(linkValidity), createdBy);
        shareRepository.save(share);
        return "ring.com/share/" + token;
    }

    @Override
    public boolean validateToken(String token) {
        Share share = shareRepository.findByToken(token);
        return share != null && share.getExpiry().isAfter(Instant.now());
    }
}
