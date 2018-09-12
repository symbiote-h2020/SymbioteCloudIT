package eu.h2020.symbiote.client.l1;

import eu.h2020.symbiote.client.ClientFixture;
import eu.h2020.symbiote.client.LambdaCondition;
import eu.h2020.symbiote.client.SymbioteCloudITApplication;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.core.ci.QueryResponse;
import eu.h2020.symbiote.core.internal.CoreQueryRequest;
import eu.h2020.symbiote.core.internal.cram.ResourceUrlsResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SymbioteCloudITApplication.class})
@TestPropertySource(locations = "classpath:application.properties")
//@DirtiesContext
public class Core_IntegrationTests extends ClientFixture {
	private static Logger log = LoggerFactory.getLogger(Core_IntegrationTests.class);
	
	@Before
	public void setUp() throws Exception {
		log.info("JUnit: setup START {}", new RuntimeException().getStackTrace()[0]);
		clearRegistrationHandlerL1();
		registerDefaultL1Resources();
		log.info("JUnit: setup END {}", new RuntimeException().getStackTrace()[0]);
        TimeUnit.SECONDS.sleep(5);
    }

	@After
	public void cleanUp() {
		clearRegistrationHandlerL1();
	}

	@Test
	public void testSearch() {
		// GET http://localhost:8777/query?homePlatformId=xplatform&platform_id=xplatform
		QueryResponse query = searchClient.search(new CoreQueryRequest.Builder().platformId(platformId).build(), true, homePlatformIds);

		assertThat(query.getBody())
				.filteredOn(new LambdaCondition<>(
						r -> r.getName().contains(defaultResourceIdPrefix)
				))
				.extracting("name")
				.containsOnly(getDefaultSensorName(),
						getDefaultActuatorName(),
						getDefaultServiceName());
	}
	
	@Test
	public void testGetUrlForSensor() throws Exception {
        // POST http://localhost:8777/get_resource_url?platformId=xplatform&resourceId=5ab412f14a234e0f916be9bf

		String resourceId = findDefaultSensor().getId();
		ResourceUrlsResponse response = cramClient.getResourceUrl(resourceId, true, homePlatformIds);

		assertUrlExists(response, resourceId);
		assertUrlPath(response, resourceId, "/rap/Sensors('" + resourceId + "')");
	}

	@Test
	public void testGetUrlForSensorWithNullFilteringPolicies() throws Exception {
		// POST http://localhost:8777/get_resource_url?platformId=xplatform&resourceId=5ab412f14a234e0f916be9bf
        clearRegistrationHandlerL1();

        LinkedList<CloudResource> resources = new LinkedList<>();
        defaultResourceIdPrefix = String.valueOf(System.currentTimeMillis());
        CloudResource cloudResource = createSensorResource(defaultResourceIdPrefix, "-isen1");
        cloudResource.setFilteringPolicy(null);
        resources.add(cloudResource);
        registerResources(resources, Layer.L1);

		String resourceId = findDefaultSensor().getId();
		ResourceUrlsResponse response = cramClient.getResourceUrl(resourceId, true, homePlatformIds);

		assertUrlExists(response, resourceId);
		assertUrlPath(response, resourceId, "/rap/Sensors('" + resourceId + "')");
	}

	@Test
	public void testGetUrlForActuator() throws Exception {
        String resourceId = findDefaultActuator().getId();
		ResourceUrlsResponse response = cramClient.getResourceUrl(resourceId, true, homePlatformIds);
		
		assertUrlExists(response, resourceId);
		assertUrlPath(response, resourceId, "/rap/Actuators('" + resourceId + "')");
	}
	
	@Test
	public void testGetUrlForService() throws Exception {
		String resourceId = findDefaultService().getId();
		ResourceUrlsResponse response = cramClient.getResourceUrl(resourceId, true, homePlatformIds);
		
		assertUrlExists(response, resourceId);
		assertUrlPath(response, resourceId, "/rap/Services('" + resourceId + "')");
	}

	private void assertUrlPath(ResourceUrlsResponse response, String resourceId, String expectedPath)
			throws MalformedURLException {
		Map<String, String> map = response.getBody();
		URL url = new URL(map.get(resourceId));
		assertThat(url.getPath()).isEqualTo(expectedPath);
	}

	private void assertUrlExists(ResourceUrlsResponse response, String resourceId) {
		Map<String, String> map = response.getBody();
		assertThat(map)
			.hasSize(1)
			.containsKey(resourceId);
	}
}
