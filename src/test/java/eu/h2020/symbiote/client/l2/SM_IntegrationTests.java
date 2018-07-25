package eu.h2020.symbiote.client.l2;

import eu.h2020.symbiote.client.ClientFixture;
import eu.h2020.symbiote.client.SymbioteCloudITApplication;
import eu.h2020.symbiote.cloud.model.internal.Subscription;
import feign.FeignException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SymbioteCloudITApplication.class})
@TestPropertySource(locations = "classpath:application.properties")
//@DirtiesContext
public class SM_IntegrationTests extends ClientFixture {
	private static Logger log = LoggerFactory.getLogger(SM_IntegrationTests.class);
	
	@Before
	public void setUp() {
		log.info("JUnit: setup START");
        clearRegistrationHandlerL2();
		log.info("JUnit: setup END");
	}

	@After
	public void cleanUp() {
        clearRegistrationHandlerL2();
	}

	@Test(expected = FeignException.class)
    public void subscribeFail() {
        smClient.subscribe(new Subscription());
    }

    @Test
    public void subscribeSuccess() {
	    Subscription subscription = new Subscription();
	    subscription.setPlatformId(platformId);
        subscription.getResourceType().put("service", false);
        subscription.getResourceType().put("actuator", false);
        smClient.subscribe(subscription);
    }

    @Test
    public void getAllSubscriptions() {
        Subscription subscription = new Subscription();
        subscription.setPlatformId(platformId);
        subscription.getResourceType().put("service", false);
        subscription.getResourceType().put("actuator", false);
        smClient.subscribe(subscription);
	    List<Subscription> result = smClient.getAllSubscriptions().stream()
                .filter(subscription1 -> subscription1.getPlatformId().equals(platformId))
                .collect(Collectors.toList());

        assertEquals(platformId, result.get(0).getPlatformId());
        assertTrue(result.get(0).getResourceType().get("sensor"));
        assertFalse(result.get(0).getResourceType().get("service"));
    }

    @Test
    public void getPlatformSubscriptionsSuccess() {
        Subscription subscription = new Subscription();
        subscription.setPlatformId(platformId);
        subscription.getResourceType().put("service", false);
        subscription.getResourceType().put("actuator", false);
        smClient.subscribe(subscription);
        Subscription result = smClient.getPlatformSubscription(platformId);

        assertEquals(platformId, result.getPlatformId());
        assertTrue(result.getResourceType().get("sensor"));
        assertFalse(result.getResourceType().get("service"));
    }

    @Test(expected = FeignException.class)
    public void getPlatformSubscriptionsFail() {
        smClient.getPlatformSubscription("dummy");
    }
}
