package eu.h2020.symbiote.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import eu.h2020.symbiote.client.model.SparqlQueryRequestWrapper;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.cloud.model.internal.FederationSearchResult;
import eu.h2020.symbiote.core.ci.QueryResponse;
import eu.h2020.symbiote.core.internal.CoreQueryRequest;
import eu.h2020.symbiote.core.internal.cram.ResourceUrlsResponse;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.security.ClientSecurityHandlerFactory;
import eu.h2020.symbiote.security.commons.SecurityConstants;
import eu.h2020.symbiote.security.commons.Token;
import eu.h2020.symbiote.security.commons.credentials.AuthorizationCredentials;
import eu.h2020.symbiote.security.commons.credentials.HomeCredentials;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.commons.exceptions.custom.AAMException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import eu.h2020.symbiote.security.communication.AAMClient;
import eu.h2020.symbiote.security.communication.IAAMClient;
import eu.h2020.symbiote.security.communication.payloads.*;
import eu.h2020.symbiote.security.handler.ISecurityHandler;
import eu.h2020.symbiote.security.helpers.MutualAuthenticationHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 9/17/2017.
 */
@Component
public class SymbioteClient {

	private static Log log = LogFactory.getLog(SymbioteClient.class);

    private String symbIoTeCoreUrl;
    private String homePlatformUrl;
	private RestTemplate restTemplate;
	private ISecurityHandler securityHandler;
	private String username;
	private String password;
	private String clientId;
	private String paamOwnerUsername;
	private String paamOwnerPassword;

	@Autowired
	public SymbioteClient(
	        @Qualifier("symbIoTeCoreUrl") String symbIoTeCoreUrl, RestTemplate restTemplate,
			@Value("${coreAAMAddress}") String coreAAMAddress, @Value("${keystorePath}") String keystorePath,
			@Value("${keystorePassword}") String keystorePassword, @Value("${userId}") String userId,
			@Value("${demoApp.username}") String username, @Value("${demoApp.password}") String password,
			@Value("${clientId}") String clientId, @Value("${paamOwner.username}") String paamOwnerUsername,
			@Value("${paamOwner.password}") String paamOwnerPassword,
            @Value("${test.iiUrl}") String homePlatformUrl
			)
			throws SecurityHandlerException, NoSuchAlgorithmException {

		Assert.notNull(symbIoTeCoreUrl, "symbIoTeCoreUrl can not be null!");
		this.symbIoTeCoreUrl = symbIoTeCoreUrl;

		Assert.notNull(restTemplate, "RestTemplate can not be null!");
		this.restTemplate = restTemplate;

		Assert.notNull(coreAAMAddress, "coreAAMAddress can not be null!");
		Assert.notNull(keystorePath, "keystorePath can not be null!");
		Assert.notNull(keystorePassword, "keystorePassword can not be null!");
		Assert.notNull(userId, "userId can not be null!");

		Assert.notNull(username, "username can not be null!");
		this.username = username;

		Assert.notNull(password, "password can not be null!");
		this.password = password;

		Assert.notNull(clientId, "clientId can not be null!");
		this.clientId = clientId;

		Assert.notNull(paamOwnerUsername, "paamOwnerUsername can not be null!");
		this.paamOwnerUsername = paamOwnerUsername;

		Assert.notNull(paamOwnerPassword, "paamOwnerPassword can not be null!");
		this.paamOwnerPassword = paamOwnerPassword;

        Assert.notNull(homePlatformUrl, "homePlatformUrl can not be null!");
        this.homePlatformUrl = homePlatformUrl;

		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}
		} };
		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		try {
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		securityHandler = ClientSecurityHandlerFactory.getSecurityHandler(coreAAMAddress, keystorePath,
				keystorePassword, userId);

	}

	public void registerToPAAM(@RequestParam String platformId, @RequestParam(required = false) String directAAMUrl) {
		log.info("Registering to PAAM: " + platformId);
		try {

			Optional<String> opAAMUrl = Optional.ofNullable(directAAMUrl);
			IAAMClient aamClient;
			if (opAAMUrl.isPresent()) {
				log.info("Registering to PAAM: " + platformId + " with url " + opAAMUrl.get());
				aamClient = new AAMClient(opAAMUrl.get());
			} else {
				Map<String, AAM> availableAAMs = securityHandler.getAvailableAAMs();
				aamClient = new AAMClient(availableAAMs.get(platformId).getAamAddress());
				log.info("Registering to PAAM: " + platformId + " with url "
						+ availableAAMs.get(platformId).getAamAddress());
			}

			UserManagementRequest userManagementRequest = new UserManagementRequest(
					new Credentials(paamOwnerUsername, paamOwnerPassword), new Credentials(username, password),
					new UserDetails(new Credentials(username, password), "icom@icom.com", UserRole.USER,
							new HashMap<>(), new HashMap<>()),
					OperationType.CREATE);

			try {
				aamClient.manageUser(userManagementRequest);
				log.info("User registration done");
			} catch (AAMException e) {
				throw new RuntimeException(e);
			}
		} catch (SecurityHandlerException e) {
			throw new RuntimeException(e);
		}
	}

	public ResponseEntity<QueryResponse> query(@RequestParam(value = "platform_id", required = false) String platformId,
			@RequestParam(value = "platform_name", required = false) String platformName,
			@RequestParam(value = "owner", required = false) String owner,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "id", required = false) String id,
			@RequestParam(value = "description", required = false) String description,
			@RequestParam(value = "location_name", required = false) String location_name,
			@RequestParam(value = "location_lat", required = false) Double location_lat,
			@RequestParam(value = "location_long", required = false) Double location_long,
			@RequestParam(value = "max_distance", required = false) Integer max_distance,
			@RequestParam(value = "observed_property", required = false) String[] observed_property,
			@RequestParam(value = "observed_property_iri", required = false) String[] observed_property_iri,
			@RequestParam(value = "resource_type", required = false) String resource_type,
			@RequestParam(value = "should_rank", required = false) Boolean should_rank,
			@RequestParam String homePlatformId) {

		log.info("Searching for resources with token from platform " + homePlatformId);

		CoreQueryRequest queryRequest = new CoreQueryRequest();
		queryRequest.setPlatform_id(platformId);
		queryRequest.setPlatform_name(platformName);
		queryRequest.setOwner(owner);
		queryRequest.setName(name);
		queryRequest.setId(id);
		queryRequest.setDescription(description);
		queryRequest.setLocation_name(location_name);
		queryRequest.setLocation_lat(location_lat);
		queryRequest.setLocation_long(location_long);
		queryRequest.setMax_distance(max_distance);
		queryRequest.setResource_type(resource_type);
		queryRequest.setShould_rank(should_rank);

		if (observed_property != null) {
			queryRequest.setObserved_property(Arrays.asList(observed_property));
		}

		if (observed_property_iri != null) {
			queryRequest.setObserved_property_iri(Arrays.asList(observed_property_iri));
		}

		String queryUrl = queryRequest.buildQuery(symbIoTeCoreUrl).replaceAll("#", "%23");
		log.info("queryUrl = " + queryUrl);

		return sendRequestAndVerifyResponse(HttpMethod.GET, queryUrl, homePlatformId,
				SecurityConstants.CORE_AAM_INSTANCE_ID, "search", new ParameterizedTypeReference<QueryResponse>() {
				});

	}

	public ResponseEntity<FederationSearchResult> queryL2( String homePlatformId,
												  String predicate) {

		log.info("Searching for resources with token from platform " + homePlatformId);

		String prUrl =  homePlatformUrl + "/pr/search" + predicate;
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);

		return sendRequestAndVerifyResponse(HttpMethod.GET, prUrl, homePlatformId,
                homePlatformId, "platformRegistry", new ParameterizedTypeReference<FederationSearchResult>() {
				});

	}

	public ResponseEntity<Object> sparqlQuery(@RequestBody SparqlQueryRequestWrapper sparqlQueryRequestWrapper) {

		log.info("SPARQL query request " + sparqlQueryRequestWrapper);

		ObjectMapper mapper = new ObjectMapper();

		try {

			if (sparqlQueryRequestWrapper.getSparqlQueryRequest() == null)
				throw new RuntimeException("sparqlQueryRequestWrapper should not be null");
			if (sparqlQueryRequestWrapper.getHomePlatformId() == null)
				throw new RuntimeException("homePlatformId should not be null");

			String sparqlQueryRequestAsString = mapper
					.writeValueAsString(sparqlQueryRequestWrapper.getSparqlQueryRequest());
			String url = symbIoTeCoreUrl + "/sparqlQuery";

			return sendSETRequestAndVerifyResponse(HttpMethod.POST, url, sparqlQueryRequestWrapper.getHomePlatformId(),
					SecurityConstants.CORE_AAM_INSTANCE_ID, sparqlQueryRequestAsString, "search",
					new ParameterizedTypeReference<Object>() {
					});
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public ResponseEntity<ResourceUrlsResponse> getResourceUrlFromCram(@RequestParam String resourceId,
			@RequestParam String platformId) {

		log.info("Requesting url from CRAM for the resource with id " + resourceId + " with token from platform"
				+ platformId);

		String cramRequestUrl = symbIoTeCoreUrl + "/resourceUrls?id=" + resourceId;
		return sendRequestAndVerifyResponse(HttpMethod.GET, cramRequestUrl, platformId,
				SecurityConstants.CORE_AAM_INSTANCE_ID, "cram", new ParameterizedTypeReference<ResourceUrlsResponse>() {
				});

	}

	public ResponseEntity<List<Observation>> getResourceObservationHistory(@RequestParam String resourceUrl,
			@RequestParam String platformId) {

		log.info("Getting observations for the resource with url " + resourceUrl + " and platformId " + platformId);

		return sendRequestAndVerifyResponse(HttpMethod.GET, resourceUrl, platformId, platformId, "rap",
				new ParameterizedTypeReference<List<Observation>>() {
				});
	}

	public ResponseEntity<?> actuateResource(@RequestParam String resourceUrl, @RequestParam String platformId,
			@RequestBody String body) {

		log.info("Actuating resource with url " + resourceUrl + " and platformId " + platformId);
		return sendSETRequestAndVerifyResponse(HttpMethod.PUT, resourceUrl, platformId, platformId, body, "rap",
				new ParameterizedTypeReference<Object>() {
				});
	}

	public <T> ResponseEntity<T> invokeService(@RequestParam String resourceUrl, @RequestParam String platformId,
			@RequestBody String body, Class<T> clazz) {

		log.info("Invoking service  with url " + resourceUrl + " and platformId " + platformId);
		return sendSETRequestAndVerifyResponse(HttpMethod.PUT, resourceUrl, platformId, platformId, body, "rap",
				ParameterizedTypeReference.forType(clazz));
	}

	public Object getResourceObservationHistoryWithHomeToken(@RequestParam String resourceUrl,
			@RequestParam String platformId) {

		log.info("Getting observations for the resource with url " + resourceUrl + " and platformId " + platformId);
		return sendRequestAndVerifyResponse(HttpMethod.GET, resourceUrl, platformId, platformId, "rap",
				new ParameterizedTypeReference<Object>() {
				});
	}

	public Object getResourceObservationHistoryWithForeignToken(@RequestParam String resourceUrl,
			@RequestParam String homePlatformId, @RequestParam String federatedPlatformId) {

		log.info("Getting observations for the resource with url: " + resourceUrl + " by using a Foreign token");
		return sendRequestAndVerifyResponse(HttpMethod.GET, resourceUrl, homePlatformId, federatedPlatformId, "rap",
				new ParameterizedTypeReference<Object>() {
				});
	}

	private <T> ResponseEntity<T> sendRequestAndVerifyResponse(HttpMethod httpMethod, String url, String homePlatformId,
			String targetPlatformId, String componentId, ParameterizedTypeReference<T> type) {

		Map<String, String> securityRequestHeaders;
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);

		// Insert Security Request into the headers
		try {

			Set<AuthorizationCredentials> authorizationCredentialsSet = new HashSet<>();
			Map<String, AAM> availableAAMs = securityHandler.getAvailableAAMs();

			log.info("Getting certificate for " + availableAAMs.get(homePlatformId).getAamInstanceId());
			securityHandler.getCertificate(availableAAMs.get(homePlatformId), username, password, clientId);

			log.info("Getting token from " + availableAAMs.get(homePlatformId).getAamInstanceId());
			Token homeToken = securityHandler.login(availableAAMs.get(homePlatformId));

			HomeCredentials homeCredentials = securityHandler.getAcquiredCredentials()
					.get(homePlatformId).homeCredentials;
			authorizationCredentialsSet
					.add(new AuthorizationCredentials(homeToken, homeCredentials.homeAAM, homeCredentials));

			SecurityRequest securityRequest = MutualAuthenticationHelper.getSecurityRequest(authorizationCredentialsSet,
					false);
			securityRequestHeaders = securityRequest.getSecurityRequestHeaderParams();

		} catch (SecurityHandlerException | ValidationException | JsonProcessingException
				| NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		for (Map.Entry<String, String> entry : securityRequestHeaders.entrySet()) {
			httpHeaders.add(entry.getKey(), entry.getValue());
		}
		log.info("request headers: " + httpHeaders);

		HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);

		ResponseEntity<T> responseEntity = null;
		try {
			responseEntity = restTemplate.exchange(url, httpMethod, httpEntity, type);
		} catch (RestClientResponseException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		log.info("response = " + responseEntity);
		log.info("headers = " + responseEntity.getHeaders());
		log.info("body = " + responseEntity.getBody());

		String serviceResponse = responseEntity.getHeaders().get(SecurityConstants.SECURITY_RESPONSE_HEADER).get(0);

		if (serviceResponse == null)
			throw new RuntimeException("The receiver was not authenticated");

		boolean isServiceResponseVerified;
		try {
			isServiceResponseVerified = MutualAuthenticationHelper.isServiceResponseVerified(serviceResponse,
					securityHandler.getComponentCertificate(componentId, targetPlatformId));
		} catch (CertificateException | NoSuchAlgorithmException | SecurityHandlerException e) {
			throw new RuntimeException(e);
		}

		if (isServiceResponseVerified) {
			return responseEntity;
		} else {
			throw new RuntimeException("The service response is not verified");
		}
	}

	private <T> ResponseEntity<T> sendSETRequestAndVerifyResponse(HttpMethod httpMethod, String url,
			String homePlatformId, String targetPlatformId, String body, String componentId,
			ParameterizedTypeReference<T> type) {

		Map<String, String> securityRequestHeaders;
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);

		// Insert Security Request into the headers
		try {

			Set<AuthorizationCredentials> authorizationCredentialsSet = new HashSet<>();
			Map<String, AAM> availableAAMs = securityHandler.getAvailableAAMs();

			log.info("Getting certificate for " + availableAAMs.get(homePlatformId).getAamInstanceId());
			securityHandler.getCertificate(availableAAMs.get(homePlatformId), username, password, clientId);

			log.info("Getting token from " + availableAAMs.get(homePlatformId).getAamInstanceId());
			Token homeToken = securityHandler.login(availableAAMs.get(homePlatformId));

			HomeCredentials homeCredentials = securityHandler.getAcquiredCredentials()
					.get(homePlatformId).homeCredentials;
			authorizationCredentialsSet
					.add(new AuthorizationCredentials(homeToken, homeCredentials.homeAAM, homeCredentials));

			SecurityRequest securityRequest = MutualAuthenticationHelper.getSecurityRequest(authorizationCredentialsSet,
					false);
			securityRequestHeaders = securityRequest.getSecurityRequestHeaderParams();

		} catch (SecurityHandlerException | ValidationException | JsonProcessingException
				| NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		for (Map.Entry<String, String> entry : securityRequestHeaders.entrySet()) {
			httpHeaders.add(entry.getKey(), entry.getValue());
		}
		log.info("request headers: " + httpHeaders);

		HttpEntity<String> httpEntity = new HttpEntity<>(body, httpHeaders);

		ResponseEntity<T> responseEntity = null;
		try {
			responseEntity = restTemplate.exchange(url, httpMethod, httpEntity, type);
		} catch (RestClientResponseException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		log.info("response = " + responseEntity);
		log.info("headers = " + responseEntity.getHeaders());
		log.info("body = " + responseEntity.getBody());

		String serviceResponse = responseEntity.getHeaders().get(SecurityConstants.SECURITY_RESPONSE_HEADER).get(0);

		if (serviceResponse == null)
			throw new RuntimeException("The receiver was not authenticated");

		boolean isServiceResponseVerified;
		try {
			isServiceResponseVerified = MutualAuthenticationHelper.isServiceResponseVerified(serviceResponse,
					securityHandler.getComponentCertificate(componentId, targetPlatformId));
		} catch (CertificateException | NoSuchAlgorithmException | SecurityHandlerException e) {
			throw new RuntimeException(e);
		}

		if (isServiceResponseVerified) {
			return responseEntity;
		} else {
			throw new RuntimeException("The service response is not verified");
		}
	}
}
