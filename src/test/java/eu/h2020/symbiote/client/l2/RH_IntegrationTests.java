package eu.h2020.symbiote.client.l2;

import eu.h2020.symbiote.client.ClientFixture;
import eu.h2020.symbiote.client.SymbioteCloudITApplication;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.cloud.model.internal.FederationInfoBean;
import eu.h2020.symbiote.cloud.model.internal.ResourceSharingInformation;
import org.junit.After;
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

import java.util.*;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SymbioteCloudITApplication.class})
@TestPropertySource(locations = "classpath:application.properties")
//@DirtiesContext
public class RH_IntegrationTests extends ClientFixture {
	private static Logger log = LoggerFactory.getLogger(RH_IntegrationTests.class);
	
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

	@Test
	public void testGetAllRegisteredResources() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		ResponseEntity<ArrayList<CloudResource>> responseEntity = getResources();
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}

	@Test
	public void testDeleteAllRegisteredResources() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
        registerDefaultL2Resources();
        ResponseEntity<List<String>> responseEntity = deleteAllL2Resources();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}
	
	@Test
	public void testRegisterSensor() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		LinkedList<CloudResource> resources = new LinkedList<>();
		CloudResource defaultSensorResource = createSensorResource("", "isen1");
		resources.add(defaultSensorResource);
		
		ResponseEntity<ArrayList<CloudResource>> responseEntity = registerL2Resources(resources);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(1, responseEntity.getBody().size());

		CloudResource returnedResource = responseEntity.getBody().get(0);
		assertEquals(defaultSensorResource.getInternalId(), returnedResource.getInternalId());
		assertNotNull(returnedResource.getFederationInfo());
        assertNotNull(returnedResource.getFederationInfo().getAggregationId());
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}


	@Test
	public void testShareSensor() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		LinkedList<CloudResource> resources = new LinkedList<>();
		CloudResource defaultSensorResource = createSensorResource("", "isen1");
		resources.add(defaultSensorResource);

		ResponseEntity<ArrayList<CloudResource>> responseEntity = registerL2Resources(resources);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

		String fedId="fed2";
		Map<String, Map<String, Boolean>> sharingMap = new HashMap<>();
		sharingMap.put(fedId, new HashMap<>());
		Boolean bartering =true;
		sharingMap.get(fedId).put(defaultSensorResource.getInternalId(), bartering);
		ResponseEntity<?> responseEntity2 = shareResources(sharingMap);
		Map<String, List<CloudResource>> result=(Map<String, List<CloudResource>>)responseEntity2.getBody();
		assertTrue(result.containsKey(fedId));
		assertNotNull(result.get(fedId).get(0).getFederationInfo().getSharingInformation().containsKey(fedId));

		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}


	@Test
	public void testRegisterSensorMetadata() {//register with the resource metadata shared
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		LinkedList<CloudResource> resources = new LinkedList<>();
		CloudResource defaultSensorResource = createSensorResource("", "isen1");
		resources.add(defaultSensorResource);

		String fedId="fed2";
		Boolean bartering =true;
		Map<String, ResourceSharingInformation> resourceSharingInformationMapSensor = new HashMap<>();
		ResourceSharingInformation sharingInformationSensor = new ResourceSharingInformation();
		sharingInformationSensor.setBartering(bartering);
		resourceSharingInformationMapSensor.put(fedId, sharingInformationSensor);
		FederationInfoBean federationInfoBeanSensor = new FederationInfoBean();
		federationInfoBeanSensor.setSharingInformation(resourceSharingInformationMapSensor);
		defaultSensorResource.setFederationInfo(federationInfoBeanSensor);

		ResponseEntity<ArrayList<CloudResource>> responseEntity = registerL2Resources(resources);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(1, responseEntity.getBody().size());
		CloudResource returnedResource = responseEntity.getBody().get(0);
		assertEquals(defaultSensorResource.getInternalId(), returnedResource.getInternalId());
		assertNotNull(returnedResource.getFederationInfo().getAggregationId());
		assertTrue(returnedResource.getFederationInfo().getSharingInformation().containsKey(fedId));
		assertTrue(returnedResource.getFederationInfo().getSharingInformation().get(fedId).getBartering());

		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}


	@Test
	public void testRegisterResourcesMetadata() {//register with the resource metadata shared
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

		LinkedList<CloudResource> resources = new LinkedList<>();
		String fedId1="fed1";
		String fedId2="fed2";


		CloudResource defaultSensorResource = createSensorResource("", "isen1");

		Map<String, ResourceSharingInformation> resourceSharingInformationMapSensor = new HashMap<>();
		ResourceSharingInformation sharingInformationSensor1 = new ResourceSharingInformation();
		sharingInformationSensor1.setBartering(true);
		resourceSharingInformationMapSensor.put(fedId1, sharingInformationSensor1);
		ResourceSharingInformation sharingInformationSensor2 = new ResourceSharingInformation();
		sharingInformationSensor2.setBartering(false);
		resourceSharingInformationMapSensor.put(fedId2, sharingInformationSensor2);
		FederationInfoBean federationInfoBeanSensor = new FederationInfoBean();
		federationInfoBeanSensor.setSharingInformation(resourceSharingInformationMapSensor);

		defaultSensorResource.setFederationInfo(federationInfoBeanSensor);
		resources.add(defaultSensorResource);


		CloudResource defaultActuatorResource = createActuatorResource("", "iaid1");

		Map<String, ResourceSharingInformation> resourceSharingInformationMapActuator = new HashMap<>();
		ResourceSharingInformation sharingInformationActuator1 = new ResourceSharingInformation();
		sharingInformationActuator1.setBartering(true);
		resourceSharingInformationMapActuator.put(fedId1, sharingInformationActuator1);
		FederationInfoBean federationInfoBeanActuator = new FederationInfoBean();
		federationInfoBeanActuator.setSharingInformation(resourceSharingInformationMapActuator);

		defaultActuatorResource.setFederationInfo(federationInfoBeanActuator);
		resources.add(defaultActuatorResource);


		CloudResource defaultServiceResource = createServiceResource("", "isrid1");

		Map<String, ResourceSharingInformation> resourceSharingInformationMapService = new HashMap<>();
		ResourceSharingInformation sharingInformationService = new ResourceSharingInformation();
		sharingInformationService.setBartering(true);
		resourceSharingInformationMapService.put(fedId1, sharingInformationService);
		FederationInfoBean federationInfoBeanService = new FederationInfoBean();
		federationInfoBeanService.setSharingInformation(resourceSharingInformationMapService);

		defaultServiceResource.setFederationInfo(federationInfoBeanService);
		resources.add(defaultServiceResource);


		ResponseEntity<ArrayList<CloudResource>> responseEntity = registerL2Resources(resources);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(3, responseEntity.getBody().size());
		assertEquals(defaultSensorResource.getInternalId(), responseEntity.getBody().get(0).getInternalId());
		assertNotNull(responseEntity.getBody().get(0).getFederationInfo().getAggregationId());
		assertEquals(2,responseEntity.getBody().get(0).getFederationInfo().getSharingInformation().size());
		assertFalse(responseEntity.getBody().get(0).getFederationInfo().getSharingInformation().get(fedId2).getBartering());
		assertTrue(responseEntity.getBody().get(0).getFederationInfo().getSharingInformation().get(fedId1).getBartering());

		assertEquals(defaultActuatorResource.getInternalId(), responseEntity.getBody().get(1).getInternalId());
		assertNotNull(responseEntity.getBody().get(1).getFederationInfo().getAggregationId());
		assertEquals(1,responseEntity.getBody().get(1).getFederationInfo().getSharingInformation().size());
		assertTrue(responseEntity.getBody().get(1).getFederationInfo().getSharingInformation().get(fedId1).getBartering());

		assertEquals(defaultServiceResource.getInternalId(), responseEntity.getBody().get(2).getInternalId());
		assertNotNull(responseEntity.getBody().get(2).getFederationInfo().getAggregationId());
		assertEquals(1,responseEntity.getBody().get(2).getFederationInfo().getSharingInformation().size());
		assertTrue(responseEntity.getBody().get(2).getFederationInfo().getSharingInformation().get(fedId1).getBartering());

		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}

	@Test
	public void testRegisterActuator() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		LinkedList<CloudResource> resources = new LinkedList<>();
		CloudResource defaultActuatorResource = createActuatorResource("", "iaid1");
		resources.add(defaultActuatorResource);
		
		ResponseEntity<ArrayList<CloudResource>> responseEntity = registerL2Resources(resources);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(1, responseEntity.getBody().size());
		
		CloudResource returnedResource = responseEntity.getBody().get(0);
        assertEquals(defaultActuatorResource.getInternalId(), returnedResource.getInternalId());
        assertNotNull(returnedResource.getFederationInfo());
        assertNotNull(returnedResource.getFederationInfo().getAggregationId());
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}
	

	@Test
	public void testRegisterService() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		LinkedList<CloudResource> resources = new LinkedList<>();
		CloudResource defaultServiceResource = createActuatorResource("", "isrid1");
		resources.add(defaultServiceResource);
		
		ResponseEntity<ArrayList<CloudResource>> responseEntity = registerL2Resources(resources);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(1, responseEntity.getBody().size());
		
		CloudResource returnedResource = responseEntity.getBody().get(0);
        assertEquals(defaultServiceResource.getInternalId(), returnedResource.getInternalId());
        assertNotNull(returnedResource.getFederationInfo());
        assertNotNull(returnedResource.getFederationInfo().getAggregationId());
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}
	
	@Test
	public void testRegisterListOfResources() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		ResponseEntity<ArrayList<CloudResource>> responseEntity = registerDefaultL2Resources();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(3, responseEntity.getBody().size());
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}
}
