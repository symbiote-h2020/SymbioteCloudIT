package eu.h2020.symbiote.client.l12;

import eu.h2020.symbiote.client.ClientFixture;
import eu.h2020.symbiote.client.SymbioteCloudITApplication;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.cloud.model.internal.FederationSearchResult;
import eu.h2020.symbiote.cloud.model.internal.PlatformRegistryQuery;
import eu.h2020.symbiote.core.ci.QueryResponse;
import eu.h2020.symbiote.core.internal.CoreQueryRequest;
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
	public void registerL1shareToFederation() {
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
    public void registerL1shareToFederationDeleteL1() {
        //register and then share resource
        log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

        String fedId = "fed2";
        boolean bartering = true;

        registerL1TestSensorAndShareToFederation(fedId, bartering);

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
    public void registerL1shareToFederationDeleteL1DeleteL2() {
        //register and then share resource
        log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

        String fedId = "fed2";
        boolean bartering = true;

        // Register L1 resource, share it to federation and get back the name
        registerL1TestSensorAndShareToFederation(fedId, bartering);
        ResponseEntity<List<CloudResource>> responseEntity = getResources();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        String name = responseEntity.getBody().get(0).getResource().getName();

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
    public void registerL1shareToFederationDeleteL2DeleteL1() {
        //register and then share resource
        log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

        String fedId = "fed2";
        boolean bartering = true;

        // Register L1 resource, share it to federation and get back the name
        registerL1TestSensorAndShareToFederation(fedId, bartering);
        ResponseEntity<List<CloudResource>> responseEntity = getResources();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        String name = responseEntity.getBody().get(0).getResource().getName();

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
