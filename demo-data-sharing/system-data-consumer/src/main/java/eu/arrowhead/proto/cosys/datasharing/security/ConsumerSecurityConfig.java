package eu.arrowhead.proto.cosys.datasharing.security;

import eu.arrowhead.client.library.config.DefaultSecurityConfig;
import eu.arrowhead.client.library.util.ClientCommonConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;

@Configuration
@EnableWebSecurity
public class ConsumerSecurityConfig extends DefaultSecurityConfig {

    //=================================================================================================
    // members

    @Value(ClientCommonConstants.$TOKEN_SECURITY_FILTER_ENABLED_WD)
    private boolean tokenSecurityFilterEnabled;

    private ConsumerTokenSecurityFilter tokenSecurityFilter;

    //=================================================================================================
    // methods

    //-------------------------------------------------------------------------------------------------
    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        super.configure(http);
        if (tokenSecurityFilterEnabled) {
            tokenSecurityFilter = new ConsumerTokenSecurityFilter();
            http.addFilterAfter(tokenSecurityFilter, SecurityContextHolderAwareRequestFilter.class);
        }
    }

    //-------------------------------------------------------------------------------------------------
    public ConsumerTokenSecurityFilter getTokenSecurityFilter() {
        return tokenSecurityFilter;
    }
}

