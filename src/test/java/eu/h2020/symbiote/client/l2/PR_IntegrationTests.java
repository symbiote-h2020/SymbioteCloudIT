package eu.h2020.symbiote.client.l2;

import eu.h2020.symbiote.client.ClientFixture;
import eu.h2020.symbiote.client.LambdaCondition;
import eu.h2020.symbiote.client.SymbioteCloudITApplication;
import eu.h2020.symbiote.cloud.model.internal.FederationSearchResult;
import eu.h2020.symbiote.cloud.model.internal.PlatformRegistryQuery;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

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

		ResponseEntity<FederationSearchResult> query = searchL2Resources(
				new PlatformRegistryQuery.Builder().names(new ArrayList<>(Arrays.asList(getDefaultSensorName(),
                        getDefaultServiceName()))).build()
		);

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
	public void testSearchResourcesByDescription() {

		ResponseEntity<FederationSearchResult> query = searchL2Resources(//@type=
				new PlatformRegistryQuery.Builder().descriptions(Arrays.asList("@type=Beacon")).build()
		);

		String serviceName=getDefaultServiceName();
		assertThat(query.getStatusCodeValue()).isEqualTo(200);
		assertThat(query.getBody().getResources())
				.filteredOn(new LambdaCondition<>(
						r -> r.getCloudResource().getResource().getName().contains(defaultResourceIdPrefix)
				))
				.extracting("cloudResource.resource.name")
				.containsOnly(getDefaultServiceName());
	}

	@Test
	public void testSearchResources() {

		ResponseEntity<FederationSearchResult> query = searchL2Resources(
				new PlatformRegistryQuery.Builder().build()
		);

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

		ResponseEntity<FederationSearchResult> query = searchL2Resources(
		        new PlatformRegistryQuery.Builder().locationNames(new ArrayList<>(Collections.singleton("Berlin"))).build()
        );

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

		ResponseEntity<FederationSearchResult> query = searchL2Resources(
		        new PlatformRegistryQuery.Builder().resourceTrust(1.0).adaptiveTrust(6.0).build()
        );

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
