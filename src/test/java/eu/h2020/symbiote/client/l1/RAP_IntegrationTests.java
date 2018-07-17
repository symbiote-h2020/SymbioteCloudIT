package eu.h2020.symbiote.client.l1;

import eu.h2020.symbiote.client.ClientFixture;
import eu.h2020.symbiote.client.SymbioteCloudITApplication;
import eu.h2020.symbiote.model.cim.Observation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
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
		clearRegistrationHandlerL1();
		registerDefaultL1Resources();
		log.info("JUnit: setup END {}", new RuntimeException().getStackTrace()[0]);
        TimeUnit.SECONDS.sleep(3);
    }

	@After
	public void cleanUp() {
		clearRegistrationHandlerL1();
	}

	@Test
	public void testGetSensorObservation() {
		// GET https://3ef144e8.ngrok.io/rap/Sensors('5ab412f14a234e0f916be9bf')/Observations?$top=1
		String resourceId = findDefaultSensor().getId();
		String url = getResourceUrl(resourceId);

		Observation response = rapClient.getLatestObservation(url, true, homePlatformIds);
		
		assertThat(response.getResourceId()).isEqualTo(resourceId);
	}

	@Test
	public void testGetSensorObservations2() {
		// GET https://3ef144e8.ngrok.io/rap/Sensors('5ab412f14a234e0f916be9bf')/Observations?$top=2
		String resourceId = findDefaultSensor().getId();
		String url = getResourceUrl(resourceId);

		List<Observation> response = rapClient.getTopObservations(url, 2, true, homePlatformIds);
		
		assertThat(response.size()).isLessThanOrEqualTo(2);
        assertThat(response.get(0).getResourceId()).isEqualTo(resourceId);


    }

	@Test
	public void testGetSensorObservations100() {
		// GET https://3ef144e8.ngrok.io/rap/Sensors('5ab412f14a234e0f916be9bf')/Observations?$top=100
		String resourceId = findDefaultSensor().getId();
		String url = getResourceUrl(resourceId);

        List<Observation> response = rapClient.getTopObservations(url, 100, true, homePlatformIds);

        assertThat(response.size()).isLessThanOrEqualTo(100);
        assertThat(response.get(0).getResourceId()).isEqualTo(resourceId);
    }
	
	@Test
	public void testActuate() {
		// PUT https://2a5235cd.ngrok.io/rap/Actuators('5ac6528f4a234e63b247fec6')
		// Body:
		//		{
		//		  "OnOffCapability" : [
		//		    {
		//		      "on" : true
		//		    }
		//		  ]
		//		}
		String resourceId = findDefaultActuator().getId();
		String url = getResourceUrl(resourceId);
		
		
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
		String resourceId = findDefaultService().getId();
		String url = getResourceUrl(resourceId);
		
		String body = "[\n" + 
				"  {\n" + 
				"      \"inputParam1\" : \"on\"\n" + 
				"  }\n" + 
				"]";

		String response = rapClient.invokeService(url, body, true, homePlatformIds);
		assertThat(response).isEqualTo("some json");
	}
	

	private String getResourceUrl(String resourceId) {
		return cramClient.getResourceUrl(resourceId, true, homePlatformIds).getBody().get(resourceId);
	}
}
