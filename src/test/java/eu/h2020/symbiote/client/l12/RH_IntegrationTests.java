package eu.h2020.symbiote.client.l12;

import eu.h2020.symbiote.client.ClientFixture;
import eu.h2020.symbiote.client.SymbioteCloudITApplication;
import eu.h2020.symbiote.cloud.model.internal.*;
import eu.h2020.symbiote.core.ci.QueryResponse;
import eu.h2020.symbiote.core.internal.CoreQueryRequest;
import eu.h2020.symbiote.model.cim.Observation;
import feign.FeignException;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
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
		clearRegistrationHandlerL1();
		clearRegistrationHandlerL2();
		log.info("JUnit: setup END");
	}

	@After
	public void cleanUp() {
		clearRegistrationHandlerL1();
		clearRegistrationHandlerL2();
	}

	@Test
	public void registerL1shareToFederation() throws InterruptedException {
		//register and then share resource
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

        String fedId = "fed2";
        boolean bartering = true;

        Map<String, List<CloudResource>> result = registerL1TestSensorAndShareToFederation(fedId, bartering);

        // Assert that the resource has been shared to the federation
        assertNotNull(result.get(fedId).get(0).getResource().getId());
        assertTrue(result.containsKey(fedId));
        assertNotNull(result.get(fedId).get(0).getFederationInfo().getAggregationId());
        assertTrue(result.get(fedId).get(0).getFederationInfo().getSharingInformation().containsKey(fedId));
        assertNotNull(result.get(fedId).get(0).getFederationInfo().getSharingInformation().get(fedId).getSymbioteId());

        TimeUnit.SECONDS.sleep(2);

        // Search in Core
        String name = result.get(fedId).get(0).getResource().getName();
        ResponseEntity<QueryResponse> queryL1 = searchL1Resources(
                new CoreQueryRequest.Builder().name(name).platformId(platformId).build()
        );

        assertEquals(1, queryL1.getBody().getResources().size());

        // Search Platform Registry
        ResponseEntity<FederationSearchResult> queryL2 = searchL2Resources(
                new PlatformRegistryQuery.Builder().names(new ArrayList<>(Collections.singleton(name))).build()
        );
        assertEquals(1, queryL2.getBody().getResources().size());

		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}



    @Test
    public void registerL2registerL1() {//register L2, then L1. Check federationInfo is not overwitten to null. but remains using getResources at RH
        log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

        LinkedList<CloudResource> resources = new LinkedList<>();
        CloudResource defaultSensorResource = createSensorResource(String.valueOf(System.currentTimeMillis()), "isen1");
        resources.add(defaultSensorResource);

        //register L2
        ResponseEntity<List<CloudResource>> responseEntityL2 = registerL2Resources(resources);
        assertEquals(HttpStatus.OK, responseEntityL2.getStatusCode());
        assertEquals(1, responseEntityL2.getBody().size());

        CloudResource returnedResource = responseEntityL2.getBody().get(0);
        assertEquals(defaultSensorResource.getInternalId(), returnedResource.getInternalId());
        assertNotNull(returnedResource.getFederationInfo());
        assertNotNull(returnedResource.getFederationInfo().getAggregationId());

        //register L1
        ResponseEntity<List<CloudResource>> responseEntityL1 = registerL1Resources(resources);
        assertThat(responseEntityL1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntityL1.getBody()).hasSize(1);

        CloudResource returnedResourceL1 = responseEntityL1.getBody().get(0);
        assertThat(returnedResourceL1.getInternalId()).isEqualTo(defaultSensorResource.getInternalId());
        assertThat(returnedResourceL1.getResource().getId()).isNotNull();

        // Assert again that the Sensor's federation info is not null
        ResponseEntity<List<CloudResource>> responseEntity = getResources();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<CloudResource> result = responseEntity.getBody();
        assertEquals(1, result.size());
        assertEquals(defaultSensorResource.getInternalId(), result.get(0).getInternalId());
        assertNotNull(result.get(0).getFederationInfo());
        assertNotNull(result.get(0).getFederationInfo().getAggregationId());
        assertThat(result.get(0).getResource().getId()).isNotNull();

        log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
    }

    @Test
    public void registerL2registerL1getL2getL1() throws InterruptedException {  //register L2 then L1. Get observations for L2 and L1.
        log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

        LinkedList<CloudResource> resourcesL1 = new LinkedList<>();
        LinkedList<CloudResource> resourcesL2 = new LinkedList<>();
        CloudResource defaultSensorResource = createSensorResource(String.valueOf(System.currentTimeMillis()), "isen1");
        resourcesL1.add(defaultSensorResource);

        //add resource metadata required for L2
        String fedId1="fed1";
        String fedId2="fed2";
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

        resourcesL2.add(defaultSensorResource);

        //register L2
        ResponseEntity<List<CloudResource>> responseEntityL2 = registerL2Resources(resourcesL2);
        assertEquals(HttpStatus.OK, responseEntityL2.getStatusCode());
        assertEquals(1, responseEntityL2.getBody().size());
        CloudResource returnedResource = responseEntityL2.getBody().get(0);
        assertEquals(defaultSensorResource.getInternalId(), returnedResource.getInternalId());
        assertNotNull(returnedResource.getFederationInfo());
        assertNotNull(returnedResource.getFederationInfo().getAggregationId());

        //register L1
        ResponseEntity<List<CloudResource>> responseEntityL1 = registerL1Resources(resourcesL1);
        assertThat(responseEntityL1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntityL1.getBody()).hasSize(1);
        CloudResource returnedResourceL1 = responseEntityL1.getBody().get(0);
        assertThat(returnedResourceL1.getInternalId()).isEqualTo(defaultSensorResource.getInternalId());
        assertThat(returnedResourceL1.getResource().getId()).isNotNull();

        TimeUnit.SECONDS.sleep(2);

        //get resource name
        String name=defaultSensorResource.getResource().getName();

       //get observations for L2: Search in PR and get Url for L2
        ResponseEntity<FederationSearchResult> query = searchL2Resources(
                new PlatformRegistryQuery.Builder().names(new ArrayList<>(Collections.singleton(name))).build()
        );

        String resourceIdL2 = query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId1).getSymbioteId();
        String urlL2=query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId1).getoDataUrl();
        Observation response = rapClient.getLatestObservation(urlL2, true, homePlatformIds);
        assertThat(response.getResourceId()).isEqualTo(resourceIdL2);

        //get observations for L1: Search in Core and get Url for L1
        ResponseEntity<QueryResponse> queryL1 = searchL1Resources(
                new CoreQueryRequest.Builder().name(name).platformId(platformId).build()
        );

        String resourceIdL1 = queryL1.getBody().getResources().get(0).getId();//searchResourceByName();//findDefaultSensor().getId();
        String urlL1 = cramClient.getResourceUrl(resourceIdL1, true, homePlatformIds).getBody().get(resourceIdL1);
        Observation responseL1 = rapClient.getLatestObservation(urlL1, true, homePlatformIds);
        assertThat(responseL1.getResourceId()).isEqualTo(resourceIdL1);

        log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
    }

    @Test
    public void registerL1registerL2getL1getL2() throws InterruptedException {//register L1 then L2. Get observations for L1 and L2.

	    log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

        String fedId1="fed1";
        String fedId2="fed2";

        //add resource metadata for L1
        LinkedList<CloudResource> resourcesL1 = new LinkedList<>();
        CloudResource defaultSensorResource = createSensorResource(String.valueOf(System.currentTimeMillis()), "isen1");
        resourcesL1.add(defaultSensorResource);

        //register L1
        ResponseEntity<List<CloudResource>> responseEntityL1 = registerL1Resources(resourcesL1);
        assertThat(responseEntityL1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntityL1.getBody()).hasSize(1);
        CloudResource returnedResourceL1 = responseEntityL1.getBody().get(0);
        assertThat(returnedResourceL1.getInternalId()).isEqualTo(defaultSensorResource.getInternalId());
        assertThat(returnedResourceL1.getResource().getId()).isNotNull();

        //add extra resource metadata for L2
        LinkedList<CloudResource> resourcesL2 = new LinkedList<>();
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
        resourcesL2.add(defaultSensorResource);

        //register L2
        ResponseEntity<List<CloudResource>> responseEntityL2 = registerL2Resources(resourcesL2);
        assertEquals(HttpStatus.OK, responseEntityL2.getStatusCode());
        assertEquals(1, responseEntityL2.getBody().size());
        CloudResource returnedResource = responseEntityL2.getBody().get(0);
        assertEquals(defaultSensorResource.getInternalId(), returnedResource.getInternalId());
        assertNotNull(returnedResource.getFederationInfo());
        assertNotNull(returnedResource.getFederationInfo().getAggregationId());

        TimeUnit.SECONDS.sleep(2);

        //get resource name
        String name=defaultSensorResource.getResource().getName();

        //get observations for L1: Search in Core and get Url for L1
        ResponseEntity<QueryResponse> queryL1 = searchL1Resources(
                new CoreQueryRequest.Builder().name(name).platformId(platformId).build()
        );

        String resourceIdL1 = queryL1.getBody().getResources().get(0).getId();//searchResourceByName();//findDefaultSensor().getId();
        String urlL1 = cramClient.getResourceUrl(resourceIdL1, true, homePlatformIds).getBody().get(resourceIdL1);
        Observation responseL1 = rapClient.getLatestObservation(urlL1, true, homePlatformIds);
        assertThat(responseL1.getResourceId()).isEqualTo(resourceIdL1);

        //get observations for L2: Search in PR and get Url for L2
        ResponseEntity<FederationSearchResult> query = searchL2Resources(
                new PlatformRegistryQuery.Builder().names(new ArrayList<>(Collections.singleton(name))).build()
        );

        String resourceIdL2 = query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId1).getSymbioteId();
        String urlL2=query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId1).getoDataUrl();
        Observation response = rapClient.getLatestObservation(urlL2, true, homePlatformIds);
        assertThat(response.getResourceId()).isEqualTo(resourceIdL2);

        log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
    }


    @Test
    public void registerL2registerL1deleteL2getL1() throws InterruptedException {  //register L2 then L1. Get observations for L2 and L1.
        log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

        LinkedList<CloudResource> resourcesL1 = new LinkedList<>();
        LinkedList<CloudResource> resourcesL2 = new LinkedList<>();
        CloudResource defaultSensorResource = createSensorResource(String.valueOf(System.currentTimeMillis()), "isen1");
        resourcesL1.add(defaultSensorResource);

        //add resource metadata required for L2
        String fedId1="fed1";
        String fedId2="fed2";
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

        resourcesL2.add(defaultSensorResource);

        //register L2
        ResponseEntity<List<CloudResource>> responseEntityL2 = registerL2Resources(resourcesL2);
        assertEquals(HttpStatus.OK, responseEntityL2.getStatusCode());
        assertEquals(1, responseEntityL2.getBody().size());
        CloudResource returnedResource = responseEntityL2.getBody().get(0);
        assertEquals(defaultSensorResource.getInternalId(), returnedResource.getInternalId());
        assertNotNull(returnedResource.getFederationInfo());
        assertNotNull(returnedResource.getFederationInfo().getAggregationId());

        //register L1
        ResponseEntity<List<CloudResource>> responseEntityL1 = registerL1Resources(resourcesL1);
        assertThat(responseEntityL1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntityL1.getBody()).hasSize(1);
        CloudResource returnedResourceL1 = responseEntityL1.getBody().get(0);
        assertThat(returnedResourceL1.getInternalId()).isEqualTo(defaultSensorResource.getInternalId());
        assertThat(returnedResourceL1.getResource().getId()).isNotNull();

        //get resource name
        String name=defaultSensorResource.getResource().getName();

        // Delete from L2
        ResponseEntity<List<CloudResource>> responseL2 = deleteAllL2Resources();
        assertThat(responseL2.getStatusCode()).isEqualTo(HttpStatus.OK);

        //rhClient.removeL2Resources(ids);

        TimeUnit.SECONDS.sleep(2);

        //get observations for L1: Search in Core and get Url for L1
        ResponseEntity<QueryResponse> queryL1 = searchL1Resources(
                new CoreQueryRequest.Builder().name(name).platformId(platformId).build()
        );

        String resourceIdL1 = queryL1.getBody().getResources().get(0).getId();//searchResourceByName();//findDefaultSensor().getId();
        String urlL1 = cramClient.getResourceUrl(resourceIdL1, true, homePlatformIds).getBody().get(resourceIdL1);
        Observation responseL1 = rapClient.getLatestObservation(urlL1, true, homePlatformIds);
        assertThat(responseL1.getResourceId()).isEqualTo(resourceIdL1);

        log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
    }

    @Test
    public void registerL1registerL2deleteL1getL2() throws InterruptedException{//register L1 then L2. Get observations for L1 and L2.

        log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

        String fedId1="fed1";
        String fedId2="fed2";

        //add resource metadata for L1
        LinkedList<CloudResource> resourcesL1 = new LinkedList<>();
        CloudResource defaultSensorResource = createSensorResource(String.valueOf(System.currentTimeMillis()), "isen1");
        resourcesL1.add(defaultSensorResource);

        //register L1
        ResponseEntity<List<CloudResource>> responseEntityL1 = registerL1Resources(resourcesL1);
        assertThat(responseEntityL1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntityL1.getBody()).hasSize(1);
        CloudResource returnedResourceL1 = responseEntityL1.getBody().get(0);

        assertThat(returnedResourceL1.getInternalId()).isEqualTo(defaultSensorResource.getInternalId());
        assertThat(returnedResourceL1.getResource().getId()).isNotNull();

        //add extra resource metadata for L2
        LinkedList<CloudResource> resourcesL2 = new LinkedList<>();
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
        resourcesL2.add(defaultSensorResource);

        //register L2
        ResponseEntity<List<CloudResource>> responseEntityL2 = registerL2Resources(resourcesL2);
        assertEquals(HttpStatus.OK, responseEntityL2.getStatusCode());
        assertEquals(1, responseEntityL2.getBody().size());
        CloudResource returnedResource = responseEntityL2.getBody().get(0);
        assertEquals(defaultSensorResource.getInternalId(), returnedResource.getInternalId());
        assertNotNull(returnedResource.getFederationInfo());
        assertNotNull(returnedResource.getFederationInfo().getAggregationId());

        //get resource name
        String name=defaultSensorResource.getResource().getName();

        TimeUnit.SECONDS.sleep(2);

        // Delete from L1
        ResponseEntity<List<CloudResource>> responseL1 = deleteAllL1Resources();
        assertThat(responseL1.getStatusCode()).isEqualTo(HttpStatus.OK);


        //get observations for L2: Search in PR and get Url for L2
        ResponseEntity<FederationSearchResult> query = searchL2Resources(
                new PlatformRegistryQuery.Builder().names(new ArrayList<>(Collections.singleton(name))).build()
        );

        String resourceIdL2 = query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId1).getSymbioteId();
        String urlL2=query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId1).getoDataUrl();
        Observation responseL2 = rapClient.getLatestObservation(urlL2, true, homePlatformIds);
        assertThat(responseL2.getResourceId()).isEqualTo(resourceIdL2);

        log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
    }

    @Test(expected = FeignException.class)
    public void registerL2registerL1deleteL1getL1() throws InterruptedException {//register L2 then L1. delete L2, get L2 should fail.
        log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

        LinkedList<CloudResource> resourcesL1 = new LinkedList<>();
        LinkedList<CloudResource> resourcesL2 = new LinkedList<>();
        CloudResource defaultSensorResource = createSensorResource(String.valueOf(System.currentTimeMillis()), "isen1");
        resourcesL1.add(defaultSensorResource);

        //add resource metadata required for L2
        String fedId1="fed1";
        String fedId2="fed2";
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

        resourcesL2.add(defaultSensorResource);

        //register L2
        ResponseEntity<List<CloudResource>> responseEntityL2 = registerL2Resources(resourcesL2);
        assertEquals(HttpStatus.OK, responseEntityL2.getStatusCode());
        assertEquals(1, responseEntityL2.getBody().size());
        CloudResource returnedResource = responseEntityL2.getBody().get(0);
        assertEquals(defaultSensorResource.getInternalId(), returnedResource.getInternalId());
        assertNotNull(returnedResource.getFederationInfo());
        assertNotNull(returnedResource.getFederationInfo().getAggregationId());

        //register L1
        ResponseEntity<List<CloudResource>> responseEntityL1 = registerL1Resources(resourcesL1);
        assertThat(responseEntityL1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntityL1.getBody()).hasSize(1);
        CloudResource returnedResourceL1 = responseEntityL1.getBody().get(0);
        assertThat(returnedResourceL1.getInternalId()).isEqualTo(defaultSensorResource.getInternalId());
        assertThat(returnedResourceL1.getResource().getId()).isNotNull();

        TimeUnit.SECONDS.sleep(2);

        //search in core for L1
        String name=defaultSensorResource.getResource().getName();
        ResponseEntity<QueryResponse> queryL1 = searchL1Resources(
                new CoreQueryRequest.Builder().name(name).platformId(platformId).build()
        );
        String resourceIdL1 = queryL1.getBody().getResources().get(0).getId();//searchResourceByName();//findDefaultSensor().getId();
        String urlL1 = cramClient.getResourceUrl(resourceIdL1, true, homePlatformIds).getBody().get(resourceIdL1);

        // Delete from L1
        ResponseEntity<List<CloudResource>> responseL1 = deleteAllL1Resources();
        assertThat(responseL1.getStatusCode()).isEqualTo(HttpStatus.OK);

        //get observations for L1 should fail
        Observation observationL1 = rapClient.getLatestObservation(urlL1, true, homePlatformIds);
        assertThat(observationL1.getResourceId()).isEqualTo(resourceIdL1);

        log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
    }

    @Test(expected = FeignException.class)
    public void registerL2registerL1deleteL2getL2() throws InterruptedException {//register L2 then L1. delete L2, get L2 should fail.
        log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

        LinkedList<CloudResource> resourcesL1 = new LinkedList<>();
        LinkedList<CloudResource> resourcesL2 = new LinkedList<>();
        CloudResource defaultSensorResource = createSensorResource(String.valueOf(System.currentTimeMillis()), "isen1");
        resourcesL1.add(defaultSensorResource);

        //add resource metadata required for L2
        String fedId1="fed1";
        String fedId2="fed2";
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

        resourcesL2.add(defaultSensorResource);

        //register L2
        ResponseEntity<List<CloudResource>> responseEntityL2 = registerL2Resources(resourcesL2);
        assertEquals(HttpStatus.OK, responseEntityL2.getStatusCode());
        assertEquals(1, responseEntityL2.getBody().size());
        CloudResource returnedResource = responseEntityL2.getBody().get(0);
        assertEquals(defaultSensorResource.getInternalId(), returnedResource.getInternalId());
        assertNotNull(returnedResource.getFederationInfo());
        assertNotNull(returnedResource.getFederationInfo().getAggregationId());

        //register L1
        ResponseEntity<List<CloudResource>> responseEntityL1 = registerL1Resources(resourcesL1);
        assertThat(responseEntityL1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntityL1.getBody()).hasSize(1);
        CloudResource returnedResourceL1 = responseEntityL1.getBody().get(0);
        assertThat(returnedResourceL1.getInternalId()).isEqualTo(defaultSensorResource.getInternalId());
        assertThat(returnedResourceL1.getResource().getId()).isNotNull();

        TimeUnit.SECONDS.sleep(2);

        //search in PR for L2 and get url
        String name=defaultSensorResource.getResource().getName();
        ResponseEntity<FederationSearchResult> query = searchL2Resources(
                new PlatformRegistryQuery.Builder().names(new ArrayList<>(Collections.singleton(name))).build()
        );

        String resourceIdL2 = query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId1).getSymbioteId();
        String urlL2=query.getBody().getResources().get(0).getFederatedResourceInfoMap().get(fedId1).getoDataUrl();

        // Delete from L2
        ResponseEntity<List<CloudResource>> responseL2 = deleteAllL2Resources();
        assertThat(responseL2.getStatusCode()).isEqualTo(HttpStatus.OK);

        //get observations for L2 should fail
        Observation observationL2 = rapClient.getLatestObservation(urlL2, true, homePlatformIds);
        assertThat(observationL2.getResourceId()).isEqualTo(resourceIdL2);

        log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
    }

    @Test
    public void registerL1shareToFederationDeleteL1() throws InterruptedException {
        //register and then share resource
        log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

        String fedId = "fed2";
        boolean bartering = true;

        registerL1TestSensorAndShareToFederation(fedId, bartering);

        TimeUnit.SECONDS.sleep(2);

        // Delete from L1
        ResponseEntity<List<CloudResource>> responseEntity = deleteAllL1Resources();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Assert that the Sensor has no symbIoTeId but it has aggregationId
        responseEntity = getResources();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<CloudResource> result = responseEntity.getBody();
        assertEquals(1, result.size());
        assertNull(result.get(0).getResource().getId());
        assertNotNull(result.get(0).getFederationInfo().getAggregationId());
        assertTrue(result.get(0).getFederationInfo().getSharingInformation().containsKey(fedId));
        assertNotNull(result.get(0).getFederationInfo().getSharingInformation().get(fedId).getSymbioteId());

        TimeUnit.SECONDS.sleep(2);

        // Search in Core
        String name = result.get(0).getResource().getName();
        ResponseEntity<QueryResponse> queryL1 = searchL1Resources(
                new CoreQueryRequest.Builder().name(name).platformId(platformId).build()
        );

        assertEquals(0, queryL1.getBody().getResources().size());

        // Search Platform Registry
        ResponseEntity<FederationSearchResult> queryL2 = searchL2Resources(
                new PlatformRegistryQuery.Builder().names(new ArrayList<>(Collections.singleton(name))).build()
        );
        assertEquals(1, queryL2.getBody().getResources().size());

        log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
    }

    @Test
    public void registerL1shareToFederationDeleteL2() throws Exception {
        //register and then share resource
        log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

        String fedId = "fed2";
        boolean bartering = true;

        registerL1TestSensorAndShareToFederation(fedId, bartering);

        // Delete from L2
        ResponseEntity<List<CloudResource>> responseEntity = deleteAllL2Resources();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Assert that the Sensor has no federation info but it has symbioteId from Core
        responseEntity = getResources();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<CloudResource> result = responseEntity.getBody();
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getResource().getId());
        assertNull(result.get(0).getFederationInfo());

        // Search in Core
        TimeUnit.SECONDS.sleep(1);
        String name = result.get(0).getResource().getName();
        ResponseEntity<QueryResponse> queryL1 = searchL1Resources(
                new CoreQueryRequest.Builder().name(name).platformId(platformId).build()
        );

        assertEquals(1, queryL1.getBody().getResources().size());

        // Search Platform Registry
        ResponseEntity<FederationSearchResult> queryL2 = searchL2Resources(
                new PlatformRegistryQuery.Builder().names(new ArrayList<>(Collections.singleton(name))).build()
        );
        assertEquals(0, queryL2.getBody().getResources().size());

        log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
    }

    @Test
    public void registerL1shareToFederationDeleteL1DeleteL2() throws InterruptedException {
        //register and then share resource
        log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

        String fedId = "fed2";
        boolean bartering = true;

        // Register L1 resource, share it to federation and get back the name
        registerL1TestSensorAndShareToFederation(fedId, bartering);
        ResponseEntity<List<CloudResource>> responseEntity = getResources();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        String name = responseEntity.getBody().get(0).getResource().getName();

        TimeUnit.SECONDS.sleep(2);

        // Delete from L1
        responseEntity = deleteAllL1Resources();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Delete from L2
        responseEntity = deleteAllL2Resources();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Assert that the Sensor has no federation info but it has symbioteId from Core
        responseEntity = getResources();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<CloudResource> result = responseEntity.getBody();
        assertEquals(0, result.size());

        TimeUnit.SECONDS.sleep(2);

        // Search in Core
        ResponseEntity<QueryResponse> queryL1 = searchL1Resources(
                new CoreQueryRequest.Builder().name(name).platformId(platformId).build()
        );

        assertEquals(0, queryL1.getBody().getResources().size());

        // Search Platform Registry
        ResponseEntity<FederationSearchResult> queryL2 = searchL2Resources(
                new PlatformRegistryQuery.Builder().names(new ArrayList<>(Collections.singleton(name))).build()
        );
        assertEquals(0, queryL2.getBody().getResources().size());

        log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
    }

    @Test
    public void registerL1shareToFederationDeleteL2DeleteL1() throws InterruptedException {
        //register and then share resource
        log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

        String fedId = "fed2";
        boolean bartering = true;

        // Register L1 resource, share it to federation and get back the name
        registerL1TestSensorAndShareToFederation(fedId, bartering);
        ResponseEntity<List<CloudResource>> responseEntity = getResources();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        String name = responseEntity.getBody().get(0).getResource().getName();

        TimeUnit.SECONDS.sleep(2);

        // Delete from L2
        responseEntity = deleteAllL2Resources();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Delete from L1
        responseEntity = deleteAllL1Resources();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Assert that the Sensor has no federation info but it has symbioteId from Core
        responseEntity = getResources();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<CloudResource> result = responseEntity.getBody();
        assertEquals(0, result.size());

        // Search in Core
        ResponseEntity<QueryResponse> queryL1 = searchL1Resources(
                new CoreQueryRequest.Builder().name(name).platformId(platformId).build()
        );

        assertEquals(0, queryL1.getBody().getResources().size());

        // Search Platform Registry
        ResponseEntity<FederationSearchResult> queryL2 = searchL2Resources(
                new PlatformRegistryQuery.Builder().names(new ArrayList<>(Collections.singleton(name))).build()
        );
        assertEquals(0, queryL2.getBody().getResources().size());

        log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
    }

	private Map<String, List<CloudResource>> shareResourceToFederation(CloudResource cloudResource, String fedId, boolean bartering) {
        Map<String, Map<String, Boolean>> sharingMap = new HashMap<>();
        sharingMap.put(fedId, new HashMap<>());
        sharingMap.get(fedId).put(cloudResource.getInternalId(), bartering);
        ResponseEntity<?> responseEntity2 = shareResources(sharingMap);
        return (Map<String, List<CloudResource>>)responseEntity2.getBody();
    }

    private Map<String, List<CloudResource>> registerL1TestSensorAndShareToFederation(String fedId, boolean bartering) {
        // Register L1 resource
        LinkedList<CloudResource> resources = new LinkedList<>();
        CloudResource defaultSensorResource = createSensorResource(String.valueOf(System.currentTimeMillis()), "isen1");
        resources.add(defaultSensorResource);

        ResponseEntity<List<CloudResource>> responseEntity = registerL1Resources(resources);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // Share the resource to a federation
        return shareResourceToFederation(defaultSensorResource, fedId, bartering);
    }
}
