package eu.h2020.symbiote.client;

import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.core.ci.QueryResourceResult;
import eu.h2020.symbiote.core.ci.QueryResponse;
import eu.h2020.symbiote.model.cim.*;
import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyType;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ClientFixture {

	@Autowired
	protected SymbioteClient client;
	
	@Autowired
	protected RestTemplate restTemplate;

	@Value("${test.platformId}")
	protected String platformId;
	
	@Value("${test.directAAMUrl}")
	protected String directAAMUrl;

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
	
	protected String defaultResourceIdPrefix;
	
	protected void clearRegistrationHandlerL1() {
//		try {
//			syncResources();
//		} catch (Exception e) {
//			// this can be ignored because sync sometimes returns 400 first time it is called
//		}
//
//		try {
//			syncResources();
//		} catch (Exception e) {
//			// this can be ignored because sync sometimes returns 400 first time it is called
//		}

        deleteAllL1Resources();
	}

    protected void clearRegistrationHandlerL2() {
//		try {
//			syncResources();
//		} catch (Exception e) {
//			// this can be ignored because sync sometimes returns 400 first time it is called
//		}
//
//		try {
//			syncResources();
//		} catch (Exception e) {
//			// this can be ignored because sync sometimes returns 400 first time it is called
//		}

        deleteAllL2Resources();
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
	    sensor.setDescription(Collections.singletonList("This is default sensor with timestamp: " + timeStamp + " and iid: " + internalId));
	
	    FeatureOfInterest featureOfInterest = new FeatureOfInterest();
	    sensor.setFeatureOfInterest(featureOfInterest);
	    featureOfInterest.setName("outside air");
	    featureOfInterest.setDescription(Collections.singletonList("outside air quality"));
	    featureOfInterest.setHasProperty(Arrays.asList("temperature,humidity".split(",")));
	    
	    sensor.setObservesProperty(Arrays.asList("temperature,humidity".split(",")));
	    sensor.setLocatedAt(createLocation());
	    sensor.setInterworkingServiceURL(iiUrl);
	    return cloudResource;        
	}

	private WGS84Location createLocation() {
		return new WGS84Location(52.513681, 13.363782, 15,
	            "Berlin",
                Collections.singletonList("Grosser Tiergarten"));
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
	    actuator.setDescription(Collections.singletonList("This default actuator with timestamp: " + timestamp + " and iid: " + internalId));
	    
	    eu.h2020.symbiote.model.cim.Capability capability = new eu.h2020.symbiote.model.cim.Capability();
	    actuator.setCapabilities(Collections.singletonList(capability));
	    
	    capability.setName("OnOffCapabililty");
	
	    // parameters
	    eu.h2020.symbiote.model.cim.Parameter parameter = new eu.h2020.symbiote.model.cim.Parameter();
	    capability.setParameters(Collections.singletonList(parameter));
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
	    service.setDescription(Collections.singletonList("Defaut Service for testing with timestamp: " + timestamp + " and iid: " + internalId));
	    
	    eu.h2020.symbiote.model.cim.Parameter parameter = new eu.h2020.symbiote.model.cim.Parameter();
	    service.setParameters(Collections.singletonList(parameter));
	
	    parameter.setName("inputParam1");
	    parameter.setMandatory(true);
	    // restriction
	    LengthRestriction restriction = new LengthRestriction();
	    restriction.setMin(2);
	    restriction.setMax(10);
		parameter.setRestrictions(Collections.singletonList(restriction));
		
		PrimitiveDatatype datatype = new PrimitiveDatatype();
		datatype.setArray(false);
		datatype.setBaseDatatype("http://www.w3.org/2001/XMLSchema#string");
		parameter.setDatatype(datatype);
	
	    service.setInterworkingServiceURL(iiUrl);
	
	    return cloudResource;
	}

    protected ResponseEntity<ArrayList<CloudResource>> getResources() {
        HttpEntity requestEntity = new HttpEntity<>(null);
        ParameterizedTypeReference<ArrayList<CloudResource>> type = new ParameterizedTypeReference<ArrayList<CloudResource>>() {};
        return restTemplate.exchange(
                rhUrl + "/resources", HttpMethod.GET, requestEntity, type);
    }

    protected ResponseEntity<ArrayList<CloudResource>> registerL1Resources(List<CloudResource> resources) {
	    return registerResources(resources, Layer.L1);
    }

    protected ResponseEntity<ArrayList<CloudResource>> registerL2Resources(List<CloudResource> resources) {
        return registerResources(resources, Layer.L2);
    }


    protected ResponseEntity<ArrayList<CloudResource>> deleteAllL1Resources() {
        return deleteAllResources(Layer.L1);
    }

    protected ResponseEntity deleteAllL2Resources() {
        return deleteAllResources(Layer.L2);
    }

    protected ResponseEntity<ArrayList<CloudResource>> registerDefaultL1Resources() {
        return registerDefaultResources(Layer.L1);
    }

    protected ResponseEntity<ArrayList<CloudResource>> registerDefaultL2Resources() {
        return registerDefaultResources(Layer.L2);
    }

    protected ResponseEntity<ArrayList<CloudResource>> syncResources() {
        // PUT symbiotedoc.tel.fer.hr:8001/sync
        HttpEntity requestEntity = new HttpEntity<>(null);
        ParameterizedTypeReference<ArrayList<CloudResource>> type = new ParameterizedTypeReference<ArrayList<CloudResource>>() {};
        return restTemplate.exchange(rhUrl + "/sync", HttpMethod.PUT, requestEntity, type);
    }

    protected LinkedList<CloudResource> createDefaultResources() {
        return createDefaultResourceWithIdPrefix("");
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

    protected String getDefaultSensorName() {
        return getSensorName(defaultResourceIdPrefix + "-isen1");
    }

    protected String getDefaultActuatorName() {
        return getActuatorName(defaultResourceIdPrefix + "-iaid1");
    }

    protected String getDefaultServiceName() {
        return getServiceName(defaultResourceIdPrefix + "-isrid1");
    }

    private ResponseEntity<ArrayList<CloudResource>> registerResources(List<CloudResource> resources, Layer layer) {
        // POST localhost:8001/resources
        // Headers: content-type: application/json
        // body array of cloud resources

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<CloudResource>> requestEntity = new HttpEntity<>(resources, httpHeaders);
        ParameterizedTypeReference<ArrayList<CloudResource>> type = new ParameterizedTypeReference<ArrayList<CloudResource>>() {};
        return restTemplate.exchange(rhUrl + (layer == Layer.L2 ? "/local" : "") + "/resources", HttpMethod.POST, requestEntity, type);
    }


	public ResponseEntity<?> shareResources(Map<String, Map<String, Boolean>> sharingMap) {

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<Map<String, Map<String, Boolean>>> requestEntity = new HttpEntity<>(sharingMap, httpHeaders);
		ParameterizedTypeReference<Map<String, List<CloudResource>>> type = new ParameterizedTypeReference<Map<String, List<CloudResource>>>() {};
		return restTemplate.exchange(rhUrl + "/local/resources/share", HttpMethod.PUT, requestEntity, type);
	}


    public ResponseEntity<?> unshareResources( Map<String, List<String>> unshareMap) {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, List<String>>> requestEntity = new HttpEntity<>(unshareMap, httpHeaders);
        ParameterizedTypeReference<Map<String, List<CloudResource>>> type = new ParameterizedTypeReference<Map<String, List<CloudResource>>>() {};
        return restTemplate.exchange(rhUrl + "/local/resources/share", HttpMethod.DELETE, requestEntity, type);
    }

	private ResponseEntity deleteAllResources(Layer layer) {
		// DELETE localhost:8001/resources?resourceInternalIds=el_isen1,el_iaid1

		HttpEntity requestEntity = new HttpEntity<>(null);

		if (layer == Layer.L1) {
			String ids = getResources().getBody().stream()
					.filter(cloudResource -> cloudResource.getResource().getId() != null)
					.map(CloudResource::getInternalId)
					.collect(Collectors.joining(","));

			if (ids.isEmpty())
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);

            ParameterizedTypeReference<ArrayList<CloudResource>> type = new ParameterizedTypeReference<ArrayList<CloudResource>>() {
            };

            return restTemplate.exchange(
                    rhUrl + "/resources?resourceInternalIds=" + ids,
                    HttpMethod.DELETE, requestEntity, type);
        } else {

			String ids = getResources().getBody().stream()
					.filter(cloudResource -> cloudResource.getFederationInfo() != null)
					.map(CloudResource::getInternalId)
					.collect(Collectors.joining(","));

			if (ids.isEmpty())
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);


            ParameterizedTypeReference<ArrayList<String>> type = new ParameterizedTypeReference<ArrayList<String>>() {
            };

            return restTemplate.exchange(
                    rhUrl + "/local/resources?resourceIds=" + ids,
                    HttpMethod.DELETE, requestEntity, type);
        }
	}

    private ResponseEntity<ArrayList<CloudResource>> registerDefaultResources(Layer layer) {
        defaultResourceIdPrefix = String.valueOf(System.currentTimeMillis());
        LinkedList<CloudResource> resources = createDefaultResourceWithIdPrefix(defaultResourceIdPrefix);

        return registerResources(resources, layer);
    }

	private LinkedList<CloudResource> createDefaultResourceWithIdPrefix(String prefix) {
		if(!prefix.isEmpty())
			prefix = prefix + "-";
		LinkedList<CloudResource> resources = new LinkedList<>();
		resources.add(createSensorResource(prefix, "isen1"));
		resources.add(createActuatorResource(prefix, "iaid1"));
		resources.add(createServiceResource(prefix, "isrid1"));
		return resources;
	}

	private String getSensorName(String internalId) {
		return "DefaultSensor" + internalId;
	}

    private String getActuatorName(String internalId) {
		return "DefaultActuator" + internalId;
	}

    private String getServiceName(String internalId) {
		return "DefaultService" + internalId;
	}

	protected ResponseEntity<QueryResponse>  searchL1Resources(String platformId,
															   String platformName,
															   String owner,
															   String name,
															   String id,
															   String description,
															   String location_name,
															   Double location_lat,
															   Double location_long,
															   Integer max_distance,
															   String[] observed_property,
															   String[] observed_property_iri,
															   String resource_type,
															   Boolean should_rank,
															   String homePlatformId) {
		return searchResources(platformId,
				platformName,
				owner,
				name,
				id,
				description,
				location_name,
				location_lat,
				location_long,
				max_distance,
				observed_property,
				observed_property_iri,
				resource_type,
				should_rank,
				homePlatformId,
				null,
				Layer.L1);

	}

	protected ResponseEntity searchL2Resources(String platformId, String predicate) {

		return searchResources(platformId,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				predicate,//L2 additional arguments
				Layer.L2);
	}

	public ResponseEntity searchResources(String platformId,
														 String platformName,
														 String owner,
														 String name,
														 String id,
														 String description,
														 String location_name,
														 Double location_lat,
														 Double location_long,
														 Integer max_distance,
														 String[] observed_property,
														 String[] observed_property_iri,
														 String resource_type,
														 Boolean should_rank,
														 String homePlatformId,
														 String predicate,
														 Layer layer
														 ) {

		if (layer.equals(Layer.L1)) {
			return client.query(platformId,
					platformName,
					owner,
					name,
					id,
					description,
					location_name,
					location_lat,
					location_long,
					max_distance,
					observed_property,
					observed_property_iri,
					resource_type,
					should_rank,
					homePlatformId);
		}
		else //L2 level
			return client.queryL2(platformId,predicate);
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

	    // If it is 0, ask one more time
	    if (query.getBody().getResources().size() == 0)
	        query = client.query(platformId, // platformId,
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
		return query.getBody().getResources().get(0);
	}

	public enum Layer {
	    L1, L2;
    }
}

