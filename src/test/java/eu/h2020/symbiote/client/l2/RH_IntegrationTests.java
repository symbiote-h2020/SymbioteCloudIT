package eu.h2020.symbiote.client.l2;

import eu.h2020.symbiote.client.ClientFixture;
import eu.h2020.symbiote.client.SymbioteCloudITApplication;
import eu.h2020.symbiote.cloud.model.internal.*;
import eu.h2020.symbiote.model.cim.Actuator;
import eu.h2020.symbiote.model.cim.Sensor;
import eu.h2020.symbiote.model.cim.Service;
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
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
		ResponseEntity<List<CloudResource>> responseEntity = getResources();
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
		
		ResponseEntity<List<CloudResource>> responseEntity = registerL2Resources(resources);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(1, responseEntity.getBody().size());

		CloudResource returnedResource = responseEntity.getBody().get(0);
		assertEquals(defaultSensorResource.getInternalId(), returnedResource.getInternalId());
		assertNotNull(returnedResource.getFederationInfo());
        assertNotNull(returnedResource.getFederationInfo().getAggregationId());
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}

	@Test
	public void testUpdateSensor() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		LinkedList<CloudResource> resources = new LinkedList<>();
		CloudResource defaultSensorResource = createSensorResource("", "isen1");
		resources.add(defaultSensorResource);

		ResponseEntity<List<CloudResource>> responseEntity = registerL2Resources(resources);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(1, responseEntity.getBody().size());

		CloudResource returnedResource = responseEntity.getBody().get(0);
		assertEquals(defaultSensorResource.getInternalId(), returnedResource.getInternalId());
		assertNotNull(returnedResource.getFederationInfo());
		assertNotNull(returnedResource.getFederationInfo().getAggregationId());
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);

		List <String> descriptionSensor=returnedResource.getResource().getDescription();
		String newDescription=" and a newDescription";
		descriptionSensor.add(newDescription);
		returnedResource.getResource().setDescription(descriptionSensor);//Collections.singletonList("This is default sensor with timestamp: " + timeStamp + " and iid: " + internalId));
		List<CloudResource> resourcesToUpdate = new LinkedList<>();
		resourcesToUpdate.add(returnedResource);
		List<CloudResource> resourceUpdated = rhClient.updateL2Resources(resourcesToUpdate);
		assertThat(resourceUpdated.get(0).getInternalId()).isEqualTo(defaultSensorResource.getInternalId());
		assertThat(resourceUpdated.get(0).getResource().getDescription().contains(newDescription));

		String name=defaultSensorResource.getResource().getName();

		ResponseEntity<FederationSearchResult> query = searchL2Resources(
				new PlatformRegistryQuery.Builder().descriptions(descriptionSensor).build()
		);
		assertThat(query.getBody().getResources().contains(resourceUpdated));


	}

	@Test
	public void testRegisterActuator() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		LinkedList<CloudResource> resources = new LinkedList<>();
		CloudResource defaultActuatorResource = createActuatorResource("", "iaid1");
		resources.add(defaultActuatorResource);

		ResponseEntity<List<CloudResource>> responseEntity = registerL2Resources(resources);
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

		ResponseEntity<List<CloudResource>> responseEntity = registerL2Resources(resources);
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
		ResponseEntity<List<CloudResource>> responseEntity = registerDefaultL2Resources();
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(3, responseEntity.getBody().size());
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}


	@Test
	public void testShareSensor() {//register and then share resource
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		LinkedList<CloudResource> resources = new LinkedList<>();
		CloudResource defaultSensorResource = createSensorResource("", "isen1");
		resources.add(defaultSensorResource);

		ResponseEntity<List<CloudResource>> responseEntity = registerL2Resources(resources);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

		String fedId="fed2";
		Map<String, Map<String, Boolean>> sharingMap = new HashMap<>();
		sharingMap.put(fedId, new HashMap<>());
		Boolean bartering =true;
		sharingMap.get(fedId).put(defaultSensorResource.getInternalId(), bartering);
		ResponseEntity<?> responseEntity2 = shareResources(sharingMap);
		Map<String, List<CloudResource>> result=(Map<String, List<CloudResource>>)responseEntity2.getBody();
		assertTrue(result.containsKey(fedId));
		assertNotNull(result.get(fedId).get(0).getFederationInfo().getSharingInformation().get(fedId));

		String aggregationId = result.get(fedId).get(0).getFederationInfo().getAggregationId();
		assertEquals(aggregationId + "@" + fedId,
                result.get(fedId).get(0).getFederationInfo().getSharingInformation().get(fedId).getSymbioteId());

		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}

	@Test
	public void testShareResources() {//register and then share resource
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
		LinkedList<CloudResource> resources = new LinkedList<>();
		CloudResource defaultSensorResource = createSensorResource("", "isen1");
		resources.add(defaultSensorResource);
		CloudResource defaultActuatorResource = createSensorResource("", "iaid1");
		resources.add(defaultActuatorResource);
		CloudResource defaultServiceResource = createSensorResource("", "isrid1");
		resources.add(defaultServiceResource);
		ResponseEntity<List<CloudResource>> responseEntity = registerL2Resources(resources);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

		String fedId1="fed1";
		String fedId2="fed2";
		Map<String, Map<String, Boolean>> sharingMap = new HashMap<>();
		sharingMap.put(fedId1, new HashMap<>());
		sharingMap.put(fedId2, new HashMap<>());
		sharingMap.get(fedId2).put(defaultSensorResource.getInternalId(), false);
		sharingMap.get(fedId1).put(defaultSensorResource.getInternalId(), true);
		sharingMap.get(fedId1).put(defaultActuatorResource.getInternalId(), true);
		sharingMap.get(fedId1).put(defaultServiceResource.getInternalId(), true);

		ResponseEntity<?> responseEntity2 = shareResources(sharingMap);
		Map<String, List<CloudResource>> result=(Map<String, List<CloudResource>>)responseEntity2.getBody();

		assertEquals(3, result.get(fedId1).size());
		assertEquals(1, result.get(fedId2).size());

        assertEquals(defaultSensorResource.getInternalId(), result.get(fedId2).get(0).getInternalId());
		assertNotNull(result.get(fedId2).get(0).getFederationInfo().getAggregationId());
		assertEquals(2,result.get(fedId2).get(0).getFederationInfo().getSharingInformation().size());
		assertFalse(result.get(fedId2).get(0).getFederationInfo().getSharingInformation().get(fedId2).getBartering());
		assertTrue(result.get(fedId1).get(0).getFederationInfo().getSharingInformation().get(fedId1).getBartering());

		CloudResource actuator = result.get(fedId1).get(1).getResource() instanceof Actuator ?
				result.get(fedId1).get(1) : result.get(fedId1).get(2);
		CloudResource service = result.get(fedId1).get(2).getResource() instanceof Service ?
				result.get(fedId1).get(2) : result.get(fedId1).get(1);

		assertEquals(defaultActuatorResource.getInternalId(), actuator.getInternalId());
		assertNotNull(actuator.getFederationInfo().getAggregationId());
		assertEquals(1,actuator.getFederationInfo().getSharingInformation().size());
		assertTrue(actuator.getFederationInfo().getSharingInformation().get(fedId1).getBartering());
		assertEquals(defaultServiceResource.getInternalId(), service.getInternalId());
		assertNotNull(service.getFederationInfo().getAggregationId());
		assertEquals(1,service.getFederationInfo().getSharingInformation().size());
		assertTrue(service.getFederationInfo().getSharingInformation().get(fedId1).getBartering());

		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}

	@Test
	public void testRegisterAllResourcesMetadata() {//register resources with resource metadata sharing information filled
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

		LinkedList<CloudResource> resources = new LinkedList<>();
		String fedId1="fed1";
		String fedId2="fed2";

		CloudResource defaultSensorResource = createSensorResource("", "isen1");

		Map<String, ResourceSharingInformation> resourceSharingInformationMapSensor = new HashMap<>();
		ResourceSharingInformation sharingInformationSensor1 = new ResourceSharingInformation();
		sharingInformationSensor1.setBartering(false);
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
		sharingInformationActuator1.setBartering(false);
		resourceSharingInformationMapActuator.put(fedId1, sharingInformationActuator1);
		FederationInfoBean federationInfoBeanActuator = new FederationInfoBean();
		federationInfoBeanActuator.setSharingInformation(resourceSharingInformationMapActuator);

		defaultActuatorResource.setFederationInfo(federationInfoBeanActuator);
		resources.add(defaultActuatorResource);

		CloudResource defaultServiceResource = createServiceResource("", "isrid1");

		Map<String, ResourceSharingInformation> resourceSharingInformationMapService = new HashMap<>();
		ResourceSharingInformation sharingInformationService = new ResourceSharingInformation();
		sharingInformationService.setBartering(false);
		resourceSharingInformationMapService.put(fedId1, sharingInformationService);
		FederationInfoBean federationInfoBeanService = new FederationInfoBean();
		federationInfoBeanService.setSharingInformation(resourceSharingInformationMapService);

		defaultServiceResource.setFederationInfo(federationInfoBeanService);
		resources.add(defaultServiceResource);

		ResponseEntity<List<CloudResource>> responseEntity = registerL2Resources(resources);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(3, responseEntity.getBody().size());

		assertEquals(defaultSensorResource.getInternalId(), responseEntity.getBody().get(0).getInternalId());
		assertNotNull(responseEntity.getBody().get(0).getFederationInfo().getAggregationId());
		assertEquals(2,responseEntity.getBody().get(0).getFederationInfo().getSharingInformation().size());
		assertFalse(responseEntity.getBody().get(0).getFederationInfo().getSharingInformation().get(fedId2).getBartering());
		assertFalse(responseEntity.getBody().get(0).getFederationInfo().getSharingInformation().get(fedId1).getBartering());

        String aggregationId = responseEntity.getBody().get(0).getFederationInfo().getAggregationId();
        assertEquals(aggregationId + "@" + fedId1,
                responseEntity.getBody().get(0).getFederationInfo().getSharingInformation().get(fedId1).getSymbioteId());

        assertEquals(defaultActuatorResource.getInternalId(), responseEntity.getBody().get(1).getInternalId());
		assertNotNull(responseEntity.getBody().get(1).getFederationInfo().getAggregationId());
		assertEquals(1,responseEntity.getBody().get(1).getFederationInfo().getSharingInformation().size());
		assertFalse(responseEntity.getBody().get(1).getFederationInfo().getSharingInformation().get(fedId1).getBartering());
        aggregationId = responseEntity.getBody().get(1).getFederationInfo().getAggregationId();
        assertEquals(aggregationId + "@" + fedId1,
                responseEntity.getBody().get(1).getFederationInfo().getSharingInformation().get(fedId1).getSymbioteId());

		assertEquals(defaultServiceResource.getInternalId(), responseEntity.getBody().get(2).getInternalId());
		assertNotNull(responseEntity.getBody().get(2).getFederationInfo().getAggregationId());
		assertEquals(1,responseEntity.getBody().get(2).getFederationInfo().getSharingInformation().size());
		assertFalse(responseEntity.getBody().get(2).getFederationInfo().getSharingInformation().get(fedId1).getBartering());

        aggregationId = responseEntity.getBody().get(2).getFederationInfo().getAggregationId();
        assertEquals(aggregationId + "@" + fedId1,
                responseEntity.getBody().get(2).getFederationInfo().getSharingInformation().get(fedId1).getSymbioteId());

		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}

    @Test
    public void testUnshareResources() {//register and then share resource
        log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);
        LinkedList<CloudResource> resources = new LinkedList<>();
        String fedId1="fed1";
        String fedId2="fed2";

        //register resource metadata with the sharing information filled
        CloudResource defaultSensorResource = createSensorResource("", "isen1");

        Map<String, ResourceSharingInformation> resourceSharingInformationMapSensor = new HashMap<>();
        ResourceSharingInformation sharingInformationSensor1 = new ResourceSharingInformation();
        sharingInformationSensor1.setBartering(false);
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
        sharingInformationActuator1.setBartering(false);
        resourceSharingInformationMapActuator.put(fedId1, sharingInformationActuator1);
        FederationInfoBean federationInfoBeanActuator = new FederationInfoBean();
        federationInfoBeanActuator.setSharingInformation(resourceSharingInformationMapActuator);

        defaultActuatorResource.setFederationInfo(federationInfoBeanActuator);
        resources.add(defaultActuatorResource);

        CloudResource defaultServiceResource = createServiceResource("", "isrid1");

        Map<String, ResourceSharingInformation> resourceSharingInformationMapService = new HashMap<>();
        ResourceSharingInformation sharingInformationService = new ResourceSharingInformation();
        sharingInformationService.setBartering(false);
        resourceSharingInformationMapService.put(fedId1, sharingInformationService);
        FederationInfoBean federationInfoBeanService = new FederationInfoBean();
        federationInfoBeanService.setSharingInformation(resourceSharingInformationMapService);

        defaultServiceResource.setFederationInfo(federationInfoBeanService);
        resources.add(defaultServiceResource);

        ResponseEntity<List<CloudResource>> responseEntity = registerL2Resources(resources);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        //unshare resources sensor and actuator from the federations
        Map<String, List<String>> unshareMap = new HashMap<>();
        unshareMap.put(fedId1, new LinkedList<>());
        unshareMap.put(fedId2, new LinkedList<>());

        unshareMap.get(fedId2).add(defaultSensorResource.getInternalId());
        unshareMap.get(fedId1).add(defaultSensorResource.getInternalId());
        unshareMap.get(fedId1).add(defaultActuatorResource.getInternalId());

        ResponseEntity<?> responseEntityUnshare = unshareResources(unshareMap);
        Map<String, List<CloudResource>> result=(Map<String, List<CloudResource>>)responseEntityUnshare.getBody();

        assertEquals(2, result.get(fedId1).size());
        assertEquals(1, result.get(fedId2).size());
        CloudResource actuator = result.get(fedId1).get(0).getResource() instanceof Actuator ?
                result.get(fedId1).get(0) : result.get(fedId1).get(1);
        CloudResource sensor = result.get(fedId1).get(0).getResource() instanceof Sensor ?
                result.get(fedId1).get(0) : result.get(fedId1).get(1);
        assertEquals(defaultActuatorResource.getInternalId(),actuator.getInternalId());
        assertEquals(defaultSensorResource.getInternalId(),sensor.getInternalId());
        assertEquals(defaultSensorResource.getInternalId(),result.get(fedId2).get(0).getInternalId());

        log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
    }

}
