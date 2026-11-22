package com.rumi.billing.infrastructure;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI billingServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Rumi Billing Service API")
                .description("REST API of the Billing bounded context: subscription plans (US05), subscription "
                        + "status with the payment history (US07) and online payment through a simulated payment "
                        + "gateway (US24). Every route is for administrators and is called through the API gateway, "
                        + "which sets the X-User-Id and X-Role headers.")
                .version("0.1.0"));
    }
}
