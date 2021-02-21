package se.predictly.gcp.pubsub.health.config;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!unittest")
public class GcpPubSubAutoConfiguration {

	public static final String PRODUCTION_SUBSCRIPTION = "production-subscription";
	public static final String PRODUCTION_TOPIC = "production";

	private static final Logger LOGGER = LoggerFactory.getLogger(GcpPubSubAutoConfiguration.class);
	
	@Autowired
	private PubSubTemplate pubSubTemplate;

	@Autowired
	private PubSubAdmin pubSubAdmin;
	
	private AtomicInteger counter = new AtomicInteger();
	
	@PostConstruct
	public void createResourcesAndRegisterConsumer() {
		LOGGER.info("Creating topic and subscription");
		setupPubSubResources();
		
		LOGGER.info("Registering PubSub consumer");
		pubSubTemplate.subscribe(PRODUCTION_SUBSCRIPTION, this::consumeWithDelay);
	}

	private void consumeWithDelay(BasicAcknowledgeablePubsubMessage msg) {
		try {
			List<String> randomStrings = IntStream.range(15, 100)
					.mapToObj(i -> RandomStringUtils.random(i, "abcdefghijklmnopqrstuvxyzABCDEFGHIJKLMNOPQRSTUVXYZ"))
					.collect(Collectors.toList());

			IntStream.range(0, 1000).forEach(i -> {
				Collections.sort(randomStrings);
				Collections.shuffle(randomStrings);
			});
		} finally {
			ackMsg(msg);
		}
	}

	private void setupPubSubResources() {
		Topic production = pubSubAdmin.getTopic(PRODUCTION_TOPIC);
		if (production == null) {
			production = pubSubAdmin.createTopic(PRODUCTION_TOPIC);
		}
		Subscription productionSubscription = pubSubAdmin.getSubscription(PRODUCTION_SUBSCRIPTION);
		if (productionSubscription == null) {
			productionSubscription = pubSubAdmin.createSubscription(PRODUCTION_SUBSCRIPTION, PRODUCTION_TOPIC);
		}
	}

	private void ackMsg(BasicAcknowledgeablePubsubMessage msg) {
		try {
			msg.ack().get(1000L, TimeUnit.MILLISECONDS);
			int count = counter.incrementAndGet();
			if (count % 1000 == 0) {
				LOGGER.info("Processed {} messages", count);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.info("Thread was interrupted while sleeping/waiting", e);
		} catch (ExecutionException e) {
			Throwable t = e.getCause();
			LOGGER.info("Unexpected error while waiting for ack response", t);
		} catch (TimeoutException e) {
			LOGGER.info("Timed out waiting for response from PubSub", e);
		}
	}
}
