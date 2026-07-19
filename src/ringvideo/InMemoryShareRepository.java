package ringvideo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryShareRepository implements ShareRepository {
    private final Map<String, Share> sharesByToken = new ConcurrentHashMap<>();

    @Override
    public void save(Share share) {
        sharesByToken.put(share.getToken(), share);
    }

    @Override
    public Share findByToken(String token) {
        return sharesByToken.get(token);
    }
}
