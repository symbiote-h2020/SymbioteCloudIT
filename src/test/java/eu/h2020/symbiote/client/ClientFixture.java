package eu.h2020.symbiote.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.core.ci.QueryResourceResult;
import eu.h2020.symbiote.core.ci.QueryResponse;
import eu.h2020.symbiote.model.cim.Actuator;
import eu.h2020.symbiote.model.cim.FeatureOfInterest;
import eu.h2020.symbiote.model.cim.LengthRestriction;
import eu.h2020.symbiote.model.cim.PrimitiveDatatype;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.model.cim.StationarySensor;
import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyType;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;

public class ClientFixture {

	@Autowired
	SymbioteClient client;
	
	@Autowired
	RestTemplate restTemplate;

	@Value("${test.platformId}")
	String platformId; 
	
	@Value("${test.directAAMUrl}")
	String directAAMUrl;

	@Value("${test.rhUrl}")
	String rhUrl;

	@Value("${test.iiUrl}")
	String iiUrl;
	
	@Value("${keystorePath}")
	String keystorePath;
	
	@Value("${keystorePassword}")
	String keystorePassword;

	@Value("${paamOwner.username}")
	String paamOwnerUsername;
	
	@Value("${paamOwner.password}")
	String paamOwnerPassword;
	
	@Value("${clientId}")
	String clientId;

	@Value("${symbIoTeCoreUrl}")
	String symbIoTeCoreUrl;
	
	String defaultResourceIdPrefix;
	
	protected void clearRegistrationHandler() throws Exception {
		try {
			syncResources();
		} catch (Exception e) {
			// this can be ignored because sync sometimes returns 400 first time it is called
		}
		syncResources();
		deleteAllResources();
	}

	protected CloudResource createSensorResource(String timeStamp, String internalId) {
	    CloudResource cloudResource = new CloudResource();
	    cloudResource.setInternalId(internalId);
	    cloudResource.setPluginId("platform_01");
	
	    try {
			cloudResource.setAccessPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
			cloudResource.setFilteringPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
		} catch (InvalidArgumentsException e) {
			e.printStackTrace();
		}
	    
	    StationarySensor sensor = new StationarySensor();
	    cloudResource.setResource(sensor);
	    sensor.setName(getSensorName(timeStamp + internalId));
	    sensor.setDescription(Arrays.asList("This is default sensor with timestamp: " + timeStamp + " and iid: " + internalId));
	
	    FeatureOfInterest featureOfInterest = new FeatureOfInterest();
	    sensor.setFeatureOfInterest(featureOfInterest);
	    featureOfInterest.setName("outside air");
	    featureOfInterest.setDescription(Arrays.asList("outside air quality"));
	    featureOfInterest.setHasProperty(Arrays.asList("temperature,humidity".split(",")));
	    
	    sensor.setObservesProperty(Arrays.asList("temperature,humidity".split(",")));
	    sensor.setLocatedAt(createLocation());
	    sensor.setInterworkingServiceURL(iiUrl);
	    return cloudResource;        
	}

	private WGS84Location createLocation() {
		WGS84Location location = new WGS84Location(52.513681, 13.363782, 15, 
	            "Berlin", 
	            Arrays.asList("Grosser Tiergarten"));
		return location;
	}

	protected CloudResource createActuatorResource(String timestamp, String internalId) {
	    CloudResource cloudResource = new CloudResource();
	    cloudResource.setInternalId(internalId);
	    cloudResource.setPluginId("platform_01");
	    
	    try {
			cloudResource.setAccessPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
			cloudResource.setFilteringPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
		} catch (InvalidArgumentsException e) {
			e.printStackTrace();
		}
	    
	    Actuator actuator = new Actuator();
	    cloudResource.setResource(actuator);
	    
	    actuator.setLocatedAt(createLocation());
	    actuator.setName(getActuatorName(timestamp + internalId));
	    actuator.setDescription(Arrays.asList("This default actuator with timestamp: " + timestamp + " and iid: " + internalId));
	    
	    eu.h2020.symbiote.model.cim.Capability capability = new eu.h2020.symbiote.model.cim.Capability();
	    actuator.setCapabilities(Arrays.asList(capability));
	    
	    capability.setName("OnOffCapabililty");
	
	    // parameters
	    eu.h2020.symbiote.model.cim.Parameter parameter = new eu.h2020.symbiote.model.cim.Parameter();
	    capability.setParameters(Arrays.asList(parameter));
	    parameter.setName("on");
	    parameter.setMandatory(true);
	    PrimitiveDatatype datatype = new PrimitiveDatatype();
		parameter.setDatatype(datatype);
		datatype.setBaseDatatype("boolean");
	    
	    actuator.setInterworkingServiceURL(iiUrl);
	
	    return cloudResource;
	}

	protected CloudResource createServiceResource(String timestamp, String internalId) {
	    CloudResource cloudResource = new CloudResource();
	    cloudResource.setInternalId(internalId);
	    cloudResource.setPluginId("platform_01");
	    
	    try {
			cloudResource.setAccessPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
			cloudResource.setFilteringPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
		} catch (InvalidArgumentsException e) {
			e.printStackTrace();
		}
	
	    Service service = new Service();
	    cloudResource.setResource(service);
	    
	    service.setName(getServiceName(timestamp + internalId));
	    service.setDescription(Arrays.asList("Defaut Service for testing with timestamp: " + timestamp + " and iid: " + internalId));
	    
	    eu.h2020.symbiote.model.cim.Parameter parameter = new eu.h2020.symbiote.model.cim.Parameter();
	    service.setParameters(Arrays.asList(parameter));
	
	    parameter.setName("inputParam1");
	    parameter.setMandatory(true);
	    // restriction
	    LengthRestriction restriction = new LengthRestriction();
	    restriction.setMin(2);
	    restriction.setMax(10);
		parameter.setRestrictions(Arrays.asList(restriction));
		
		PrimitiveDatatype datatype = new PrimitiveDatatype();
		datatype.setArray(false);
		datatype.setBaseDatatype("http://www.w3.org/2001/XMLSchema#string");
		parameter.setDatatype(datatype);
	
	    service.setInterworkingServiceURL(iiUrl);
	
	    return cloudResource;
	}

	protected ResponseEntity<ArrayList<CloudResource>> registerResources(List<CloudResource> resources) {
		// POST localhost:8001/resources
		// Headers: content-type: application/json
		// body array of cloud resources
		
	    HttpHeaders httpHeaders = new HttpHeaders();
	    httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
	    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
	
	    HttpEntity<List<CloudResource>> requestEntity = new HttpEntity<>(resources, httpHeaders);
		ParameterizedTypeReference<ArrayList<CloudResource>> type = new ParameterizedTypeReference<ArrayList<CloudResource>>() {};
		ResponseEntity<ArrayList<CloudResource>> responseEntity = restTemplate.exchange(rhUrl + "/resources", HttpMethod.POST, requestEntity, type);
	
		return responseEntity;
	}

	protected ResponseEntity<ArrayList<CloudResource>> getResources() {
		HttpEntity requestEntity = new HttpEntity<>(null);
		ParameterizedTypeReference<ArrayList<CloudResource>> type = new ParameterizedTypeReference<ArrayList<CloudResource>>() {};
		ResponseEntity<ArrayList<CloudResource>> responseEntity = restTemplate.exchange(rhUrl + "/resources", HttpMethod.GET, requestEntity, type);
	
		return responseEntity;
	}

	protected ResponseEntity<ArrayList<CloudResource>> deleteAllResources() {
		// DELETE localhost:8001/resources?resourceInternalIds=el_isen1,el_iaid1
		String ids = getResources().getBody().stream()
			.map(r -> r.getInternalId())
			.collect(Collectors.joining(","));
		
		HttpEntity requestEntity = new HttpEntity<>(null);
		ParameterizedTypeReference<ArrayList<CloudResource>> type = new ParameterizedTypeReference<ArrayList<CloudResource>>() {};
		ResponseEntity<ArrayList<CloudResource>> responseEntity = restTemplate.exchange(rhUrl + "/resources?resourceInternalIds=" + ids, HttpMethod.DELETE, requestEntity, type);
	
		return responseEntity;
		
	}

	protected ResponseEntity<ArrayList<CloudResource>> syncResources() {
		// PUT symbiotedoc.tel.fer.hr:8001/sync
		HttpEntity requestEntity = new HttpEntity<>(null);
		ParameterizedTypeReference<ArrayList<CloudResource>> type = new ParameterizedTypeReference<ArrayList<CloudResource>>() {};
		ResponseEntity<ArrayList<CloudResource>> responseEntity = restTemplate.exchange(rhUrl + "/sync", HttpMethod.PUT, requestEntity, type);
	
		return responseEntity;
	}

	protected LinkedList<CloudResource> createDefaultResources() {
		LinkedList<CloudResource> resources = createDefaultResourceWithIdPrefix("");
		return resources;
	}

	LinkedList<CloudResource> createDefaultResourceWithIdPrefix(String prefix) {
		if(!prefix.isEmpty())
			prefix = prefix + "-";
		LinkedList<CloudResource> resources = new LinkedList<>();
		resources.add(createSensorResource(prefix, "isen1"));
		resources.add(createActuatorResource(prefix, "iaid1"));
		resources.add(createServiceResource(prefix, "isrid1"));
		return resources;
	}

	protected ResponseEntity<ArrayList<CloudResource>> registerDefaultResources() {
		defaultResourceIdPrefix = String.valueOf(System.currentTimeMillis());
		LinkedList<CloudResource> resources = createDefaultResourceWithIdPrefix(defaultResourceIdPrefix);
		
		ResponseEntity<ArrayList<CloudResource>> responseEntity = registerResources(resources);
		return responseEntity;
	}

	String getSensorName(String internalId) {
		return "DefaultSensor" + internalId;
	}
	
	String getActuatorName(String internalId) {
		return "DefaultActuator" + internalId;
	}

	String getServiceName(String internalId) {
		return "DefaultService" + internalId;
	}

	String getDefaultSensorName() {
		return getSensorName(defaultResourceIdPrefix + "-isen1");
	}
	
	String getDefaultActuatorName() {
		return getActuatorName(defaultResourceIdPrefix + "-iaid1");
	}

	String getDefaultServiceName() {
		return getServiceName(defaultResourceIdPrefix + "-isrid1");
	}

	protected QueryResourceResult findDefaultSensor() {
		return searchResourceByName(getDefaultSensorName());
	}

	protected QueryResourceResult findDefaultActuator() {
		return searchResourceByName(getDefaultActuatorName());
	}

	protected QueryResourceResult findDefaultService() {
		return searchResourceByName(getDefaultServiceName());
	}

	private QueryResourceResult searchResourceByName(String name) {
	    ResponseEntity<QueryResponse> query = client.query(platformId, // platformId, 
	    		null, // platformName, 
	    		null, // owner, 
	    		name, // name, 
	    		null, // id, 
	    		null, // description, 
	    		null, // location_name, 
	    		null, // location_lat, 
	    		null, // location_long, 
	    		null, // max_distance, 
	    		null, // observed_property, 
	    		null, // observed_property_iri, 
	    		null, // resource_type, 
	    		null, // should_rank, 
	    		platformId  // homePlatformId - can not be null
	    );
		return query.getBody().getBody().get(0);
	}
}

