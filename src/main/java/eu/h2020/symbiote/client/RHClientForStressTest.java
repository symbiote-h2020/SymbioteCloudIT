package eu.h2020.symbiote.client;

import eu.h2020.symbiote.client.interfaces.RHClient;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.model.cim.*;
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

public class RHClientForStressTest {

    private static AbstractSymbIoTeClientFactory factory;

    private static Log log = LogFactory.getLog(RHClientForStressTest.class);

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




        //set parameters for the stress test
        int runsNumber=10;//number of execution runs
        int stress = 100;//number of resources to access

        int run=0;
        //register and access resources periodically
        while(run<runsNumber) {
            sendRequestAndVerifyResponseRHStress(exampleHomePlatformIdentifier, run, stress);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            run++;
        }




    }

    public static AbstractSymbIoTeClientFactory getClientFactory() {
        return factory;
    }



    public static ResponseEntity<?> sendRequestAndVerifyResponseRHStress(String homePlatformId, Integer run, Integer stress) {


        // Start from here
        List<Callable<QueryHttpResult>> tasks = new ArrayList<>();

        AbstractSymbIoTeClientFactory factory = getClientFactory();

        //populate tasks list
        for( int i = 0; i < stress; i++ ) {
            tasks.add(new RHQueryCallable("Runner"+run+i, homePlatformId, factory));//name is the internalId to be used
        }

        ExecutorService executorService = Executors.newFixedThreadPool(stress.intValue());


        try {


            String directoryName = "./output";
            String fileName = directoryName+"/log"+ String.valueOf(System.currentTimeMillis());

            File directory = new File(directoryName);
            if(!directory.exists())
                directory.mkdir();
            File file = new File(fileName);
            PrintWriter outputFile = new PrintWriter(file);


            long in = System.currentTimeMillis();

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


            outputFile.println("Timestamp " + in + " Number of requests " + stress + " All " + ( out - in ) + " ms min " + minTimer.orElse(-1l) + " ms max "
                    + maxTimer.orElse(-1l) + " ms avg " + avgTimer.orElse( -1.0) + " ms");
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

            //resource fields randomized
            Resource resource = new Resource();
            cloudResource.setResource(resource);
            Long timeStamp = System.currentTimeMillis();
            String internalId = cloudResource.getInternalId();
            resource.setName(timeStamp + internalId);
            resource.setDescription(Collections.singletonList("outside air quality"));
            resource.setInterworkingServiceURL("https://intracom.symbiote-h2020.eu");
            getRandomFields(cloudResource);

            long in = System.currentTimeMillis();

            CloudResource returnedResource = rhClient.addL1Resource(cloudResource);

            long executionTime = (System.currentTimeMillis() - in );

            ResponseEntity<CloudResource> responseEntity = new ResponseEntity(returnedResource, HttpStatus.OK);
            if (returnedResource==null)
                responseEntity= new ResponseEntity(HttpStatus.NO_CONTENT);

            log.debug("["+this.name+"] finished with status " + responseEntity.getStatusCode() + " in "
                    + executionTime + " ms" );

            return new QueryHttpResult(this.name,responseEntity,executionTime);
        }


        private void getRandomFields(CloudResource cloudResource ) {
            long randomizer = System.currentTimeMillis();

            Resource resource = cloudResource.getResource();

            if( randomizer%5==1 ) {
                log.debug("Adding temperature, humidity to cloudResource");
                StationarySensor sensor = new StationarySensor();
                sensor.setName(resource.getName());
                sensor.setInterworkingServiceURL(resource.getInterworkingServiceURL());
                sensor.setDescription(Collections.singletonList("temperature"));
                FeatureOfInterest featureOfInterest = new FeatureOfInterest();
                featureOfInterest.setName("outside air");
                featureOfInterest.setDescription(Collections.singletonList("outside temperature and humidity"));
                featureOfInterest.setHasProperty(Arrays.asList("temperature,humidity".split(",")));
                sensor.setObservesProperty(Arrays.asList("temperature,humidity".split(",")));
                sensor.setLocatedAt(new WGS84Location(2.35, 40.8646, 12,
                        "Paris", Collections.singletonList("Somewhere in Paris")));
                cloudResource.setResource(sensor);
            } else if ( randomizer%5==2) {
                log.debug("Adding atmosphericPressure, carbonMonoxideConcentration to cloudResource");
                StationarySensor sensor = new StationarySensor();
                sensor.setName(resource.getName());
                sensor.setInterworkingServiceURL(resource.getInterworkingServiceURL());
                FeatureOfInterest featureOfInterest = new FeatureOfInterest();
                featureOfInterest.setName("outside air");
                featureOfInterest.setDescription(Collections.singletonList("outside air quality"));
                featureOfInterest.setHasProperty(Arrays.asList("atmosphericPressure,carbonMonoxideConcentration".split(",")));
                sensor.setObservesProperty(Arrays.asList("atmosphericPressure,carbonMonoxideConcentration".split(",")));
                sensor.setLocatedAt(new WGS84Location(52.513681, 13.363782, 15,
                                 "Berlin", Collections.singletonList("Grosser Tiergarten")));
                cloudResource.setResource(sensor);

            } else if (randomizer%5==3) {
                log.debug("Adding fields to service");

                Service service = new Service();
                service.setInterworkingServiceURL(resource.getInterworkingServiceURL());
                service.setName(resource.getName());
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
                cloudResource.setResource(service);

            } else if (randomizer%5==0) {
                log.debug("Adding fields to actuator");
                Actuator actuator = new Actuator();
                actuator.setInterworkingServiceURL(resource.getInterworkingServiceURL());
                actuator.setName(resource.getName());
                actuator.setDescription(Collections.singletonList("light"));
                actuator.setInterworkingServiceURL(resource.getInterworkingServiceURL());

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
                actuator.setLocatedAt(new WGS84Location(2.645, 41.246, 15,
                        "Paris", Collections.singletonList("Somewhere in Paris")));
                cloudResource.setResource(actuator);
            } else if (randomizer%5==4) {
                log.debug("Adding fields to actuator");
                Actuator actuator = new Actuator();
                actuator.setInterworkingServiceURL(resource.getInterworkingServiceURL());
                actuator.setName(resource.getName());
                actuator.setDescription(Collections.singletonList("light"));
                actuator.setInterworkingServiceURL(resource.getInterworkingServiceURL());

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
                actuator.setLocatedAt(new WGS84Location(52.513681, 13.363782, 15,
                        "Berlin", Collections.singletonList("Grosser Tiergarten")));
                cloudResource.setResource(actuator);
            }



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
