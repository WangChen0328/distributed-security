package cn.wangchen.security.distributed.order.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;

/**
 * @author wangchen
 * @version 1.0
 * @date 2022/5/21 10:11
 */
@Configuration
@EnableResourceServer
public class ResourceConfigServer extends ResourceServerConfigurerAdapter {

    /**
     * 服务名，必须在oauth注册
     */
    public static final String RESOURCE_ID = "order";

    @Autowired
    TokenStore tokenStore;

    /**
     * 远程配置令牌验证服务
     * @return
     */
    @Bean
    public ResourceServerTokenServices tokenService() {
        //使用远程服务请求授权服务器校验token,必须指定校验token 的url、client_id，client_secret
        RemoteTokenServices service=new RemoteTokenServices();
        service.setCheckTokenEndpointUrl("http://localhost:53020/uaa/oauth/check_token");
        service.setClientId("c1");//client_id
        service.setClientSecret("secret"); //client_secret
        return service;
    }

    /**
     * 令牌服务验证配置
     * @param resources
     * @throws Exception
     */
    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources.resourceId(RESOURCE_ID)
            /**
             * 使用远程服务验证令牌
             */
            //.tokenServices(tokenService())
            /**
             * 使用本地方式，验证jwt令牌
             */
            .tokenStore(tokenStore)
            /**
             * 此资源必须验证令牌
             */
            .stateless(true);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
            .antMatchers("/**").access("#oauth2.hasScope('all')")
            .and().csrf().disable()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
}
