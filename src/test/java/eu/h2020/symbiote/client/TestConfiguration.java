package eu.h2020.symbiote.client;

import eu.h2020.symbiote.client.interfaces.*;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.communication.IAAMClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class TestConfiguration {

    @Value("${test.platformId}")
    private String platformId;

    @Value("${keystorePath}")
    private String keystorePath;

    @Value("${keystorePassword}")
    private String keystorePassword;

    @Value("${clientId}")
    private String clientId;

    @Value("${symbIoTeCoreUrl}")
    private String symbIoTeCoreUrl;

    @Value("${demoApp.username}")
    private String username;

    @Value("${demoApp.password}")
    private String password;

    @Bean
    public AbstractSymbIoTeClientFactory abstractSymbIoTeClientFactory() throws SecurityHandlerException, NoSuchAlgorithmException {
        String coreUrl = symbIoTeCoreUrl.replace("/coreInterface", "");
        AbstractSymbIoTeClientFactory factory = AbstractSymbIoTeClientFactory
                .getFactory(new AbstractSymbIoTeClientFactory.Config(
                                coreUrl,
                                keystorePath,
                                keystorePassword,
                                AbstractSymbIoTeClientFactory.Type.FEIGN
                        )
                );
        AbstractSymbIoTeClientFactory.HomePlatformCredentials homePlatformCredentials =
                new AbstractSymbIoTeClientFactory.HomePlatformCredentials(platformId, username, password, clientId);
        factory.initializeInHomePlatforms(new HashSet<>(Collections.singletonList(homePlatformCredentials)));

        return factory;
    }

    @Bean
    public RHClient rhClient(AbstractSymbIoTeClientFactory abstractSymbIoTeClientFactory) {
        return abstractSymbIoTeClientFactory.getRHClient(platformId);
    }

    @Bean
    public SearchClient searchClient(AbstractSymbIoTeClientFactory abstractSymbIoTeClientFactory) {
        return abstractSymbIoTeClientFactory.getSearchClient();
    }

    @Bean
    public CRAMClient cramClient(AbstractSymbIoTeClientFactory abstractSymbIoTeClientFactory) {
        return abstractSymbIoTeClientFactory.getCramClient();
    }

    @Bean
    public RAPClient rapClient(AbstractSymbIoTeClientFactory abstractSymbIoTeClientFactory) {
        return abstractSymbIoTeClientFactory.getRapClient();
    }

    @Bean
    public PRClient prClient(AbstractSymbIoTeClientFactory abstractSymbIoTeClientFactory) {
        return abstractSymbIoTeClientFactory.getPRClient(platformId);
    }

    @Bean
    public SMClient smClient(AbstractSymbIoTeClientFactory abstractSymbIoTeClientFactory) {
        return abstractSymbIoTeClientFactory.getSMClient(platformId);
    }

    @Bean
    public IAAMClient iaamClient(AbstractSymbIoTeClientFactory abstractSymbIoTeClientFactory) {
        return abstractSymbIoTeClientFactory.getAAMClient(platformId);
    }

    @Bean(name = "homePlatformIds")
    public Set<String> homePlatformIds() {
        return new HashSet<>(Collections.singletonList(platformId));
    }
}
