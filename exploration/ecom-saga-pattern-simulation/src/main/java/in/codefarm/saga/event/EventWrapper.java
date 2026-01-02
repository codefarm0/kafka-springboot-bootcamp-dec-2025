package in.codefarm.saga.event;

public record EventWrapper<T>(
    EventMetadata metadata,
    T payload
) {
}

