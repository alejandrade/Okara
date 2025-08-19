package io.shrouded.okara.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "feed.pubsub")
public class FeedPubSubProperties {
    
    /**
     * The Pub/Sub topic name for feed events
     */
    private String topic = "feed-events";
    
    /**
     * The Pub/Sub subscription name for feed fanout processing
     */
    private String subscription = "feed-fanout-subscription";
}