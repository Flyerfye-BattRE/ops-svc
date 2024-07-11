package com.battre.opssvc.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.servicediscovery.ServiceDiscoveryClient;
import software.amazon.awssdk.services.servicediscovery.model.CreateServiceRequest;
import software.amazon.awssdk.services.servicediscovery.model.CreateServiceResponse;
import software.amazon.awssdk.services.servicediscovery.model.DeleteServiceRequest;
import software.amazon.awssdk.services.servicediscovery.model.DeregisterInstanceRequest;
import software.amazon.awssdk.services.servicediscovery.model.DnsConfig;
import software.amazon.awssdk.services.servicediscovery.model.DnsRecord;
import software.amazon.awssdk.services.servicediscovery.model.InstanceSummary;
import software.amazon.awssdk.services.servicediscovery.model.ListInstancesRequest;
import software.amazon.awssdk.services.servicediscovery.model.ListInstancesResponse;
import software.amazon.awssdk.services.servicediscovery.model.ListServicesResponse;
import software.amazon.awssdk.services.servicediscovery.model.RecordType;
import software.amazon.awssdk.services.servicediscovery.model.RegisterInstanceRequest;
import software.amazon.awssdk.services.servicediscovery.model.RegisterInstanceResponse;
import software.amazon.awssdk.services.servicediscovery.model.ServiceSummary;

@Configuration
@Profile({"dev", "prod"})
public class AwsCloudMapConfig {
  private static final Logger logger = Logger.getLogger(AwsCloudMapConfig.class.getName());

  @Value("${IS_LOCAL}")
  private String IS_LOCAL;

  @Value("${AWS_REGION}")
  private String AWS_REGION;

  @Value("${AWS_NAMESPACE_ID}")
  private String AWS_NAMESPACE_ID;

  @Value("${AWS_SERVICE}")
  private String AWS_SERVICE;

  @Value("${AWS_INSTANCE}")
  private String AWS_INSTANCE;

  @Value("${server.port}")
  private String SERVER_PORT;

  @Value("${grpc.server.port}")
  private String GRPC_PORT;

  private ServiceDiscoveryClient serviceDiscoveryClient;

  private String serviceId;

  private Region effectiveRegion;

  public AwsCloudMapConfig() {}

  @PostConstruct
  public void registerService() {
    // Svcs and insts only need to be registered for local container insts since AWS:ECS does it
    // automatically
    if (Boolean.parseBoolean(IS_LOCAL)) {
      effectiveRegion = Region.of(AWS_REGION);

      this.serviceDiscoveryClient =
          ServiceDiscoveryClient.builder()
              .region(effectiveRegion)
              .credentialsProvider(DefaultCredentialsProvider.create())
              .build();

      serviceId = createService();
      registerInstance(serviceId);
    }
  }

  @PreDestroy
  public void onDestroy() {
    if (Boolean.parseBoolean(IS_LOCAL)) {
      deregisterInstance();
      deregisterService();
    }
  }

  private void deregisterInstance() {
    logger.info("De-registering service instance");
    DeregisterInstanceRequest request =
        DeregisterInstanceRequest.builder().serviceId(serviceId).instanceId(AWS_INSTANCE).build();

    serviceDiscoveryClient.deregisterInstance(request);
  }

  private void deregisterService() {
    logger.info("De-registering service");
    DeleteServiceRequest request = DeleteServiceRequest.builder().id(serviceId).build();

    serviceDiscoveryClient.deleteService(request);
  }

  public String createService() {
    // Check if the service already exists
    ListServicesResponse listServicesResponse = serviceDiscoveryClient.listServices();
    for (ServiceSummary serviceSummary : listServicesResponse.services()) {
      if (serviceSummary.name().equals(AWS_SERVICE)) {
        return serviceSummary.id();
      }
    }

    DnsRecord dnsRecord = DnsRecord.builder().type(RecordType.A).ttl(60L).build();

    DnsConfig dnsConfig =
        DnsConfig.builder()
            .dnsRecords(Collections.singletonList(dnsRecord))
            .namespaceId(AWS_NAMESPACE_ID)
            .build();

    CreateServiceRequest createServiceRequest =
        CreateServiceRequest.builder()
            .name(AWS_SERVICE)
            .namespaceId(AWS_NAMESPACE_ID)
            .dnsConfig(dnsConfig)
            .build();

    CreateServiceResponse createServiceResponse =
        serviceDiscoveryClient.createService(createServiceRequest);

    return createServiceResponse.service().id();
  }

  public void registerInstance(String serviceId) {
    ListInstancesResponse listInstancesResponse =
        serviceDiscoveryClient.listInstances(
            ListInstancesRequest.builder().serviceId(serviceId).build());

    // Check for and delete any pre-existing instance
    for (InstanceSummary instanceSummary : listInstancesResponse.instances()) {
      if (instanceSummary.id().equals(AWS_INSTANCE)) {
        logger.info(
            "Found an instance of [" + AWS_INSTANCE + "]: Deleting before creating new instance");

        try {
          deregisterInstance();

          // Allow time for the instance to deregister
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    RegisterInstanceRequest registerInstanceRequest =
        RegisterInstanceRequest.builder()
            .serviceId(serviceId)
            .instanceId(AWS_INSTANCE)
            .attributes(
                Map.of(
                    // IPV4 must be provided but is never used and so is set to localhost
                    "AWS_INSTANCE_IPV4", "127.0.0.1",
                    "AWS_INSTANCE_PORT", SERVER_PORT,
                    "GRPC_INSTANCE_PORT", GRPC_PORT))
            .build();

    RegisterInstanceResponse registerInstanceResponse =
        serviceDiscoveryClient.registerInstance(registerInstanceRequest);

    System.out.println(
        "Service registered with instance ID: " + registerInstanceResponse.operationId());
  }
}
