package cn.wangchen.security.distributed.uaa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author wangchen
 * @version 1.0
 * @date 2022/5/20 15:35
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableHystrix
@EnableFeignClients(basePackages = {"cn.wangchen.security.distributed.uaa"})
public class UAAServer {
    public static void main(String[] args) { SpringApplication.run(UAAServer.class, args); }
}
