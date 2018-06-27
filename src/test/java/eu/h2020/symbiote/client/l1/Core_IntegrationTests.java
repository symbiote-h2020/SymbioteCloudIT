package eu.h2020.symbiote.client.l1;

import eu.h2020.symbiote.client.ClientFixture;
import eu.h2020.symbiote.client.LambdaCondition;
import eu.h2020.symbiote.client.SymbioteCloudITApplication;
import eu.h2020.symbiote.core.ci.QueryResponse;
import eu.h2020.symbiote.core.internal.cram.ResourceUrlsResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.MalformedURLException;
import java.net.URL;
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
		clearRegistrationHandler();
		registerDefaultL1Resources();
		log.info("JUnit: setup END {}", new RuntimeException().getStackTrace()[0]);
        TimeUnit.SECONDS.sleep(3);
    }

	@After
	public void cleanUp() {
		clearRegistrationHandler();
	}

	@Test
	public void testSearch() {
		// GET http://localhost:8777/query?homePlatformId=xplatform&platform_id=xplatform
        ResponseEntity<QueryResponse> query = client.query(platformId, // platformId,
	    		null, // platformName, 
	    		null, // owner, 
	    		null, // name, 
	    		null, // id, 
	    		null, // description, 
	    		null, // location_name, 
	    		null, // location_lat, 
	    		null, // location_long, 
	    		null, // max_distance, 
	    		null, // observed_property, 
	    		null, // observed_property_iri, 
	    		null, // resource_type, 
	    		null, // should_rank, 
	    		platformId  // homePlatformId - can not be null
	    );
	    
	    assertThat(query.getStatusCodeValue()).isEqualTo(200);
	    assertThat(query.getBody().getBody())
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
		ResponseEntity<ResourceUrlsResponse> response = client.getResourceUrlFromCram(resourceId, platformId);

		assertUrlExists(response, resourceId);
		assertUrlPath(response, resourceId, "/rap/Sensors('" + resourceId + "')");
	}

	@Test
	public void testGetUrlForActuator() throws Exception {
        String resourceId = findDefaultActuator().getId();
		ResponseEntity<ResourceUrlsResponse> response = client.getResourceUrlFromCram(resourceId, platformId);
		
		assertUrlExists(response, resourceId);
		assertUrlPath(response, resourceId, "/rap/Actuators('" + resourceId + "')");
	}
	
	@Test
	public void testGetUrlForService() throws Exception {
		String resourceId = findDefaultService().getId();
		ResponseEntity<ResourceUrlsResponse> response = client.getResourceUrlFromCram(resourceId, platformId);
		
		assertUrlExists(response, resourceId);
		assertUrlPath(response, resourceId, "/rap/Services('" + resourceId + "')");
	}
	
	private void assertUrlPath(ResponseEntity<ResourceUrlsResponse> response, String resourceId, String expectedPath)
			throws MalformedURLException {
		Map<String, String> map = response.getBody().getBody();
		URL url = new URL(map.get(resourceId));
		assertThat(url.getPath()).isEqualTo(expectedPath);
	}

	private void assertUrlExists(ResponseEntity<ResourceUrlsResponse> response, String resourceId) {
		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		Map<String, String> map = response.getBody().getBody();
		assertThat(map)
			.hasSize(1)
			.containsKey(resourceId);
	}
}
