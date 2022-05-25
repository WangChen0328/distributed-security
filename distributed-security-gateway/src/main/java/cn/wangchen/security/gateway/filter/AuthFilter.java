package cn.wangchen.security.gateway.filter;

import cn.wangchen.security.gateway.util.EncryptUtil;
import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 此方法用于处理，已经被 OAuth2 审核过的请求。
 * 然后将security上下文中的 jwt令牌进行，解码。
 * 将用户、权限等信息，转发给其他资源。
 * @author wangchen
 * @version 1.0
 * @date 2022/5/22 14:01
 */
@Component
public class AuthFilter extends ZuulFilter {

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof OAuth2Authentication)) {
            return null;
        }
        OAuth2Authentication oAuth2Authentication = (OAuth2Authentication) authentication;
        Authentication userAuthentication = oAuth2Authentication.getUserAuthentication();

        //获取当前用户信息
        String name = userAuthentication.getName();

        //获取当前权限信息
        List<String> authorities = new ArrayList<>();
        userAuthentication.getAuthorities().forEach(grantedAuthority -> authorities.add(grantedAuthority.getAuthority()));

        //获取request中其他信息
        OAuth2Request oAuth2Request = oAuth2Authentication.getOAuth2Request();
        Map<String, String> requestParameters = oAuth2Request.getRequestParameters();
        Map<String, Object> jsonToken = new HashMap<>(requestParameters);

        //把身份信息和权限信息放在json中，加入http的header中
        jsonToken.put("username", name);
        jsonToken.put("authorities", authorities);

        //转发给微服务
        ctx.addZuulRequestHeader("json-token", EncryptUtil.encodeUTF8StringBase64(JSON.toJSONString(jsonToken)));

        return null;
    }
}
