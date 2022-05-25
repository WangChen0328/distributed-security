package cn.wangchen.security.distributed.uaa.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.code.InMemoryAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.code.JdbcAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * 授权服务器配置
 *  1> 客户端详情
 *  2> 令牌服务和授权模式
 *  3> 令牌端点约束
 *
 *  申请授权码
 *  /uaa/oauth/authorize?client_id=c1&response_type=code&scope=all&redirect_uri=http://www.baidu.com
 *
 *  client_id：客户端准入标识。
 *  response_type：授权码模式固定为code。
 *  scope：客户端权限。
 *  redirect_uri：跳转uri，当授权码申请成功后会跳转到此地址，并在后边带上code参数（授权码）。
 *
 *  申请令牌
 *  /uaa/oauth/token?client_id=c1&client_secret=secret&grant_type=authorization_code&code=5PgfcD&redirect_uri=http://w ww.baidu.com
 *
 *  client_id：客户端准入标识。
 *  client_secret：客户端秘钥。
 *  grant_type：授权类型，填写authorization_code，表示授权码模式
 *  code：授权码，就是刚刚获取的授权码，注意：授权码只使用一次就无效了，需要重新申请。
 *  redirect_uri：申请授权码时的跳转url，一定和申请授权码时用的redirect_uri一致。
 *
 *  {
 *     "access_token": "555bb1bc-b894-4cc1-a1ad-035141548f93", 访问令牌
 *     "token_type": "bearer",
 *     "refresh_token": "cb8d0010-8e52-4cfd-9fe8-f8b3b505ce2d", 刷新令牌
 *     "expires_in": 7199,
 *     "scope": "all"
 * }
 *
 * @author wangchen
 * @version 1.0
 * @date 2022/5/20 16:00
 */
@Configuration
@EnableAuthorizationServer
public class AuthorizationServer extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 默认为 InMemoryClientDetailsService 存入内存
     * 重写为 JdbcClientDetailsService 存入数据库，集群模式
     */
    @Autowired
    private ClientDetailsService clientDetailsService;

    @Bean
    public ClientDetailsService clientDetailsService(DataSource dataSource) {
        JdbcClientDetailsService clientDetailsService = new JdbcClientDetailsService(dataSource);
        clientDetailsService.setPasswordEncoder(passwordEncoder);
        return clientDetailsService;
    }

    /**
     * 客户端注册信息
     * @param clients
     * @throws Exception
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        //clients.inMemory() //使用内存方式存储client信息
        //    .withClient("c1") // client_id
        //    .secret("secret")//客户端密码  使用认证管理器的 userDetailService的密码加密区，目前用的明文
        //    .resourceIds("order")//资源列表
        //    .authorizedGrantTypes(//client的授权类型
        //        "authorization_code", //授权码模式
        //        "password", //密码模式
        //        "client_credentials",//客户端模式
        //        "implicit",//简化模式，没有code环节
        //        "refresh_token")
        //    .scopes("all")//允许的授权范围
        //    .autoApprove("false")//false 跳转到授权页面
        //    .redirectUris("http://www.baidu.com");//回调地址，一定和页面的回调地址保持一致

        /**
         * 从数据库中加载client信息
         * 使用数据库存储，客户端的注册信息
         * 需要先将表中加入数据
         */
        clients.withClientDetails(clientDetailsService);
    }

    /**
     * 令牌存储策略
     */
    @Autowired
    private TokenStore tokenStore;

    /**
     * jwt令牌转换
     */
    @Autowired
    private JwtAccessTokenConverter accessTokenConverter;

    /**
     * 令牌服务
     * @return
     */
    @Bean
    public AuthorizationServerTokenServices tokenService() {
        DefaultTokenServices service = new DefaultTokenServices();
        service.setClientDetailsService(clientDetailsService);//客户端注册信息,可以从内存和数据库中读取
        service.setSupportRefreshToken(true);//支持刷新令牌
        service.setTokenStore(tokenStore);//令牌存储策略

        /**
         * 增加jwt令牌增强
         */
        TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
        tokenEnhancerChain.setTokenEnhancers(Arrays.asList(accessTokenConverter));
        service.setTokenEnhancer(tokenEnhancerChain);

        service.setAccessTokenValiditySeconds(7200); // 令牌默认有效期2小时
        service.setRefreshTokenValiditySeconds(259200); // 刷新令牌默认有效期3天
        return service;
    }

    /**
     * 授权码服务
     * 用于 "authorization_code" 授权码类型模式
     * @return
     */
    @Bean
    public AuthorizationCodeServices authorizationCodeServices(DataSource dataSource) {
        //设置授权码模式的授权码如何 存取，暂时采用内存方式
        //return new InMemoryAuthorizationCodeServices();
        return new JdbcAuthorizationCodeServices(dataSource);
    }

    /**
     * 认证管理器
     * 用于 "password" 授权码类型模式
     */
    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * 授权码存储策略
     */
    @Autowired
    private AuthorizationCodeServices authorizationCodeServices;

    /**
     * 配置令牌的授权模式和令牌服务
     * @param endpoints
     * @throws Exception
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
            /**
             * 密码模式，必须使用认证管理器
             * 该认证管理器，引用的事 spring security的认证管理器
             * 会调用自定义的 UserDetailsService
             */
            .authenticationManager(authenticationManager)
            /**
             * 授权码模式
             */
            .authorizationCodeServices(authorizationCodeServices)
            /**
             * 令牌服务，令牌的存储规则、令牌发放的客户端信息
             */
            .tokenServices(tokenService())
            /**
             * 访问的HTTP方式
             */
            .allowedTokenEndpointRequestMethods(HttpMethod.POST);
    }

    /**
     * 令牌端点的安全约束
     * /oauth/authorize：授权端点。
     * /oauth/token：令牌端点。
     * /oauth/confirm_access：用户确认授权提交端点。
     * /oauth/error：授权服务错误信息端点。
     * /oauth/check_token：用于资源服务访问的令牌解析端点。
     * /oauth/token_key：提供公有密匙的端点，如果你使用JWT令牌的话。
     *
     * @param security
     * @throws Exception
     */
    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security
            .tokenKeyAccess("permitAll()")//提供公有密匙的端点，如果你使用JWT令牌的话
            .checkTokenAccess("permitAll()")//用于资源服务访问的令牌解析端点
            .allowFormAuthenticationForClients();
    }
}
