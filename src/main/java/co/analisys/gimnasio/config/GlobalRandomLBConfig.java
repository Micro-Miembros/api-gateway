package co.analisys.gimnasio.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;


@Configuration
@LoadBalancerClients(defaultConfiguration = GlobalRandomLBConfig.RandomPerServiceConfiguration.class)
public class GlobalRandomLBConfig {


    public static class RandomPerServiceConfiguration {

        @Bean
        public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
                Environment environment,
                LoadBalancerClientFactory factory) {
            String serviceId = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
            System.out.println("Creating RandomLoadBalancer for service: " + serviceId);
            return new RandomLoadBalancer(
                    factory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class),
                    serviceId);
        }
    }
}
