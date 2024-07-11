package com.battre.opssvc.config;

import com.battre.grpcifc.DiscoveryClientAdapter;
import com.battre.grpcifc.GrpcMethodInvoker;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.servicediscovery.ServiceDiscoveryClient;
import software.amazon.awssdk.services.servicediscovery.model.DiscoverInstancesRequest;
import software.amazon.awssdk.services.servicediscovery.model.DiscoverInstancesResponse;
import software.amazon.awssdk.services.servicediscovery.model.HttpInstanceSummary;

/** Configures the grpc method invoker to be able to use AWS CloudMap for service discovery */
@Configuration
public class GrpcMethodInvokerConfig {
  private static final Logger logger = Logger.getLogger(GrpcMethodInvokerConfig.class.getName());

  @Value("${AWS_NAMESPACE_NAME}")
  private String AWS_NAMESPACE_NAME;

  @Value("${IS_LOCAL}")
  private String IS_LOCAL;

  @Bean
  public ServiceDiscoveryClient serviceDiscoveryClient() {
    return ServiceDiscoveryClient.builder().build();
  }

  @Bean
  public DiscoveryClientAdapter discoveryClientAdapter(
      ServiceDiscoveryClient serviceDiscoveryClient) {
    return serviceName -> {
      DiscoverInstancesRequest request =
          DiscoverInstancesRequest.builder()
              .namespaceName(AWS_NAMESPACE_NAME)
              .serviceName(Boolean.parseBoolean(IS_LOCAL) ? serviceName + "Local" : serviceName)
              .build();

      DiscoverInstancesResponse response = serviceDiscoveryClient.discoverInstances(request);

      logger.info("Found instances: ");
      for (HttpInstanceSummary instance : response.instances()) {
        logger.info("Instance: " + instance.attributes().toString());
      }

      return response.instances().stream()
          .findFirst()
          .map(
              instance ->
                  Boolean.parseBoolean(IS_LOCAL)
                      ? "host.docker.internal:" + instance.attributes().get("GRPC_INSTANCE_PORT")
                      : instance.attributes().get("AWS_INSTANCE_IPV4") + ":80")
          .orElseThrow(
              () ->
                  new RuntimeException(
                      "Service instance not found in AWS Cloud Map: " + serviceName));
    };
  }

  @Bean
  public GrpcMethodInvoker grpcMethodInvoker(DiscoveryClientAdapter discoveryClientAdapter) {
    return new GrpcMethodInvoker(discoveryClientAdapter);
  }
}
