package co.analisys.gimnasio.config;  
import org.springframework.context.annotation.Bean; 
import org.springframework.context.annotation.Configuration; 
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration 
@EnableWebFluxSecurity 
public class SecurityConfig { 
 
    @Bean 
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) { 
        http 
            .authorizeExchange() 
                .pathMatchers("/class/**").permitAll() 
                .pathMatchers("/equipment/**").permitAll() 
                .pathMatchers("/member/**").permitAll() 
                .pathMatchers("/notification/**").permitAll() 
                .pathMatchers("/payment/**").permitAll() 
                .pathMatchers("/trainer/**").permitAll() 
                .anyExchange().authenticated() 
            .and() 
            .oauth2ResourceServer() 
                .jwt(); 
        return http.build(); 
    } 
} 