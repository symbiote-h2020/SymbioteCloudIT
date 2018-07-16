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
import java.util.List;

import static eu.h2020.symbiote.client.AbstractSymbIoTeClientFactory.*;

public class ClientWithGuestToken {

    public static void main(String[] args) {

        /*
        Get the factory and the component clients
         */

        // FILL ME
        String coreAddress = "https://symbiote-open.man.poznan.pl";
        String keystorePath = "testKeystore";
        String keystorePassword = "testKeystore";
        Type type = Type.FEIGN;

        // Get the configuration
        Config config = getGuestTokenConfiguration(coreAddress, keystorePath, keystorePassword, type);

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
        QueryResponse queryResponse = searchClient.searchAsGuest(coreQueryRequest, true);


        /*
        Ask CRAM for the specific resource url
         */

        // Here, we request the url of only the first resource contained in the Search response. We also validate the
        // CRAM response
        String resourceId = queryResponse.getResources().get(2).getId();
        ResourceUrlsResponse resourceUrlsResponse = cramClient.getResourceUrlAsGuest(resourceId, true);
        String resourceUrl = resourceUrlsResponse.getBody().get(resourceId);


        /*
        Get observations from RAP
         */

        // Here, we request the latest 10 observations from RAP
        List<Observation> observations = rapClient.getTopObservationsAsGuest(resourceUrl, 10, true);
        System.out.println(observations);
    }
}
