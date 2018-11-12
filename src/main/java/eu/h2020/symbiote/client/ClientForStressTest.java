package eu.h2020.symbiote.client;

import eu.h2020.symbiote.client.interfaces.RHClient;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.model.cim.FeatureOfInterest;
import eu.h2020.symbiote.model.cim.StationarySensor;
import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static eu.h2020.symbiote.client.AbstractSymbIoTeClientFactory.*;

public class ClientForStressTest {

    private static AbstractSymbIoTeClientFactory factory;

    private static Log log = LogFactory.getLog(ClientForStressTest.class);

    public static void main(String[] args) {

        /*
        Get the factory and the client
         */

        // FILL ME
        // mandatory to run
        String coreAddress = "https://symbiote-dev.man.poznan.pl";
        String keystorePath = "testKeystore";
        String keystorePassword = "testKeystore";
        String exampleHomePlatformIdentifier = "icom-platform";

        Type type = Type.FEIGN;

        // Get the configuration
        Config config = new Config(coreAddress, keystorePath, keystorePassword, type);

        // Get the factory
     //   AbstractSymbIoTeClientFactory factory;
        try {
            factory = getFactory(config);
        } catch (SecurityHandlerException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }

        deleteAllResources(Layer.L1, exampleHomePlatformIdentifier);

        sendRequestAndVerifyResponseRHStress(exampleHomePlatformIdentifier, 10);


    }

    public static AbstractSymbIoTeClientFactory getClientFactory() {
        return factory;
    }



    public static ResponseEntity<?> sendRequestAndVerifyResponseRHStress(String homePlatformId, Integer stress) {

        String directoryName = "./output";
        String fileName = directoryName+"/log"+ String.valueOf(System.currentTimeMillis());

        // Start from here
        List<Callable<QueryHttpResult>> tasks = new ArrayList<>();

        AbstractSymbIoTeClientFactory factory = getClientFactory();

        //populate tasks list
        for( int i = 0; i < stress.intValue(); i++ ) {
            tasks.add(new RHQueryCallable("Runner"+i, homePlatformId, factory));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(stress.intValue());

        long in = System.currentTimeMillis();
        try {

            File directory = new File(directoryName);
            if(!directory.exists())
                directory.mkdir();
            File file = new File(fileName);
            PrintWriter outputFile = new PrintWriter(file);

            // This is the actual test
            List<Future<QueryHttpResult>> futures = executorService.invokeAll(tasks);

            List<QueryHttpResult> resultList = new ArrayList<>(futures.size());

            // Check for exceptions
            for (Future<QueryHttpResult> future : futures) {
                // Throws an exception if an exception was thrown by the task.
                resultList.add(future.get());
            }

            long out = System.currentTimeMillis();

            //prepare results
            OptionalLong maxTimer = resultList.stream().mapToLong(qRes -> qRes.getExecutionTime()).max();
            OptionalLong minTimer = resultList.stream().mapToLong(qRes -> qRes.getExecutionTime()).min();
            OptionalDouble avgTimer = resultList.stream().mapToLong(qRes -> qRes.getExecutionTime()).average();

            resultList.stream().forEach(s -> {
                    log.debug( "["+ s.getName() + "] finished in " + s.getExecutionTime() + " ms ");
                    outputFile.println( "["+ s.getName() + "] " + s.getExecutionTime() + " ms ");
        }
            );

            log.debug("All tasks finished in " + ( out - in ) + " ms | min " + minTimer.orElse(-1l) + " | max "
                    + maxTimer.orElse(-1l) + " | avg " + avgTimer.orElse( -1.0) );


            outputFile.println("All " + ( out - in ) + " ms \nmin " + minTimer.orElse(-1l) + " ms \nmax "
                    + maxTimer.orElse(-1l) + " ms \navg " + avgTimer.orElse( -1.0) + " ms");
            outputFile.close();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return new ResponseEntity<Object>("", HttpStatus.OK);
    }

    private static class RHQueryCallable implements Callable<QueryHttpResult> {

        private final String name;
        private final AbstractSymbIoTeClientFactory factory;
        private final String homePlatformId;


        public RHQueryCallable(String name, String homePlatformId, AbstractSymbIoTeClientFactory factory) {
            this.name = name;
            this.factory =  factory;
            this.homePlatformId = homePlatformId;
        }

        @Override
        public QueryHttpResult call() throws Exception {
            log.debug("["+this.name+"] starting");

            RHClient rhClient = factory.getRHClient(homePlatformId);

            CloudResource cloudResource = new CloudResource();
            cloudResource.setInternalId(this.name);

            //TODO randomize resource here
            StationarySensor sensor = new StationarySensor();
            cloudResource.setResource(sensor);
            Long timeStamp = System.currentTimeMillis();
            String internalId = cloudResource.getInternalId();
            sensor.setName(timeStamp + internalId);
            sensor.setDescription(Collections.singletonList("This is default sensor with timestamp: " + timeStamp + " and iid: " + internalId));

            FeatureOfInterest featureOfInterest = new FeatureOfInterest();
            sensor.setFeatureOfInterest(featureOfInterest);
            featureOfInterest.setName("outside air");
            featureOfInterest.setDescription(Collections.singletonList("outside air quality"));
            featureOfInterest.setHasProperty(Arrays.asList("temperature,humidity".split(",")));

            sensor.setObservesProperty(Arrays.asList("temperature,humidity".split(",")));
            sensor.setLocatedAt(new WGS84Location(52.513681, 13.363782, 15,
                    "Berlin", Collections.singletonList("Grosser Tiergarten")));
            sensor.setInterworkingServiceURL("https://intracom.symbiote-h2020.eu");
            /////

            long in = System.currentTimeMillis();

            CloudResource returnedResource = rhClient.addL1Resource(cloudResource);

            long executionTime = (System.currentTimeMillis() - in );

            ResponseEntity<CloudResource> responseEntity = new ResponseEntity(returnedResource, HttpStatus.OK);;
            if (returnedResource==null)
                responseEntity= new ResponseEntity(HttpStatus.NO_CONTENT);

            log.debug("["+this.name+"] finished with status " + responseEntity.getStatusCode() + " in "
                    + executionTime + " ms" );

            return new QueryHttpResult(this.name,responseEntity,executionTime);
        }

    }

    private static class QueryHttpResult {

        private String name;
        private final ResponseEntity responseEntity;
        private final long executionTime;

        public QueryHttpResult(String name, ResponseEntity responseEntity, long executionTime ) {
            this.name = name;
            this.responseEntity = responseEntity;
            this.executionTime = executionTime;
        }

        public ResponseEntity getResponseEntity() {
            return responseEntity;
        }

        public long getExecutionTime() {
            return executionTime;
        }

        public String getName() {
            return name;
        }
    }


    private static ResponseEntity deleteAllResources(Layer layer, String platformId) {
        // DELETE localhost:8001/resources?resourceInternalIds=el_isen1,el_iaid1

        HttpEntity requestEntity = new HttpEntity<>(null);

        RHClient rhClient = factory.getRHClient(platformId);

        if (layer == Layer.L1) {

           ResponseEntity<List<CloudResource>> resources = new ResponseEntity<>(rhClient.getResources(), HttpStatus.OK);

            List<String> ids = resources.getBody().stream()
                    .filter(cloudResource -> cloudResource.getResource().getId() != null)
                    .map(CloudResource::getInternalId)
                    .collect(Collectors.toList());

            if (ids.isEmpty())
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);

            return new ResponseEntity(rhClient.deleteL1Resources(ids), HttpStatus.OK);
        } else {

            ResponseEntity<List<CloudResource>> resources = new ResponseEntity<>(rhClient.getResources(), HttpStatus.OK);

            List<String> ids = resources.getBody().stream()
                    .filter(cloudResource -> cloudResource.getFederationInfo() != null)
                    .map(CloudResource::getInternalId)
                    .collect(Collectors.toList());

            if (ids.isEmpty())
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);

            return new ResponseEntity(rhClient.removeL2Resources(ids), HttpStatus.OK);
        }


    }

    public enum Layer {
        L1, L2
    }
}
