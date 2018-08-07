package eu.h2020.symbiote.client.l1;

import eu.h2020.symbiote.client.ClientFixture;
import eu.h2020.symbiote.client.SymbioteCloudITApplication;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.security.commons.enums.AccountStatus;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.commons.exceptions.custom.AAMException;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.UserDetails;
import eu.h2020.symbiote.security.communication.payloads.UserManagementRequest;
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SymbioteCloudITApplication.class})
@TestPropertySource(locations = "classpath:application.properties")
//@DirtiesContext
public class PAAM_IntegrationTests extends ClientFixture {
	private static Logger log = LoggerFactory.getLogger(PAAM_IntegrationTests.class);

	@Test
	public void testCreatingUser() {
		log.info("JUnit: START TEST {}", new RuntimeException().getStackTrace()[0]);

		UserManagementRequest userManagementRequest = new UserManagementRequest(
				new Credentials(paamOwnerUsername, paamOwnerPassword), new Credentials(username, password),
				new UserDetails(new Credentials(username, password), "icom@icom.com", UserRole.USER,
						AccountStatus.ACTIVE, new HashMap<>(), new HashMap<>(), true, false),
				OperationType.CREATE);

		try {
			iaamClient.manageUser(userManagementRequest);
			log.info("User registration done");
		} catch (AAMException e) {
			throw new RuntimeException(e);
		}
		log.info("JUnit: END TEST {}", new RuntimeException().getStackTrace()[0]);
	}
}
