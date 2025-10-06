package co.analisys.gimnasio.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AggregationFilter implements GatewayFilter {

    @Autowired
    private WebClient.Builder webClientBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (path.startsWith("/api/gimnasio/usuario/") && path.endsWith("/info-completa")) {
            String userId = extractUserIdFromPath(path);
            return aggregateUserInfo(exchange, userId);
        }

        return chain.filter(exchange);
    }

    private String extractUserIdFromPath(String path) {
        String[] parts = path.split("/");
        return parts[4]; 
    }

    private Mono<Void> aggregateUserInfo(ServerWebExchange exchange, String userId) {
        System.out.println("=== AGREGANDO INFORMACIÓN PARA USUARIO ID: " + userId + " ===");

        // Extraer el token JWT del request original
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            System.err.println("No se encontró token de autorización");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        Mono<Object> miembroInfo = webClientBuilder.build().get()
            .uri("http://member-service-1:8081/api/gimnasio/miembros/" + userId)
            .header("Authorization", authHeader)
            .retrieve()
            .bodyToMono(Object.class);

        Mono<Object> clasesInfo = webClientBuilder.build().get()
            .uri("http://class-service:8083/api/gimnasio/clases/miembro/" + userId)
            .header("Authorization", authHeader)
            .retrieve()
            .bodyToMono(Object.class);

        Mono<Object> pagoInfo = webClientBuilder.build().get()
            .uri("http://payment-service:8086/api/gimnasio/pagos/" + userId + "/vigente")
            .header("Authorization", authHeader)
            .retrieve()
            .bodyToMono(Object.class);

        return Mono.zip(miembroInfo, clasesInfo, pagoInfo)
            .map(tuple -> {
                Map<String, Object> result = new HashMap<>();
                result.put("usuario", tuple.getT1());
                result.put("clases", tuple.getT2());
                result.put("pagoVigente", tuple.getT3());
                result.put("timestamp", System.currentTimeMillis());

                System.out.println("Información agregada exitosamente para usuario: " + userId);
                return result;
            })
            .flatMap(result -> {
                try {
                    exchange.getResponse().setStatusCode(HttpStatus.OK);
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

                    byte[] data = objectMapper.writeValueAsBytes(result);
                    return exchange.getResponse().writeWith(
                        Mono.just(exchange.getResponse().bufferFactory().wrap(data))
                    );
                } catch (Exception e) {
                    System.err.println("Error serializando respuesta: " + e.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return exchange.getResponse().setComplete();
                }
            })
            .doOnError(error -> {
                System.err.println("Error en agregación: " + error.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            });
    }
}