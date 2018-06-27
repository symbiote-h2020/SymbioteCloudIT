package eu.h2020.symbiote.client.l1;

import eu.h2020.symbiote.client.ClientFixture;
import eu.h2020.symbiote.client.SymbioteCloudITApplication;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.LinkedList;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SymbioteCloudITApplication.class})
@TestPropertySource(locations = "classpath:application.properties")
//@DirtiesContext
public class RH_IntegrationTests extends ClientFixture {
	private static Logger log = LoggerFactory.getLogger(RH_IntegrationTests.class);
	
	@Before
	public void setUp() {
		log.info("JUnit: setup START");
		clearRegistrationHandler();
		log.info("JUnit: setup END");
	}

	@After
	public void cleanUp() {
		clearRegistrationHandler();
	}

	@Test
	public void testCreatingUser() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		client.registerToPAAM(platformId, directAAMUrl);
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}

	@Test
	public void testGetAllRegisteredResources() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		ResponseEntity<ArrayList<CloudResource>> responseEntity = getResources();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}

	@Test
	public void testDeleteAllRegisteredResources() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
        registerDefaultL1Resources();
        ResponseEntity<ArrayList<CloudResource>> responseEntity = deleteAllL1Resources();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}

	@Ignore
	@Test
	public void testSyncResources() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		
		syncResources();
		ResponseEntity<ArrayList<CloudResource>> responseEntity = syncResources();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}
	
	@Test
	public void testRegisterSensor() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		LinkedList<CloudResource> resources = new LinkedList<>();
		CloudResource defaultSensorResource = createSensorResource("", "isen1");
		resources.add(defaultSensorResource);
		
		ResponseEntity<ArrayList<CloudResource>> responseEntity = registerL1Resources(resources);
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getBody()).hasSize(1);
		
		CloudResource returnedResource = responseEntity.getBody().get(0);
		assertThat(returnedResource.getInternalId()).isEqualTo(defaultSensorResource.getInternalId());
		assertThat(returnedResource.getResource().getId()).isNotNull();
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}
	

	@Test
	public void testRegisterActuator() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		LinkedList<CloudResource> resources = new LinkedList<>();
		CloudResource defaultActuatorResource = createActuatorResource("", "iaid1");
		resources.add(defaultActuatorResource);
		
		ResponseEntity<ArrayList<CloudResource>> responseEntity = registerL1Resources(resources);
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getBody()).hasSize(1);
		
		CloudResource returnedResource = responseEntity.getBody().get(0);
		assertThat(returnedResource.getInternalId()).isEqualTo(defaultActuatorResource.getInternalId());
		assertThat(returnedResource.getResource().getId()).isNotNull();
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}
	

	@Test
	public void testRegisterService() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		LinkedList<CloudResource> resources = new LinkedList<>();
		CloudResource defaultServiceResource = createActuatorResource("", "isrid1");
		resources.add(defaultServiceResource);
		
		ResponseEntity<ArrayList<CloudResource>> responseEntity = registerL1Resources(resources);
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getBody()).hasSize(1);
		
		CloudResource returnedResource = responseEntity.getBody().get(0);
		assertThat(returnedResource.getInternalId()).isEqualTo(defaultServiceResource.getInternalId());
		assertThat(returnedResource.getResource().getId()).isNotNull();
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}
	
	@Test
	public void testRegisterListOfResources() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		ResponseEntity<ArrayList<CloudResource>> responseEntity = registerDefaultL1Resources();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getBody()).hasSize(3);
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}
}
