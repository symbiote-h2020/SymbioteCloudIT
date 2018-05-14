package eu.h2020.symbiote.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import eu.h2020.symbiote.cloud.model.internal.CloudResource;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SymbioteCloudITApplication.class})
@TestPropertySource(locations = "classpath:application.properties")
//@DirtiesContext
public class RH_IntegrationTests extends ClientFixture {
	private static Logger log = LoggerFactory.getLogger(RH_IntegrationTests.class);
	
	@Before
	public void setUp() throws Exception {
		log.info("JUnit: setup START");
		clearRegistrationHandler();
		log.info("JUnit: setup END");
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
		ResponseEntity<ArrayList<CloudResource>> responseEntity = deleteAllResources();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}

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
		
		ResponseEntity<ArrayList<CloudResource>> responseEntity = registerResources(resources);
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
		
		ResponseEntity<ArrayList<CloudResource>> responseEntity = registerResources(resources);
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
		
		ResponseEntity<ArrayList<CloudResource>> responseEntity = registerResources(resources);
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
		ResponseEntity<ArrayList<CloudResource>> responseEntity = registerDefaultResources();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getBody()).hasSize(3);
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}
}
