package meetingscheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// ConcurrentHashMap, same reasoning as PaymentRepository in the Payment Gateway
// design: this system explicitly must "handle concurrent scheduling requests," so a
// plain HashMap would risk lost updates/corruption under concurrent save()/find()
// calls - not a theoretical concern given the stated non-functional requirement.
public class CalendarRepository {
    private final Map<String, Calendar> calendarsByUserId = new ConcurrentHashMap<>();

    public void save(String userId, Calendar calendar) {
        calendarsByUserId.put(userId, calendar);
    }

    public Calendar find(String userId) {
        return calendarsByUserId.get(userId);
    }

    public void update(String userId, Calendar calendar) {
        calendarsByUserId.put(userId, calendar);
    }
}
