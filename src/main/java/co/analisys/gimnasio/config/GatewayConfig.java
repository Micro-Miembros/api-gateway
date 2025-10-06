package co.analisys.gimnasio.config;
import co.analisys.gimnasio.filter.AggregationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

@Configuration 
public class GatewayConfig { 
    
    @Autowired
    private AggregationFilter aggregationFilter;
    
    @Bean 
    public GlobalFilter customGlobalFilter() { 
        return (exchange, chain) -> { 
            ServerHttpRequest request = exchange.getRequest(); 
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION); 
            if (authHeader != null) { 
                return chain.filter(exchange.mutate() 
                    .request(request.mutate() 
                        .header(HttpHeaders.AUTHORIZATION, authHeader) 
                        .build()) 
                    .build()); 
            } 
            return chain.filter(exchange); 
        }; 
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("aggregated-user-info", r -> r.path("/api/gimnasio/usuario/*/info-completa")
                .filters(f -> f.filter(aggregationFilter))
                .uri("no://op"))
            .build();
    }
}