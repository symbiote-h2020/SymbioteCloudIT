package eu.h2020.symbiote.client.l2;

import eu.h2020.symbiote.client.ClientFixture;
import eu.h2020.symbiote.client.LambdaCondition;
import eu.h2020.symbiote.client.SymbioteCloudITApplication;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.cloud.model.internal.FederationSearchResult;
import eu.h2020.symbiote.core.ci.QueryResourceResult;
import eu.h2020.symbiote.core.ci.QueryResponse;
import eu.h2020.symbiote.core.internal.cram.ResourceUrlsResponse;
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SymbioteCloudITApplication.class})
@TestPropertySource(locations = "classpath:application.properties")
//@DirtiesContext
public class PR_IntegrationTests extends ClientFixture {
	private static Logger log = LoggerFactory.getLogger(PR_IntegrationTests.class);
	
	@Before
	public void setUp() throws Exception {
		log.info("JUnit: setup START {}", new RuntimeException().getStackTrace()[0]);
		clearRegistrationHandlerL2();
		registerDefaultL2Resources();
		log.info("JUnit: setup END {}", new RuntimeException().getStackTrace()[0]);
        TimeUnit.SECONDS.sleep(5);
    }

	@After
	public void cleanUp() {
		clearRegistrationHandlerL2();
	}

	@Test
	public void testSearchResourcesByName() {

		String names[] = {getDefaultSensorName(),
				getDefaultServiceName()};
		String name = String.join(",",names);

		ResponseEntity<FederationSearchResult> query = searchL2Resources(platformId,
				"?name="+name);

		assertThat(query.getStatusCodeValue()).isEqualTo(200);
		assertThat(query.getBody().getResources())
				.filteredOn(new LambdaCondition<>(
						r -> r.getCloudResource().getResource().getName().contains(defaultResourceIdPrefix)
				))
				.extracting("cloudResource.resource.name")
				.containsOnly(getDefaultSensorName(),
						getDefaultServiceName());
	}


	@Test
	public void testSearchResources() {

		String predicate="";// null predicate for findAll();
		ResponseEntity<FederationSearchResult> query = searchL2Resources(platformId,
				predicate);

		assertThat(query.getStatusCodeValue()).isEqualTo(200);
		assertThat(query.getBody().getResources())
				.filteredOn(new LambdaCondition<>(
						r -> r.getCloudResource().getResource().getName().contains(defaultResourceIdPrefix)
				))
				.extracting("cloudResource.resource.name")
				.containsOnly(getDefaultSensorName(),
							getDefaultActuatorName(),
						getDefaultServiceName());
	}


	@Test
	public void testSearchResourcesLocatedAt() {

		String predicate="?location_name=Berlin";

		ResponseEntity<FederationSearchResult> query = searchL2Resources(platformId,
				predicate);

		assertThat(query.getStatusCodeValue()).isEqualTo(200);
		assertThat(query.getBody().getResources())
				.filteredOn(new LambdaCondition<>(
						r -> r.getCloudResource().getResource().getName().contains(defaultResourceIdPrefix)
				))
				.extracting("cloudResource.resource.name")
				.containsOnly(getDefaultSensorName(),
						getDefaultActuatorName());
	}



//	@Test //todo: check updates from trust manager are successful
	public void testSearchResourcesTrust() {

		String predicate="?resource_trust=1.0&adaptive_trust=6.0";

		ResponseEntity<FederationSearchResult> query = searchL2Resources(platformId,
				predicate);

		assertThat(query.getStatusCodeValue()).isEqualTo(200);
		assertThat(query.getBody().getResources())
				.filteredOn(new LambdaCondition<>(
						r -> r.getCloudResource().getResource().getName().contains(defaultResourceIdPrefix)
				))
				.extracting("cloudResource.resource.name")
				.containsOnly(getDefaultSensorName(),
						getDefaultActuatorName(),
						getDefaultServiceName());
	}


}
