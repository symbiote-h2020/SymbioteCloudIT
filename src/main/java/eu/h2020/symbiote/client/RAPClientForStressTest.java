package eu.h2020.symbiote.client;

import eu.h2020.symbiote.client.interfaces.CRAMClient;
import eu.h2020.symbiote.client.interfaces.RAPClient;
import eu.h2020.symbiote.client.interfaces.RHClient;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.core.internal.cram.ResourceUrlsResponse;
import eu.h2020.symbiote.model.cim.*;
import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyType;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static eu.h2020.symbiote.client.AbstractSymbIoTeClientFactory.*;

@SpringBootApplication
public class RAPClientForStressTest {

    private static AbstractSymbIoTeClientFactory factory;

    private static Log log = LogFactory.getLog(RAPClientForStressTest.class);

    static int resourcesNumber;
    static List<CloudResource> resources = new ArrayList<>();
    static HashMap<String, CloudResource> resourcesPerId = new HashMap<>();
    static List<String> resourceIds = new ArrayList<>();
    static ResourceUrlsResponse resourceUrlsResponse;
    static Boolean authentication;

    public static void main(String[] args) {
        SpringApplication.run(RAPClientForStressTest.class, args);
    }

    @Component
    public static class CLR implements CommandLineRunner {


        @Override
        public void run(String... args) throws Exception {

            //message retrieval - start rabbit exchange and consumers
            startTest();
        }
    }

    public static void startTest() {

        log.debug("Starting");
        /*
        Get the factory and the client
         */


        // FILL ME
        // mandatory to run
        String coreAddress = "https://symbiote-ext.man.poznan.pl";//"https://symbiote-dev.man.poznan.pl";
        String keystorePath = "testKeystore";
        String keystorePassword = "testKeystore";
        String exampleHomePlatformIdentifier = "icom-platform";

        String directoryName = "./output";

        Boolean authentication = false;
        String testName = authentication.toString();


        //set parameters for the stress test
        int runsNumber=20;//number of execution runs
        int stress = 10;//100;//number of resources to access
        int addNumber = 10;
        resourcesNumber=20;//number of resources to register

        int run=0;
        //register and access resources periodically
        Type type = Type.FEIGN;

        // Get the configuration
        Config config = new Config(coreAddress, keystorePath, keystorePassword, type);

        // Get the factory
        try {
            factory = getFactory(config);
            Set<HomePlatformCredentials> platformCredentials = new HashSet<>();

            // example credentials
            String username = "user";
            String password = "user";
            String clientId = "iliaClient";
            HomePlatformCredentials exampleHomePlatformCredentials = new HomePlatformCredentials(
                    exampleHomePlatformIdentifier,
                    username,
                    password,
                    clientId);
            platformCredentials.add(exampleHomePlatformCredentials);


            // Get Certificates for the specified platforms
            factory.initializeInHomePlatforms(platformCredentials);

        } catch (SecurityHandlerException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }

        deleteAllResources(Layer.L1, exampleHomePlatformIdentifier);

        RHClient rhClient = factory.getRHClient(exampleHomePlatformIdentifier);

        List<String> resourceNames = new ArrayList<>();

        for(int i=0; i< resourcesNumber; i++) {
            CloudResource cloudResource = new CloudResource();


            //resource fields randomized
            Resource resource = new Resource();
            cloudResource.setResource(resource);
            Long timeStamp = System.currentTimeMillis();
            cloudResource.setInternalId("Runner"+i);
            String internalId = cloudResource.getInternalId();
            resource.setName(timeStamp + internalId);
            resourceNames.add(resource.getName());
            resource.setDescription(Collections.singletonList("outside air quality"));
            resource.setInterworkingServiceURL("https://intracom.symbiote-h2020.eu");

            try {
                cloudResource.setAccessPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
                cloudResource.setFilteringPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
            } catch (InvalidArgumentsException e) {
                e.printStackTrace();
            }

            getRandomFields(cloudResource);

            resources.add(cloudResource);


        }

        List <CloudResource> returnedResources = rhClient.addL1Resources(resources);

        for(CloudResource returnedResource: returnedResources) {
            resourceIds.add(returnedResource.getResource().getId());
            resourcesPerId.put(returnedResource.getResource().getId(), returnedResource);
        }


        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        CRAMClient cramClient = factory.getCramClient();
        resourceUrlsResponse = cramClient.getResourceUrl(new HashSet<> (resourceIds), true, new HashSet<>(Collections.singletonList(exampleHomePlatformIdentifier)));


        while(run<runsNumber) {
            sendRequestAndVerifyResponseRAPStress(exampleHomePlatformIdentifier, run, stress, directoryName, testName, factory, authentication);
//            try {
//                Thread.sleep(20000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            run++;

            stress+=addNumber;
        }


    }

    public static AbstractSymbIoTeClientFactory getClientFactory() {
        return factory;
    }



    public static ResponseEntity<?> sendRequestAndVerifyResponseRAPStress(String homePlatformId, Integer run, Integer stress,
                                                                          String directoryName, String testName, AbstractSymbIoTeClientFactory factory, Boolean authentication) {



        // Start from here
        List<Callable<RAPQueryHttpResult>> tasks = new ArrayList<>();

        //populate tasks list
        for( int i = 0; i < stress; i++ ) {
            tasks.add(new RAPQueryCallable("Runner "+run+ "_" +i, homePlatformId, factory));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(stress.intValue());


        try {

            String fileName = directoryName + (!testName.isEmpty() ? "/" + testName : "") + "/req_" + stress + "_ts_" + String.valueOf(System.currentTimeMillis());

            File directory = new File(directoryName+ (!testName.isEmpty() ? "/" + testName : ""));
            if(!directory.exists()) {
                directory.mkdir();
            }
            File file = new File(fileName);
            PrintWriter outputFile = new PrintWriter(file);

            long in = System.currentTimeMillis();

            // This is the actual test
            List<Future<RAPQueryHttpResult>> futures = executorService.invokeAll(tasks);

            List<RAPQueryHttpResult> resultList = new ArrayList<>(futures.size());

            // Check for exceptions
            for (Future<RAPQueryHttpResult> future : futures) {
                // Throws an exception if an exception was thrown by the task.
                resultList.add(future.get());
            }

            long out = System.currentTimeMillis();

            //prepare results
            OptionalLong maxTimer = resultList.stream().mapToLong(qRes -> qRes.getExecutionTime()).max();
            OptionalLong minTimer = resultList.stream().mapToLong(qRes -> qRes.getExecutionTime()).min();
            OptionalDouble avgTimer = resultList.stream().mapToLong(qRes -> qRes.getExecutionTime()).average();
            Integer failures = resultList.stream().mapToInt(qRes ->  (qRes.responseEntity.getStatusCode()!= HttpStatus.OK ? 1 : 0)).sum();

            resultList.stream().forEach(s -> {
                        log.debug( "["+ s.getName() + "] finished in " + s.getExecutionTime() + " ms ");
                        outputFile.println( s.getName() + " " + s.getExecutionTime());
                    }
            );

            log.debug("All tasks finished in " + ( out - in ) + " ms | min " + minTimer.orElse(-1l) + " | max "
                    + maxTimer.orElse(-1l) + " | avg " + avgTimer.orElse( -1.0) );


            outputFile.println("Timestamp " + in + "\nRequestsNumber " + stress + "\nFailures_perc" + (100.0*(stress-failures)/(double) stress) + "\nAll_ms " + ( out - in ) + "\nmin_ms " + minTimer.orElse(-1l) + "\nmax_ms "
                    + maxTimer.orElse(-1l) + "\navg_ms " + avgTimer.orElse( -1.0));
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


    static void getRandomFields(CloudResource cloudResource ) {
        long randomizer = System.currentTimeMillis();

        cloudResource.setPluginId("RapPluginExample");
        Resource resource = cloudResource.getResource();

        if( randomizer%5==4 ) {
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
        } else if ( randomizer%5==3) {
            log.debug("Adding atmosphericPressure, carbonMonoxideConcentration to cloudResource");
            StationarySensor sensor = new StationarySensor();
            sensor.setName(resource.getName());
            sensor.setInterworkingServiceURL(resource.getInterworkingServiceURL());
            sensor.setDescription(Collections.singletonList("temperature"));
            FeatureOfInterest featureOfInterest = new FeatureOfInterest();
            featureOfInterest.setName("outside air");
            featureOfInterest.setDescription(Collections.singletonList("outside air quality"));
            featureOfInterest.setHasProperty(Arrays.asList("atmosphericPressure,carbonMonoxideConcentration".split(",")));
            sensor.setObservesProperty(Arrays.asList("atmosphericPressure,carbonMonoxideConcentration".split(",")));
            sensor.setLocatedAt(new WGS84Location(52.513681, 13.363782, 15,
                    "Berlin", Collections.singletonList("Grosser Tiergarten")));
            cloudResource.setResource(sensor);
        } else if ( randomizer%5==2) {
            log.debug("Adding fields to service");

            Service service = new Service();
            service.setInterworkingServiceURL(resource.getInterworkingServiceURL());
            service.setName(resource.getName());
            List<String> descriptionList = Arrays.asList("@type=Beacon", "@beacon.id=f7826da6-4fa2-4e98-8024-bc5b71e0893e", "@beacon.major=44933", "@beacon.minor=46799", "@beacon.tx=0x50");
            service.setDescription(descriptionList);
            Parameter parameter = new Parameter();
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

        } else if (randomizer%5==1) {
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

        } else  {
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


    private static class RAPQueryCallable implements Callable<RAPQueryHttpResult> {

        private final String name;
        private final AbstractSymbIoTeClientFactory factory;
        private final String homePlatformId;


        public RAPQueryCallable(String name, String homePlatformId, AbstractSymbIoTeClientFactory factory) {
            this.name = name;
            this.factory =  factory;
            this.homePlatformId = homePlatformId;
        }

        @Override
        public RAPQueryHttpResult call() throws Exception {
            log.debug("["+this.name+"] starting");

            RAPClient rapClient = factory.getRapClient();

            long randomizer = System.currentTimeMillis();

            int resourceId = (int) randomizer%resourcesNumber;
            String resourceUrl = resourceUrlsResponse.getBody().get(resourceIds.get(resourceId));


            ResponseEntity<CloudResource> responseEntity = new ResponseEntity(HttpStatus.OK);

            long in = System.currentTimeMillis();
            long out=System.currentTimeMillis();

            if (resourcesPerId.get(resourceIds.get(resourceId)).getResource() instanceof Sensor) {
                in = System.currentTimeMillis();
                Observation returnedObservation = rapClient.getLatestObservation(resourceUrl, authentication, new HashSet<>(Collections.singletonList(homePlatformId)));
                out=System.currentTimeMillis();
                if (returnedObservation==null)
                    responseEntity= new ResponseEntity(HttpStatus.NO_CONTENT);
                else
                    responseEntity = new ResponseEntity(returnedObservation, HttpStatus.OK);
            }
            else
            if (resourcesPerId.get(resourceIds.get(resourceId)).getResource() instanceof Actuator) {

                String body = "{\n" +
                        "  \"OnOffCapability\" : [\n" +
                        "    {\n" +
                        "      \"on\" : true\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

                in = System.currentTimeMillis();
                rapClient.actuate(resourceUrl, body, authentication, new HashSet<>(Collections.singletonList(homePlatformId)));
                out=System.currentTimeMillis();
                responseEntity = new ResponseEntity(HttpStatus.OK);

            }
            else
            if (resourcesPerId.get(resourceIds.get(resourceId)).getResource() instanceof Service) {

                String body = "[\n" +
                        "  {\n" +
                        "      \"inputParam1\" : \"on\"\n" +
                        "  }\n" +
                        "]";


                in = System.currentTimeMillis();
                String response = rapClient.invokeService(resourceUrl, body, authentication, new HashSet<>(Collections.singletonList(homePlatformId)));
//                String response = rapClient.invokeService(resourceUrl, body, true, new HashSet<>(Collections.singletonList(homePlatformId)));
//                String response = rapClient.invokeServiceAsGuest(resourceUrl, body, true);
                out=System.currentTimeMillis();

                responseEntity = new ResponseEntity(response, HttpStatus.OK);
            }


            long executionTime = (out - in );




            log.debug("["+this.name+"] finished with status " + responseEntity.getStatusCode() + " in "
                    + executionTime + " ms" );

            return new RAPQueryHttpResult(this.name,responseEntity,executionTime);
        }


    }


    private static class RAPQueryHttpResult {

        private String name;
        private final ResponseEntity responseEntity;
        private final long executionTime;

        public RAPQueryHttpResult(String name, ResponseEntity responseEntity, long executionTime ) {
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

      //  HttpEntity requestEntity = new HttpEntity<>(null);

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