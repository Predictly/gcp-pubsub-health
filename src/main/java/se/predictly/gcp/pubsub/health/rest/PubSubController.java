package se.predictly.gcp.pubsub.health.rest;

import java.util.stream.IntStream;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import se.predictly.gcp.pubsub.health.config.GcpPubSubAutoConfiguration;

@RestController
public class PubSubController {

	private static final Integer NR_OF_MESSAGES = 10000;
	
	@Autowired
	private PubSubTemplate pubSubTemplate;
	
	@GetMapping(path = "/production")
	public String fillProductionTopic() {
		IntStream.range(0, 10000).forEach((i) -> pubSubTemplate.publish("production", String.format("%d", i)));
		return String.format("Sent %d msgs to PubSub topic %s", NR_OF_MESSAGES, GcpPubSubAutoConfiguration.PRODUCTION_TOPIC);
	}
}
