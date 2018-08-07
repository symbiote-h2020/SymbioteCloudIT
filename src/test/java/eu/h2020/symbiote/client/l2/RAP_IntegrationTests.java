package eu.h2020.symbiote.client.l2;

import eu.h2020.symbiote.client.ClientFixture;
import eu.h2020.symbiote.client.SymbioteCloudITApplication;
import eu.h2020.symbiote.cloud.model.internal.*;
import eu.h2020.symbiote.model.cim.Observation;
import feign.FeignException;
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

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SymbioteCloudITApplication.class})
@TestPropertySource(locations = "classpath:application.properties")
//@DirtiesContext
public class RAP_IntegrationTests extends ClientFixture {
	private static Logger log = LoggerFactory.getLogger(RAP_IntegrationTests.class);
	
	@Before
	public void setUp() throws Exception {
		log.info("JUnit: setup START {}", new RuntimeException().getStackTrace()[0]);
		clearRegistrationHandlerL2();
	//	registerDefaultL2Resources();
		log.info("JUnit: setup END {}", new RuntimeException().getStackTrace()[0]);
        TimeUnit.SECONDS.sleep(3);
    }

	@After
	public void cleanUp() {
		clearRegistrationHandlerL2();
	}//TODO: L2

	@Test
	public void testGetSensorObservation() {

		//register all resource metadata
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

		registerL2Resources(resources);

		//get url
		String name=defaultSensorResource.getResource().getName();
		ResponseEntity<FederationSearchResult> query = searchL2Resources(
				new PlatformRegistryQuery.Builder().names(new ArrayList<>(Collections.singleton(name))).build()
		);

        String resourceId = query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId1).getSymbioteId();
        String url=query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId1).getoDataUrl();

        Observation response = rapClient.getLatestObservation(url, true, homePlatformIds);

		assertThat(response.getResourceId()).isEqualTo(resourceId);
		
	}

	@Test
	public void testGetSensorObservations2() {

		//register all resource metadata
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

		registerL2Resources(resources);

		//get url
		String name=defaultSensorResource.getResource().getName();//getDefaultSensorName();
		ResponseEntity<FederationSearchResult> query = searchL2Resources(
		        new PlatformRegistryQuery.Builder().names(new ArrayList<>(Collections.singleton(name))).build()
        );

        String resourceId = query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId1).getSymbioteId();
        String url = query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId1).getoDataUrl();
        List<Observation> response = rapClient.getTopObservations(url, 2, true, homePlatformIds);

        assertThat(response.size()).isLessThanOrEqualTo(2);
        assertThat(response.get(0).getResourceId()).isEqualTo(resourceId);
    }

	@Test
	public void testGetSensorObservations100() {
		//register all resource metadata
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

		registerL2Resources(resources);

		//get url
		String name=defaultSensorResource.getResource().getName();//getDefaultSensorName();
        ResponseEntity<FederationSearchResult> query = searchL2Resources(
                new PlatformRegistryQuery.Builder().names(new ArrayList<>(Collections.singleton(name))).build()
        );
        String resourceId = query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId1).getSymbioteId();
		String url=query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId1).getoDataUrl();

        List<Observation> response = rapClient.getTopObservations(url, 100, true, homePlatformIds);

        assertThat(response.size()).isLessThanOrEqualTo(100);
        assertThat(response.get(0).getResourceId()).isEqualTo(resourceId);
    }
	
	@Test
	public void testActuate() {
		// PUT https://2a5235cd.ngrok.io/rap/Actuators('5ac6528f4a234e63b247fec6')
		// Body:
		//		{
		//		  "OnOffCapability" : [Search
		//		    {
		//		      "on" : true
		//		    }
		//		  ]
		//		}

		//register all resource metadata
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

		registerL2Resources(resources);

		//get url
		String name=defaultActuatorResource.getResource().getName();//getDefaultSensorName();
        ResponseEntity<FederationSearchResult> query = searchL2Resources(
                new PlatformRegistryQuery.Builder().names(new ArrayList<>(Collections.singleton(name))).build()
        );

		String url=query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId1).getoDataUrl();
		
		String body = "{\n" + 
				"  \"OnOffCapability\" : [\n" + 
				"    {\n" + 
				"      \"on\" : true\n" + 
				"    }\n" + 
				"  ]\n" + 
				"}";
        rapClient.actuate(url, body, true, homePlatformIds);

	}

	@Test
	public void testInvokeService() {
		// PUT  https://1b38d90c.ngrok.io/rap/Services('5ab5db974a234e717380721f')
		//		[
		//		  {
		//		      "inputParam1" : "on"
		//		  }
		//		]


		//register all resource metadata
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

		registerL2Resources(resources);

		//get url
		String name=defaultServiceResource.getResource().getName();//getDefaultSensorName();
        ResponseEntity<FederationSearchResult> query = searchL2Resources(
                new PlatformRegistryQuery.Builder().names(new ArrayList<>(Collections.singleton(name))).build()
        );

		String url=query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId1).getoDataUrl();
		String body = "[\n" + 
				"  {\n" + 
				"      \"inputParam1\" : \"on\"\n" + 
				"  }\n" + 
				"]";
        String response = rapClient.invokeService(url, body, true, homePlatformIds);
        assertThat(response).isEqualTo("some json");
	}

	@Test
	public void testGetSensorObservationInDifferentFederation() {

		//register all resource metadata
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

		registerL2Resources(resources);

		//get url
		String name=defaultSensorResource.getResource().getName();
		ResponseEntity<FederationSearchResult> query = searchL2Resources(
				new PlatformRegistryQuery.Builder().names(new ArrayList<>(Collections.singleton(name))).build()
		);

		String url = query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId2).getoDataUrl();

		try {
            rapClient.getLatestObservation(url, true, homePlatformIds);
        } catch (FeignException e) {
            assertThat(e.status()).isEqualTo(401);
        }
	}

}
