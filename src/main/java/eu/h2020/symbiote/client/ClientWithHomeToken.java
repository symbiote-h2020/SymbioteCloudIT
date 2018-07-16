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
        Config config = getHomeTokenConfiguration(
                coreAddress, keystorePath, keystorePassword, homePlatformId, username, password, clientId, type);

        // Get the factory
        AbstractSymbIoTeClientFactory factory;
        try {
            factory = getFactory(config);
        } catch (SecurityHandlerException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }

        // Get the necessary component clients
        SearchClient searchClient = factory.getSearchClient();
        CRAMClient cramClient = factory.getCramClient();
        RAPClient rapClient = factory.getRapClient();


        /*
        Search for resources in Core
         */

        // Create the request
        CoreQueryRequest coreQueryRequest = new CoreQueryRequest.Builder()
                .platformId("fer1")
                .build();

        // Send the request and validate the Search response
        QueryResponse queryResponse = searchClient.search(coreQueryRequest, true);


        /*
        Ask CRAM for the specific resource url
         */

        // Here, we request the url of only the first resource contained in the Search response. We also validate the
        // CRAM response
        String resourceId = queryResponse.getResources().get(0).getId();
        ResourceUrlsResponse resourceUrlsResponse = cramClient.getResourceUrl(resourceId, true);
        String resourceUrl = resourceUrlsResponse.getBody().get(resourceId);


        /*
        Get observations from RAP
         */

        // Here, we just request the latest observation from RAP
        Observation observation = rapClient.getLatestObservation(resourceUrl, true);
        System.out.println(observation);
    }
}
