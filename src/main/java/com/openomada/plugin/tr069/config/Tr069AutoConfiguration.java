package com.openomada.plugin.tr069.config;

import com.openomada.plugin.tr069.acs.AcsController;
import com.openomada.plugin.tr069.api.Tr069ApiController;
import com.openomada.plugin.tr069.cache.ParameterCache;
import com.openomada.plugin.tr069.connectionrequest.ConnectionRequestService;
import com.openomada.plugin.tr069.faults.FaultManager;
import com.openomada.plugin.tr069.integration.omada.OmadaIntegrationService;
import com.openomada.plugin.tr069.jobs.JobEngine;
import com.openomada.plugin.tr069.metrics.CwmpMetrics;
import com.openomada.plugin.tr069.provisioning.ProvisioningEngine;
import com.openomada.plugin.tr069.repository.Tr069DeviceRepository;
import com.openomada.plugin.tr069.repository.Tr069JobRepository;
import com.openomada.plugin.tr069.security.RateLimiter;
import com.openomada.plugin.tr069.session.CwmpSessionManager;
import com.openomada.plugin.tr069.telemetry.TelemetryPipeline;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Spring Boot AutoConfiguration entrypoint for the OpenOmada TR-069 Native Plugin.
 * Explicitly defines all TR-069 beans with dedicated names to guarantee zero collision
 * with Omada internal beans (e.g. Shiro's sessionManager / securityManager).
 */
@AutoConfiguration
public class Tr069AutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(Tr069AutoConfiguration.class);

    @PostConstruct
    public void init() {
        log.info("=================================================================");
        log.info("   OpenOmada TR-069 / CWMP Native Plugin Successfully Activated   ");
        log.info("   Embedded ACS Endpoint: /tr069                                 ");
        log.info("   Management REST API:   /api/v2/tr069                          ");
        log.info("=================================================================");
    }

    @Bean(name = "cwmpSessionManager")
    @ConditionalOnMissingBean(CwmpSessionManager.class)
    public CwmpSessionManager cwmpSessionManager() {
        return new CwmpSessionManager();
    }

    @Bean(name = "cwmpParameterCache")
    @ConditionalOnMissingBean(ParameterCache.class)
    public ParameterCache cwmpParameterCache() {
        return new ParameterCache();
    }

    @Bean(name = "cwmpMetrics")
    @ConditionalOnMissingBean(CwmpMetrics.class)
    public CwmpMetrics cwmpMetrics() {
        return new CwmpMetrics();
    }

    @Bean(name = "cwmpRateLimiter")
    @ConditionalOnMissingBean(RateLimiter.class)
    public RateLimiter cwmpRateLimiter() {
        return new RateLimiter();
    }

    @Bean(name = "cwmpFaultManager")
    @ConditionalOnMissingBean(FaultManager.class)
    public FaultManager cwmpFaultManager() {
        return new FaultManager();
    }

    @Bean(name = "cwmpTelemetryPipeline")
    @ConditionalOnMissingBean(TelemetryPipeline.class)
    public TelemetryPipeline cwmpTelemetryPipeline() {
        return new TelemetryPipeline();
    }

    @Bean(name = "cwmpConnectionRequestService")
    @ConditionalOnMissingBean(ConnectionRequestService.class)
    public ConnectionRequestService cwmpConnectionRequestService() {
        return new ConnectionRequestService();
    }

    @Bean(name = "cwmpJobEngine")
    @ConditionalOnMissingBean(JobEngine.class)
    public JobEngine cwmpJobEngine() {
        return new JobEngine();
    }

    @Bean(name = "cwmpProvisioningEngine")
    @ConditionalOnMissingBean(ProvisioningEngine.class)
    public ProvisioningEngine cwmpProvisioningEngine(ParameterCache parameterCache) {
        return new ProvisioningEngine(parameterCache);
    }

    @Bean(name = "tr069DeviceRepository")
    @ConditionalOnMissingBean(Tr069DeviceRepository.class)
    public Tr069DeviceRepository tr069DeviceRepository(@Autowired(required = false) @Lazy MongoTemplate mongoTemplate) {
        return new Tr069DeviceRepository(mongoTemplate);
    }

    @Bean(name = "tr069JobRepository")
    @ConditionalOnMissingBean(Tr069JobRepository.class)
    public Tr069JobRepository tr069JobRepository(@Autowired(required = false) @Lazy MongoTemplate mongoTemplate) {
        return new Tr069JobRepository(mongoTemplate);
    }

    @Bean(name = "omadaIntegrationService")
    @ConditionalOnMissingBean(OmadaIntegrationService.class)
    public OmadaIntegrationService omadaIntegrationService(@Autowired(required = false) @Lazy MongoTemplate mongoTemplate) {
        return new OmadaIntegrationService(mongoTemplate);
    }

    @Bean(name = "cwmpAcsController")
    @ConditionalOnMissingBean(AcsController.class)
    public AcsController cwmpAcsController(CwmpSessionManager sessionManager,
                                           Tr069DeviceRepository deviceRepository,
                                           OmadaIntegrationService omadaService,
                                           ProvisioningEngine provisioningEngine,
                                           JobEngine jobEngine,
                                           TelemetryPipeline telemetryPipeline,
                                           FaultManager faultManager,
                                           ParameterCache parameterCache,
                                           RateLimiter rateLimiter,
                                           CwmpMetrics metrics) {
        return new AcsController(sessionManager, deviceRepository, omadaService, provisioningEngine,
                jobEngine, telemetryPipeline, faultManager, parameterCache, rateLimiter, metrics);
    }

    @Bean(name = "tr069ApiController")
    @ConditionalOnMissingBean(Tr069ApiController.class)
    public Tr069ApiController tr069ApiController(Tr069DeviceRepository deviceRepository,
                                                 JobEngine jobEngine,
                                                 ConnectionRequestService connectionRequestService,
                                                 TelemetryPipeline telemetryPipeline,
                                                 FaultManager faultManager,
                                                 ParameterCache parameterCache,
                                                 CwmpSessionManager sessionManager,
                                                 CwmpMetrics metrics) {
        return new Tr069ApiController(deviceRepository, jobEngine, connectionRequestService,
                telemetryPipeline, faultManager, parameterCache, sessionManager, metrics);
    }

    @Bean(name = "tr069AdminWebController")
    @ConditionalOnMissingBean(com.openomada.plugin.tr069.api.Tr069AdminWebController.class)
    public com.openomada.plugin.tr069.api.Tr069AdminWebController tr069AdminWebController() {
        return new com.openomada.plugin.tr069.api.Tr069AdminWebController();
    }

    @Bean(name = "cwmpShiroCustomizer")
    public static org.springframework.beans.factory.config.BeanPostProcessor cwmpShiroCustomizer() {
        return new org.springframework.beans.factory.config.BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if ("shiroFilter".equals(beanName) || bean.getClass().getName().contains("ShiroFilterFactoryBean")) {
                    try {
                        java.lang.reflect.Method getMapMethod = bean.getClass().getMethod("getFilterChainDefinitionMap");
                        java.lang.reflect.Method setMapMethod = bean.getClass().getMethod("setFilterChainDefinitionMap", java.util.Map.class);
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, String> existing = (java.util.Map<String, String>) getMapMethod.invoke(bean);
                        java.util.Map<String, String> customized = new java.util.LinkedHashMap<>();
                        customized.put("/tr069", "anon");
                        customized.put("/tr069/", "anon");
                        customized.put("/tr069/**", "anon");
                        customized.put("/**/tr069/**", "anon");
                        customized.put("/tr069-admin", "anon");
                        customized.put("/tr069-admin/", "anon");
                        customized.put("/tr069-admin/**", "anon");
                        customized.put("/**/tr069-admin/**", "anon");
                        customized.put("/api/v2/tr069/**", "anon");
                        customized.put("/**/api/v2/tr069/**", "anon");
                        if (existing != null) {
                            customized.putAll(existing);
                        }
                        setMapMethod.invoke(bean, customized);
                        log.info("CWMP Shiro Customizer: Successfully prepended /tr069, /tr069-admin, and /api/v2/tr069 rules to ShiroFilterFactoryBean");
                    } catch (Exception e) {
                        log.warn("CWMP Shiro Customizer reflection failed: {}", e.getMessage());
                    }
                }
                return bean;
            }
        };
    }
}
