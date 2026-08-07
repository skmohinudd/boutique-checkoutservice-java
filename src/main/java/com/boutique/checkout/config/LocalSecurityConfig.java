package com.boutique.checkout.config;
import org.springframework.context.annotation.*;import org.springframework.security.config.annotation.web.builders.HttpSecurity;import org.springframework.security.web.SecurityFilterChain;
@Configuration @Profile("!aws")
public class LocalSecurityConfig {
 @Bean SecurityFilterChain localSecurity(HttpSecurity http)throws Exception{return http.csrf(csrf->csrf.disable()).authorizeHttpRequests(auth->auth.anyRequest().permitAll()).build();}
}
