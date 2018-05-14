package eu.h2020.symbiote.client.interfaces;

import eu.h2020.symbiote.core.cci.RDFResourceRegistryRequest;
import eu.h2020.symbiote.core.cci.ResourceRegistryRequest;
import eu.h2020.symbiote.core.cci.ResourceRegistryResponse;
import eu.h2020.symbiote.core.internal.ClearDataResponse;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

import org.springframework.web.bind.annotation.RequestBody;

public interface InterworkingInterfaceService {
    public static final String PLATFORM_ID = "platformId";
    public static final String DO_CREATE_RESOURCES = "/platforms/{platformId}/resources";
    public static final String DO_CREATE_RDF_RESOURCES = "/platforms/{platformId}/rdfResources";
    public static final String DO_UPDATE_RESOURCES = "/platforms/{platformId}/resources";
    public static final String DO_REMOVE_RESOURCES = "/platforms/{platformId}/resources";
    
    public static final String DO_CLEAR_DATA = "/platforms/{platformId}/clearData";

    public static final String RESOURCE_COLLECTION = "cloudResource";
    public static final String DO_SHARE_RESOURCES = "/sharing";

	
	
	@RequestLine("POST " + DO_CREATE_RESOURCES)
	@Headers({ "Accept: application/json", "Content-Type: application/json" })
	ResourceRegistryResponse createResources(@Param(PLATFORM_ID) String platformId,
			@RequestBody ResourceRegistryRequest resources);

	@RequestLine("POST " + DO_CREATE_RDF_RESOURCES)
	@Headers({ "Accept: application/json", "Content-Type: application/json" })
	ResourceRegistryResponse createRdfResources(@Param(PLATFORM_ID) String platformId,
			@RequestBody RDFResourceRegistryRequest resources);

	@RequestLine("PUT " + DO_UPDATE_RESOURCES)
	@Headers({ "Accept: application/json", "Content-Type: application/json" })
	ResourceRegistryResponse updateResource(@Param(PLATFORM_ID) String platformId,
			@RequestBody ResourceRegistryRequest resources);

	@RequestLine("DELETE " + DO_REMOVE_RESOURCES)
	@Headers({ "Accept: application/json", "Content-Type: application/json" })
	ResourceRegistryResponse removeResources(@Param(PLATFORM_ID) String platformId,
			@RequestBody ResourceRegistryRequest resources);

	@RequestLine("POST " + DO_CLEAR_DATA)
	@Headers({ "Accept: application/json", "Content-Type: application/json" })
	ClearDataResponse clearData(@Param(PLATFORM_ID) String platformId);

}
