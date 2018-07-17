package eu.h2020.symbiote.client;

import eu.h2020.symbiote.client.interfaces.CRAMClient;
import eu.h2020.symbiote.client.interfaces.RAPClient;
import eu.h2020.symbiote.client.interfaces.SearchClient;
import eu.h2020.symbiote.core.ci.QueryResponse;
import eu.h2020.symbiote.core.internal.CoreQueryRequest;
import eu.h2020.symbiote.core.internal.cram.ResourceUrlsResponse;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;

import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static eu.h2020.symbiote.client.AbstractSymbIoTeClientFactory.*;

public class ClientWithHomeToken {

    public static void main(String[] args) {

        /*
        Get the factory and the component clients
         */

        // FILL ME
        String coreAddress = "https://symbiote-open.man.poznan.pl";
        String keystorePath = "testKeystore";
        String keystorePassword = "testKeystore";
        String homePlatformId = "homePlatformId";
        String username = "userNameInHomePlatform";
        String password = "passwordInHomePlatform";
        String clientId = "exampleClientId";
        Type type = Type.FEIGN;

        // Get the configuration
        Config config = new Config(coreAddress, keystorePath, keystorePassword, type);

        // Get the factory
        AbstractSymbIoTeClientFactory factory;
        try {
            factory = getFactory(config);

            // ATTENTION: This should be preferably an interactive procedure to avoid persisting credentials
            // Here, you can add credentials FOR MORE THAN 1 platforms
            Set<HomePlatformCredentials> platformCredentials = new HashSet<>();
            HomePlatformCredentials exampleHomePlatformCredentials = new HomePlatformCredentials(homePlatformId, username, password, clientId);
            platformCredentials.add(exampleHomePlatformCredentials);

            // Get Certificates for the specified platforms
            factory.initializeInHomePlatforms(platformCredentials);
        } catch (SecurityHandlerException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }

        // Get the necessary component clients
        SearchClient searchClient = factory.getSearchClient();
        CRAMClient cramClient = factory.getCramClient();
        RAPClient rapClient = factory.getRapClient();

        // The set of platforms from which we are going to request credentials for our requests
        Set<String> platformIds = new HashSet<>(Collections.singletonList(homePlatformId));

        /*
        Search for resources in Core
         */

        // Create the request
        // Here, we specify just one search criteria, which is the platform id. You can add more criteria, such as
        // platform name, location, etc. You can check what the CoreQueryRequest.Builder supports
        CoreQueryRequest coreQueryRequest = new CoreQueryRequest.Builder()
                .platformId("fer1")
                .build();

        // Send the request and validate the Search response
        QueryResponse queryResponse = searchClient.search(coreQueryRequest, true, platformIds);


        /*
        Ask CRAM for the specific resource url
         */

        // Here, we request the url of only the first resource contained in the Search response. We also validate the
        // CRAM response
        String resourceId = queryResponse.getResources().get(0).getId();
        ResourceUrlsResponse resourceUrlsResponse = cramClient.getResourceUrl(resourceId, true, platformIds);
        String resourceUrl = resourceUrlsResponse.getBody().get(resourceId);


        /*
        Get observations from RAP
         */

        // Here, we just request the latest observation from RAP
        Observation observation = rapClient.getLatestObservation(resourceUrl, true, platformIds);
        System.out.println(observation);
    }
}
