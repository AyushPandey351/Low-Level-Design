package ringiot;

import java.util.List;

// Composes several EventValidator instances behind the SAME EventValidator
// interface - DeviceIngestionService only ever talks to "an EventValidator," not to
// a list of them, so adding a THIRD validation step later (e.g. a payload-shape
// check specific to MOTION events) is just another entry in this list, zero changes
// to DeviceIngestionService.
public class CompositeEventValidator implements EventValidator {
    private final List<EventValidator> validators;

    public CompositeEventValidator(List<EventValidator> validators) {
        this.validators = validators;
    }

    @Override
    public void validate(Event event) {
        for (EventValidator validator : validators) {
            validator.validate(event);
        }
    }
}
