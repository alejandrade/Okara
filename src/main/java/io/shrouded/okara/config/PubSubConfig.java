package io.shrouded.okara.config;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.integration.outbound.PubSubMessageHandler;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import io.shrouded.okara.service.FeedFanoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PubSubConfig {

    private final FeedPubSubProperties pubSubProperties;
    private final FeedFanoutService feedFanoutService;

    // Output channel for publishing feed events
    @Bean(name = "feedEventsOutputChannel")
    public MessageChannel feedEventsOutputChannel() {
        return new DirectChannel();
    }

    // Input channel for receiving feed events
    @Bean(name = "feedEventsInputChannel")
    public MessageChannel feedEventsInputChannel() {
        return new DirectChannel();
    }

    // Outbound message handler for publishing to Pub/Sub
    @Bean
    @ServiceActivator(inputChannel = "feedEventsOutputChannel")
    public MessageHandler messageSender(PubSubTemplate pubsubTemplate) {
        log.info("Configuring Pub/Sub message sender for topic: {}", pubSubProperties.getTopic());
        PubSubMessageHandler handler = new PubSubMessageHandler(pubsubTemplate, pubSubProperties.getTopic());
        handler.setSuccessCallback((ackId, message) ->
                                           log.debug("Feed event published successfully: {}", ackId));
        handler.setFailureCallback((cause, message) ->
                                           log.error("Failed to publish feed event: {}", cause.getMessage()));
        return handler;
    }

    // Inbound channel adapter for receiving from Pub/Sub
    @Bean
    public PubSubInboundChannelAdapter messageChannelAdapter(
            @Qualifier("feedEventsInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {

        log.info("Configuring Pub/Sub message receiver for subscription: {}", pubSubProperties.getSubscription());
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(
                pubSubTemplate, pubSubProperties.getSubscription());
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(String.class);
        return adapter;
    }

    // Message receiver for processing feed events
    @Bean
    @ServiceActivator(inputChannel = "feedEventsInputChannel")
    public MessageHandler feedEventReceiver() {
        return message -> {
            String payload = (String) message.getPayload();
            BasicAcknowledgeablePubsubMessage originalMessage =
                    message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE,
                                             BasicAcknowledgeablePubsubMessage.class);

            try {
                // Process the feed event using FeedFanoutService
                log.debug("Processing feed event: {}", payload);

                feedFanoutService.processFeedEvent(payload)
                                 .doOnSuccess(v -> {
                                     log.debug("Feed event processed successfully");
                                     // Acknowledge the message after successful processing
                                     if (originalMessage != null) {
                                         originalMessage.ack();
                                     }
                                 })
                                 .doOnError(error -> {
                                     log.error("Failed to process feed event: {}", error.getMessage(), error);
                                     // Don't ack the message, it will be retried
                                     if (originalMessage != null) {
                                         originalMessage.nack();
                                     }
                                 })
                                 .subscribe();

            } catch (Exception e) {
                log.error("Error processing feed event: {}", e.getMessage(), e);
                // Don't ack the message, it will be retried
                if (originalMessage != null) {
                    originalMessage.nack();
                }
            }
        };
    }
}