package in.codefarm.notification_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data payload for NotificationFailedEvent published to notifications topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationFailedEventData {
    private String notificationId;
    private String orderId;
    private String failureReason;
}

