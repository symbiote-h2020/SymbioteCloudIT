package eu.h2020.symbiote.client;

import eu.h2020.symbiote.client.interfaces.*;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.core.ci.QueryResponse;
import eu.h2020.symbiote.core.internal.CoreQueryRequest;
import eu.h2020.symbiote.core.internal.cram.ResourceUrlsResponse;
import eu.h2020.symbiote.model.cim.*;
import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyType;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static eu.h2020.symbiote.client.AbstractSymbIoTeClientFactory.*;

@Profile("search")
@Component
public class SearchClientForStressTest implements IStressTest {

    private static AbstractSymbIoTeClientFactory factory;

    private static Log log = LogFactory.getLog(SearchClientForStressTest.class);

    static int resourcesNumber;
    static List<CloudResource> resources = new ArrayList<>();
    static HashMap<String, CloudResource> resourcesPerId = new HashMap<>();
    static List<String> resourceIds = new ArrayList<>();
   // static ResourceUrlsResponse resourceUrlsResponse;
    static Boolean authentication;

    static PrintWriter outputFileStats = null;

    public void test() {

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

        //set parameters for the stress test
        int runsNumber=10;//100;//number of execution runs for periodical execution
        int stress = 5;//1;////number of resources to access
        int addNumber = 5;//1;
        int experimentRounds = 10;//20; //repeat experiment for specific run 10 times
        resourcesNumber=10000;//number of resources to register

        String directoryName = "./output";

        Boolean authentication = true;
        String testName = "search_add5_repeat10_byclient_byname_registered_" + resourcesNumber + "_" + authentication.toString();


        int run=0;
        //register and access resources periodically
        Type type = Type.FEIGN;

        // Get the configuration
        Config config = new Config(coreAddress, keystorePath, keystorePassword, type);

        // Get the factory

       // AbstractSymbIoTeClientFactory factory;
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

      //  List <CloudResource> returnedResources = rhClient.addL1Resources(resources);

        ////
        List<CloudResource> returnedResources = new ArrayList<>();
        if(resources.size()>50)
            for(int id=0; id<resources.size(); id+=50) {
                // List<String> idssublist = ids.subList(id, Math.min(id+19, ids.size()-1));
                returnedResources.addAll(rhClient.addL1Resources(resources.subList(id, Math.min(id+50, resources.size()))));
            }
        else
            returnedResources.addAll(rhClient.addL1Resources(resources));

        ////






        for(CloudResource returnedResource: returnedResources) {
            resourceIds.add(returnedResource.getResource().getId());
            resourcesPerId.put(returnedResource.getResource().getId(), returnedResource);
        }


        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

     //   CRAMClient cramClient = factory.getCramClient();
     //   resourceUrlsResponse = cramClient.getResourceUrl(new HashSet<> (resourceIds), true, new HashSet<>(Collections.singletonList(exampleHomePlatformIdentifier)));







        String fileNameStats = directoryName + (!testName.isEmpty() ? "/" + testName : "") + "/stats";

        File directoryStats = new File(directoryName+ (!testName.isEmpty() ? "/" + testName : ""));
        if(!directoryStats.exists()) {
            directoryStats.mkdir();
        }
        File fileStats = new File(fileNameStats);


        try {
            outputFileStats = new PrintWriter(fileStats);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        outputFileStats.append( "timestamp" + "\t" + "milliseconds" + "\t" + "requestsNumber" + "\t" + "Failures" + "\t" + "All_ms" + "\t" +  "min_ms"  + "\t"
                +  "max_ms"  + "\t" +  "avg_ms\n");
       // System.out.println("directory " + directoryName +  " filenNameStats " +fileNameStats);


        while(run<runsNumber) {

            for (int i=0; i<experimentRounds; i++) {
                sendRequestAndVerifyResponseSearchStress(exampleHomePlatformIdentifier, run, stress, directoryName, testName, config, authentication);
            }
            run++;

            stress+=addNumber;
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        outputFileStats.close();

        try {
            Thread.sleep(120000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        deleteAllResources(Layer.L1, exampleHomePlatformIdentifier);
    }

//    public static AbstractSymbIoTeClientFactory getClientFactory() {
//        return factory;
//    }



    public static ResponseEntity<?> sendRequestAndVerifyResponseSearchStress(String homePlatformId, Integer run, Integer stress,
                                                                          String directoryName, String testName, Config config, Boolean authentication) {



        // Start from here
        List<Callable<SearchQueryHttpResult>> tasks = new ArrayList<>();

        //populate tasks list
        for( int i = 0; i < stress; i++ ) {
            tasks.add(new SearchQueryCallable("Runner "+run+ "_" +i, homePlatformId, authentication, config));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(stress);


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
            List<Future<SearchQueryHttpResult>> futures = executorService.invokeAll(tasks);

            List<SearchQueryHttpResult> resultList = new ArrayList<>(futures.size());

            // Check for exceptions
            for (Future<SearchQueryHttpResult> future : futures) {
                // Throws an exception if an exception was thrown by the task.
                resultList.add(future.get());
            }

            long out = System.currentTimeMillis();

            //prepare results
            OptionalLong maxTimer = resultList.stream().mapToLong(qRes -> qRes.getExecutionTime()).filter(time -> time>0).max();
            OptionalLong minTimer = resultList.stream().mapToLong(qRes -> qRes.getExecutionTime()).filter(time -> time>0).min();
            OptionalDouble avgTimer = resultList.stream().mapToLong(qRes -> qRes.getExecutionTime()).filter(time -> time>0).average();
            Integer failures = resultList.stream().mapToInt(qRes ->  (qRes.responseEntity.getStatusCode().is2xxSuccessful() ? 0 : 1)).sum();
//            OptionalLong maxTimer = resultList.stream().mapToLong(qRes -> (qRes.responseEntity.getStatusCode().is2xxSuccessful() ? qRes.getExecutionTime() : null)).filter(Objects::nonNull).max();//filter(time -> time>0)
//            OptionalLong minTimer = resultList.stream().mapToLong(qRes -> (qRes.responseEntity.getStatusCode().is2xxSuccessful() ? qRes.getExecutionTime() : null)).filter(Objects::nonNull).min();
//            OptionalDouble avgTimer = resultList.stream().mapToLong(qRes -> (qRes.responseEntity.getStatusCode().is2xxSuccessful() ? qRes.getExecutionTime() : null)).filter(Objects::nonNull).average();


            resultList.stream().forEach(s -> {
                        log.debug( "["+ s.getName() + "] finished in " + s.getExecutionTime() + " ms ");
                        outputFile.println( s.getName() + " " + s.getExecutionTime());
                    }
            );

            log.debug("All tasks finished in " + ( out - in ) + " ms | min " + minTimer.orElse(-1l) + " | max "
                    + maxTimer.orElse(-1l) + " | avg " + avgTimer.orElse( -1.0) );


            outputFile.println("Timestamp " + in + "\nRequestsNumber " + stress + "\nFailures_perc " + (100.00*(failures)/(double) stress) + "\nAll_ms " + ( out - in ) + "\nmin_ms " + minTimer.orElse(-1l) + "\nmax_ms "
                    + maxTimer.orElse(-1l) + "\navg_ms " + avgTimer.orElse( -1.0));
            outputFile.close();

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

            outputFileStats.append( formatter.format(in) + "\t" + in + "\t" + stress + "\t" + (100.00*(failures)/(double) stress) + "\t" + ( out - in ) + "\t" + minTimer.orElse(-1l) + "\t"
                    + maxTimer.orElse(-1l) + "\t" + avgTimer.orElse( -1.0)+ "\n");


        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }

        return new ResponseEntity<Object>("", HttpStatus.OK);
    }


    static void getRandomFields(CloudResource cloudResource ) {
       // long randomizer = System.currentTimeMillis();
        int randomizer = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

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


    private static class SearchQueryCallable implements Callable<SearchQueryHttpResult> {

        private final String name;
        private final Config config;
        private final String homePlatformId;
        private final boolean authentication;


        public SearchQueryCallable(String name, String homePlatformId, boolean authentication, Config config) {
            this.name = name;
            this.config =  config;
            this.homePlatformId = homePlatformId;
            this.authentication = authentication;
        }

        @Override
        public SearchQueryHttpResult call() throws Exception {
            log.debug("["+this.name+"] starting");

            ResponseEntity<QueryResponse> responseEntity = new ResponseEntity(HttpStatus.OK);

            long in = System.currentTimeMillis();
            long out=System.currentTimeMillis();


            try {

                AbstractSymbIoTeClientFactory factoryCalable;

                factoryCalable = getFactory(config);
                    Set<HomePlatformCredentials> platformCredentials = new HashSet<>();

                    // example credentials
                    String username = "user";
                    String password = "user";
                    String clientId = this.name;//"iliaClient";
                    HomePlatformCredentials callableHomePlatformCredentials = new HomePlatformCredentials(
                            homePlatformId,
                            username,
                            password,
                            clientId);
                    platformCredentials.add(callableHomePlatformCredentials);

                    // Get Certificates for the specified platforms
                factoryCalable.initializeInHomePlatforms(platformCredentials);

            SearchClient searchClient = factoryCalable.getSearchClient();

            CoreQueryRequest q = new CoreQueryRequest();

            //long randomizer = System.currentTimeMillis();
            int randomizer = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);


            log.debug("Adding resource");
            String id = resourceIds.get(randomizer%resourcesNumber);
            q.setName(resourcesPerId.get(id).getResource().getName());//getInternalId());
                //  q.setPlatform_id(homePlatformId);
//            if( randomizer%5==1 ) {
//                log.debug("Adding temperature to query and resource type stationarysensor");
//                q.setObserved_property(Arrays.asList("temperature"));
//                q.setDescription("temperature");
//                q.setResource_type("StationarySensor");
//            } else if ( randomizer%5==2) {
//                log.debug("Adding humidity to query");
//                q.setObserved_property(Arrays.asList("humidity"));
//                q.setShould_rank(Boolean.TRUE);
//            } else if (randomizer%5==3) {
//                log.debug("Adding resource_type Actuator");
//                q.setResource_type("Actuator");
//                q.setShould_rank(Boolean.FALSE);
//            } else if (randomizer%5==3) {
//                log.debug("Adding location name Paris and resource type StationarySensor");
//                q.setResource_type("StationarySensor");
//                q.setLocation_name("Paris");
//            } else {
//                log.debug("Adding resource_type Service");
//                q.setResource_type("Service");
//            }


                in = System.currentTimeMillis();

                QueryResponse response = searchClient.search(q, authentication, new HashSet<>(Collections.singletonList(homePlatformId)));//getLatestObservation(resourceUrl, authentication, new HashSet<>(Collections.singletonList(homePlatformId)));
                responseEntity = new ResponseEntity<>(response, HttpStatus.OK);

                out = System.currentTimeMillis();

            } catch (Throwable t) {
                log.warn("Throwable", t);
                responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            long executionTime = (out - in );

            log.debug("["+this.name+"] finished with status " + responseEntity.getStatusCode() + " in "
                    + executionTime + " ms" );

            return new SearchQueryHttpResult(this.name,responseEntity,executionTime);
        }


    }


    private static class SearchQueryHttpResult {

        private String name;
        private final ResponseEntity responseEntity;
        private final long executionTime;

        public SearchQueryHttpResult(String name, ResponseEntity responseEntity, long executionTime ) {
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

            List<CloudResource> deletedResources = new ArrayList<>();
            if(ids.size()>50)
            for(int id=0; id<ids.size(); id+=50)
                deletedResources.addAll(rhClient.deleteL1Resources(ids.subList(id, Math.min(id+50, ids.size()))));
            else
                deletedResources.addAll(rhClient.deleteL1Resources(ids));

            return new ResponseEntity(deletedResources, HttpStatus.OK);

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