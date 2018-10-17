package eu.h2020.symbiote.client;

import eu.h2020.symbiote.client.interfaces.*;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.cloud.model.internal.FederationSearchResult;
import eu.h2020.symbiote.cloud.model.internal.PlatformRegistryQuery;
import eu.h2020.symbiote.core.ci.QueryResourceResult;
import eu.h2020.symbiote.core.ci.QueryResponse;
import eu.h2020.symbiote.core.internal.CoreQueryRequest;
import eu.h2020.symbiote.model.cim.*;
import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyType;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.communication.IAAMClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

public class ClientFixture {

	@Autowired
	protected RestTemplate restTemplate;

	@Autowired
	protected RHClient rhClient;

    @Autowired
    protected SearchClient searchClient;

    @Autowired
    protected CRAMClient cramClient;

    @Autowired
    protected RAPClient rapClient;

    @Autowired
    protected PRClient prClient;

	@Autowired
	protected SMClient smClient;

    @Autowired
    protected IAAMClient iaamClient;

    @Autowired
    @Qualifier("homePlatformIds")
    protected Set<String> homePlatformIds;

	@Value("${test.platformId}")
	protected String platformId;
	
	@Value("${test.directAAMUrl}")
	protected String directAAMUrl;

	@Value("${test.rhUrl}")
    protected String rhUrl;

	@Value("${test.iiUrl}")
    protected String iiUrl;
	
	@Value("${keystorePath}")
    protected String keystorePath;
	
	@Value("${keystorePassword}")
    protected String keystorePassword;

	@Value("${paamOwner.username}")
    protected String paamOwnerUsername;
	
	@Value("${paamOwner.password}")
    protected String paamOwnerPassword;
	
	@Value("${clientId}")
    protected String clientId;

	@Value("${symbIoTeCoreUrl}")
    protected String symbIoTeCoreUrl;

    @Value("${demoApp.username}")
    protected String username;

    @Value("${demoApp.password}")
    protected String password;

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
	    
	    Capability capability = new Capability();
	    actuator.setCapabilities(Collections.singletonList(capability));
	    
	    capability.setName("OnOffCapabililty");
	
	    // parameters
	    Parameter parameter = new Parameter();
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
	    //service.setDescription(Collections.singletonList("Defaut Service for testing with timestamp: " + timestamp + " and iid: " + internalId));
		List<String> descriptionList=Arrays.asList("@type=Beacon","@beacon.id=f7826da6-4fa2-4e98-8024-bc5b71e0893e","@beacon.major=44933","@beacon.minor=46799","@beacon.tx=0x50");
		service.setDescription(descriptionList);


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

    protected ResponseEntity<List<CloudResource>> getResources() {
        return new ResponseEntity<>(rhClient.getResources(), HttpStatus.OK);
    }

    protected ResponseEntity<List<CloudResource>> registerL1Resources(List<CloudResource> resources) {
	    return new ResponseEntity<>(rhClient.addL1Resources(resources), HttpStatus.OK);
    }

    protected ResponseEntity<List<CloudResource>> registerL2Resources(List<CloudResource> resources) {
        return new ResponseEntity<>(rhClient.addL2Resources(resources), HttpStatus.OK);
    }


    protected ResponseEntity<List<CloudResource>> deleteAllL1Resources() {
        return deleteAllResources(Layer.L1);
    }

    protected ResponseEntity deleteAllL2Resources() {
        return deleteAllResources(Layer.L2);
    }

    protected ResponseEntity<List<CloudResource>> registerDefaultL1Resources() {
        return registerDefaultResources(Layer.L1);
    }

    protected ResponseEntity<List<CloudResource>> registerDefaultL2Resources() {
        return registerDefaultResources(Layer.L2);
    }

    protected ResponseEntity<List<CloudResource>> syncResources() {
        // PUT symbiotedoc.tel.fer.hr:8001/sync
        return new ResponseEntity<>(rhClient.sync(), HttpStatus.OK);
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

    protected ResponseEntity<List<CloudResource>> registerResources(List<CloudResource> resources, Layer layer) {
        // POST localhost:8001/resources
        // Headers: content-type: application/json
        // body array of cloud resources

        if (layer == Layer.L1)
            return new ResponseEntity<>(rhClient.addL1Resources(resources), HttpStatus.OK);
        else
            return new ResponseEntity<>(rhClient.addL2Resources(resources), HttpStatus.OK);
    }


	public ResponseEntity<?> shareResources(Map<String, Map<String, Boolean>> sharingMap) {

		return new ResponseEntity<>(rhClient.shareL2Resources(sharingMap), HttpStatus.OK);
	}


    public ResponseEntity<?> unshareResources( Map<String, List<String>> unshareMap) {

        return new ResponseEntity<>(rhClient.unshareL2Resources(unshareMap), HttpStatus.OK);
    }

	private ResponseEntity deleteAllResources(Layer layer) {
		// DELETE localhost:8001/resources?resourceInternalIds=el_isen1,el_iaid1

		HttpEntity requestEntity = new HttpEntity<>(null);

		if (layer == Layer.L1) {
			List<String> ids = getResources().getBody().stream()
					.filter(cloudResource -> cloudResource.getResource().getId() != null)
					.map(CloudResource::getInternalId)
					.collect(Collectors.toList());

			if (ids.isEmpty())
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);

            return new ResponseEntity(rhClient.deleteL1Resources(ids), HttpStatus.OK);
        } else {

            List<String> ids = getResources().getBody().stream()
					.filter(cloudResource -> cloudResource.getFederationInfo() != null)
					.map(CloudResource::getInternalId)
					.collect(Collectors.toList());

			if (ids.isEmpty())
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);

            return new ResponseEntity(rhClient.removeL2Resources(ids), HttpStatus.OK);
        }
	}

    private ResponseEntity<List<CloudResource>> registerDefaultResources(Layer layer) {
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

	protected ResponseEntity<QueryResponse>  searchL1Resources(CoreQueryRequest request) {
        return new ResponseEntity<>(searchClient.search(request, true, homePlatformIds), HttpStatus.OK);
	}

	protected ResponseEntity<FederationSearchResult> searchL2Resources(PlatformRegistryQuery query) {

		return new ResponseEntity<>(prClient.search(query, true, homePlatformIds), HttpStatus.OK);
	}

	private QueryResourceResult searchResourceByName(String name) {
	    CoreQueryRequest request = new CoreQueryRequest.Builder().name(name).platformId(platformId).build();
	    QueryResponse query = searchClient.search(request, true, homePlatformIds);

	    // If it is 0, ask one more time
	    if (query.getResources().size() == 0)
	        query = searchClient.search(request, true, homePlatformIds);

		return query.getResources().get(0);
	}

	public enum Layer {
	    L1, L2
    }
}

