package in.codefarm.notification_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data payload for NotificationSentEvent published to notifications topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSentEventData {
    private String notificationId;
    private String orderId;
    private String customerId;
    private String notificationType;
    private String channel;  // EMAIL or SMS
    private String recipient;
}

