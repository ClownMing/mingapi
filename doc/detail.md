# 项目背景

API接口调用平台，帮助企业、个人统一开放接口，减少沟通成本，避免重复造轮子，为业务高效赋能。

- 普通用户：注册登录，开通接口调用权限，使用接口。
- 后台：调用统计和可视化分析接口调用情况，管理员发布接口、下线接口、新增接口。

主要功能：

- API接入
- 防止攻击（安全性）
- 不能随便调用（限制、开通）
- 统计调用次数
- 计费
- 流量保护

架构图：
![架构图](https://github.com/ClownMing/mingapi/blob/master/img.png?raw=true)
技术选型：
前端：

- Ant Design Pro
- React
- Ant Design Procomponents
- Umi
- Umi Request(Axios的封装)

后端：

- Spring Boot
- Spring Boot Starter(SDK开发)
- Dubbo (RPC)
- Nacos(注册中心)
- Spring Cloud Gateway(网关、限流、日志实现)

启动方式：
后端：

- api-backend：7529端口，后端接口管理（上传、下线、用户登录）[http://localhost:7529/api/doc.html](http://localhost:7529/api/doc.html)
- api-gateway：8090端口，网关
- api-interface：8123端口，提供各种接口服务（可以有很多个且分布在各个服务器）。注意：tests中有个测试发送请求是否能够跑通的测试用例。
- api-client-sdk：客户端SDK，只编写java文件，无端口，发送请求到8090端口，由网关进行转发到后端的api-interface服务

tests测试用例

```java

@SpringBootTest
class MingapiInterfaceApplicationTests {

    @Resource
    private MingApiClient mingApiClient;

    @Test
    void contextLoads() {
        String result1 = mingApiClient.getNameByGet("ming");
        String result2 = mingApiClient.getNameByPost("ming");
        User user = new User();
        user.setUsername("clownMing");
        String result3 = mingApiClient.getUserNameByPost(user);
        System.out.println(result1);
        System.out.println(result2);
        System.out.println(result3);
    }
}
```

# 详细设计

## 一、设计和脚手架

### 需求分析（api-backend）

- 管理员可以对接口信息进行增删改查
- 用户可以访问前台，查看接口信息

### 数据库设计

请参考参考源码的sql文件夹

- interface_info：接口信息
- user：用户信息
- user_interface：多对多的用户和接口的映射

### 脚手架和CRUD

前端：Ant design pro模板 + OneAPI插件生成crud (
参考[https://pro.ant.design/zh-CN/docs/openapi/](https://pro.ant.design/zh-CN/docs/openapi/))
swagger文档[http://localhost:7529/api/doc.html](http://localhost:7529/api/doc.html)遵循openapi规范。

在config.ts中执行

```ts
  openAPI: [
    {
        requestLibPath: "import { request } from '@umijs/max'",
        schemaPath: 'http://localhost:7529/api/v3/api-docs',
        projectName: 'mingapi-backend',
    },
]
```

后端：spring boot + mybatisX插件/mybatis-plus插件都可以生成crud

## 二、开发api-interface

api-interface是我们自定义的api接口

### 调用接口的方式

自定义的api接口，那么其他服务怎么调用呢？

在这里我们可以考虑常见的使用方式，在这里我们使用Hutool工具类

- HttpClient
- RestTemplate
- 第三方库(OKHTTP、Hutool)   [Hutool官网](https://hutool.cn/docs)

### API签名认证

为了保证安全性，不能随便访问我们的API接口，所以需要签发签名和使用签名(校验签名)。
流程：前端通过在请求头带上accessKey参数，后端通过String accessKey = request.getHeader("accessKey")
;方法取出，然后与数据库中的secretKey进行对比。一致才让客户访问，并让调用次数+1。

- accessKey：调用的标识userA,userB(复杂、无序、无规律)
- secretKey：密钥（复杂、无序、无规律）该参数不能放到请求头中
- (两者类似用户名和密码，但是ak、sk是无状态的，要求每次访问都带上)

值得我们注意的是：
1. 千万不能把秘钥直接在服务器之间进行传递，一旦被拦截后果不堪设想

可以通过md5进行非对称加密生成签名，还可以考虑增加时间戳、随机数进行防重。

生成签名工具类

```java
package com.ming.mingapiclientsdk.utils;

import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;

public class SignUtils {
    /**
     * 生成签名
     */
    public static String genSign(String body, String secretKey) {
        Digester md5 = new Digester(DigestAlgorithm.SHA256);
        String content = body + "." + secretKey;
        String digestHex = md5.digestHex(content);
        return digestHex;
    }

}
```

### SDK（starter）开发

但是我们开发者每次调用接口都要自己写签名算法吗？答案不言而喻。我们可以抽取出来做一个单独的starter。以后使用只需要引入并且配置好accessKey和secretKey就可以了。
一般分下面几步骤，具体代码请参考client-sdk模块或者网络查找资料。

- 依赖环境写在pom.xml文件，移除build部分
- 编写配置类（启动类）
- 注册配置类（启动类）到resources/META_INF/spring.factories文件中
- mvn install 打包到自己的本地仓库（后续也可以发布到自己的maven网上仓库）

## 三、开发api-backend

api-backend是本项目的管理后台，上线、下线、接口发布、接口调用分析等功能。

### 上线和下线接口（管理员）

接口管理，上线和下线功能。仅管理员可以操作。
后台接口：校验该接口是否存在、判断该接口是否可以调用（调用一次）、修改接口数据库中的状态字段为1
下线接口：校验该接口是否存在、修改接口数据库中的状态字段为0
具体代码参考InterfaceInfoController

### 申请签名

用户注册成功的时候，自动分配ak和sk。TODO：可以改成申请的方式。

### 在线调用

流程：

- 前端将用户输入的请求参数和要测试的接口发给平台后端
- (在调用前可以做一些校验)
- 平台后端去调用模拟接口

![image.png](https://cdn.nlark.com/yuque/0/2022/png/1518177/1671112449314-91566171-5b67-4044-85a9-14a06e4fa2c2.png#averageHue=%23fcfcfc&clientId=u77cd8f41-c193-4&from=paste&height=503&id=u9c7e4ac8&name=image.png&originHeight=503&originWidth=632&originalType=binary&ratio=1&rotation=0&showTitle=false&size=29113&status=done&style=none&taskId=ud14f96b3-71da-423e-baec-a4f64f27c9d&title=&width=632)

## 四、调用统计

### 表格设计

用户和接口的多对多映射表。
![image.png](https://cdn.nlark.com/yuque/0/2022/png/1518177/1671092507156-4e53dc69-e192-4f97-b288-9c00afb5fb23.png#averageHue=%23f6f6f5&clientId=u77cd8f41-c193-4&from=paste&height=246&id=ULWW2&name=image.png&originHeight=246&originWidth=904&originalType=binary&ratio=1&rotation=0&showTitle=false&size=49274&status=done&style=none&taskId=ufe415588-2594-4cff-ac27-45c22bdd8a2&title=&width=904)
为了让用户调用+1的代码复用，一般有拦截器、过滤器、AOP切面等实现方式。但是缺点是只适用于单个项目中，且容易有错误，因为统计次数是在调用次数之后执行的。如果每个团队都开发自己的接口，需要重复实现自己的AOP切面。所以我们需要在网关实现！

### 网关学习资料

作用：
1.路由：[https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#gateway-request-predicates-factories](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#gateway-request-predicates-factories)
2.负载均衡
3.统一鉴权
4.跨域 [https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#cors-configuration](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#cors-configuration)
5.统一业务处理（缓存）
6.访问控制 （黑白名单）
7.发布控制（灰度发布） [https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-weight-route-predicate-factory](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-weight-route-predicate-factory)
8.流量染色（给请求头添加一些字段，标明来源）[https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-addrequestheader-gatewayfilter-factory](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-addrequestheader-gatewayfilter-factory)
9.接口保护

-
限制请求  [https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#requestheadersize-gatewayfilter-factory](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#requestheadersize-gatewayfilter-factory)
-
信息脱敏  [https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-removerequestheader-gatewayfilter-factory](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-removerequestheader-gatewayfilter-factory)
-
降级（熔断）[https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#fallback-headers](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#fallback-headers)
-
限流（令牌桶算法、漏桶算法，RedisLimitHandler可以了解一下）[https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-requestratelimiter-gatewayfilter-factory](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-requestratelimiter-gatewayfilter-factory)
-
超时时间 [https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#http-timeouts-configuration](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#http-timeouts-configuration)
-
重试 [https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-retry-gatewayfilter-factory](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-retry-gatewayfilter-factory)

10.统一日志
11.统一文档，建议用[https://doc.xiaominfo.com/docs/middleware-sources/aggregation-introduction](https://doc.xiaominfo.com/docs/middleware-sources/aggregation-introduction)

技术选型：[https://zhuanlan.zhihu.com/p/500587132](https://zhuanlan.zhihu.com/p/500587132)

- Nginx(全局网关)、Kong网关(API网关，Kong:[https://github.com/Kong/kong),](https://github.com/Kong/kong),)编程成本相对高
- Spring Cloud Gateway(取代了Zuul)性能高、可以用Java代码来写逻辑，适于学习

本项目网关使用 Spring Cloud Gateway，官方文档如下。
![image.png](https://cdn.nlark.com/yuque/0/2022/png/1518177/1671113345258-d4c10960-5835-4da8-a835-12b12548b1ea.png#averageHue=%23f6f6f6&clientId=u77cd8f41-c193-4&from=paste&height=580&id=u0b746ae8&name=image.png&originHeight=580&originWidth=433&originalType=binary&ratio=1&rotation=0&showTitle=false&size=39995&status=done&style=none&taskId=u603d3525-ea4b-4bbb-8ab0-9beb52cda83&title=&width=433)
两种配置方式，先用配置式，不能满足要求再用编程式。

-
编程式（麻烦但是灵活）[https://spring.io/projects/spring-cloud-gateway/#samples](https://spring.io/projects/spring-cloud-gateway/#samples)
- 配置式（简单）

建议开启网关的级别为trace，方便在开发的过程中代码调试。

```yaml
logging:
  level:
    org:
      springframework:
        cloud:
          gateway: trace
```

## 五、开发api-gateway

参考架构图，网关模块。

### 业务逻辑

本项目网关的业务逻辑：
1.用户发送请求到API网关
2.记录请求日志
3.(黑白名单)
4.用户鉴权（判断ak、sk是否合法）
5.请求的模拟接口是否存在？
6.请求转发，调用模拟接口
7.响应日志
8.调用成功，接口调用次数+1
9.调用失败，返回一个规范的错误码

### 具体实现

请求转发使用前缀匹配，例如把所有路径为：/api/**的请求进行转发，转发到http://localhost:8123/api/*
举个例子：
请求网关：http://localhost:8090/api/name/get?name=testName
转发到 ：http://localhost:8123/api/name/get?name=testName

```yaml
server:
  port: 8090

spring:
  cloud:
    gateway:
      routes:
        # id不重复就好
        - id: api_route
          uri: http://localhost:8123
          predicates:
            - Path=/api/**
```

代码实现业务逻辑使用了GlobalFilter(编程式)，全局请求拦截处理（类似AOP)
> 因为网关项目没引入MyBatis等操作数据库的类库，如果该操作较为复杂，可以由backend增删改查项目提供接
> 口，我们直接调用，不用再重复写逻辑了。调用的方式可以是HTTP请求（用HTTPClient、.用RestTemplate、Feign) 或者用 RPC(Dubbo)

### 遇到问题

预期是等模拟接口调用完成，才记录响应日志、统计调用次数。 但现实是chain.filter方法立刻返回了，直到filter过滤器return后才调用了模拟接口。 原因是：chain.filter是个异步操作，理解为前端的promise
解决方案：利用response装饰者，增强原有response的处理能力。

装饰者模式参考  [参考装饰者](https://www.jianshu.com/p/d7f20ae63186)

具体代码如下，详见CustomGlobalFilter
参考博客 [参考博客](https://blog.csdn.net/qq_19636353/article/details/126759522)

具体 GlobalFilter 代码如下：

```java
/**
 * gateway 全局过滤器
 */
@Slf4j
@Component
public class CustomGlobalFilter implements GlobalFilter, Ordered {

    @DubboReference
    private InnerUserInterfaceInfoService innerUserInterfaceInfoService;

    @DubboReference
    private InnerUserService innerUserService;

    @DubboReference
    private InnerInterfaceInfoService innerInterfaceInfoService;


    private static final List<String> IP_WHITE_LIST = Arrays.asList("127.0.0.1");

    private static final String INTERFACE_HOST = "http://localhost:8123";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 请求日志
        ServerHttpRequest request = exchange.getRequest();
        log.info("请求唯一标识：" + request.getId());
        String path = INTERFACE_HOST + request.getPath().value();
        log.info("请求路径：" + path);
        String method = request.getMethod().toString();
        log.info("请求方法：" + method);
        log.info("请求参数：" + request.getQueryParams());
        String sourceAddress = request.getRemoteAddress().getHostString();
        log.info("请求来源地址：" + sourceAddress);
        // 2. 访问控制 黑白名单
        ServerHttpResponse response = exchange.getResponse();
        if (!IP_WHITE_LIST.contains(sourceAddress)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }
        // 3. 用户鉴权(判断 ak、sk是否合法)
        HttpHeaders headers = request.getHeaders();
        String accessKey = headers.getFirst("accessKey");
        String nonce = headers.getFirst("nonce");
        String timestamp = headers.getFirst("timestamp");
        String sign = headers.getFirst("sign");
        String body = headers.getFirst("body");
        // 从数据库查询是否已分配用户
        User invokeUser = null;
        try {
            // 通过传来的 accessKey，通过远程调用到数据库中查询，得到信息
            invokeUser = innerUserService.getInvokeUser(accessKey);
        } catch (Exception e) {
            log.error("getInvokeUser error", e);
        }
        if (invokeUser == null) {
            return handleNoAuth(response);
        }
        if (Long.parseLong(nonce) > 10000L) {
            return handleNoAuth(response);
        }
        // 时间和当前时间不能超过5分钟
        Long currentTime = System.currentTimeMillis() / 1000;
        final Long FIVE_MINUTES = 60 * 5L;
        if ((currentTime - Long.parseLong(timestamp)) >= FIVE_MINUTES) {
            return handleNoAuth(response);
        }
        String secretKey = invokeUser.getSecretKey();
        String serverSign = SignUtils.genSign(body, secretKey);
        if (sign == null || !sign.equals(serverSign)) {
            return handleNoAuth(response);
        }
        // 4. 请求的模拟接口是否存在,以及请求方法是否匹配
        InterfaceInfo interfaceInfo = null;
        try {
            interfaceInfo = innerInterfaceInfoService.getInterfaceInfo(path, method);
        } catch (Exception e) {
            log.error("getInterfaceInfo error", e);
        }
        if (interfaceInfo == null) {
            return handleNoAuth(response);
        }
        // todo 判断用户是否还有调用次数(这里可以判断一下是否还有次数，但是这里是自己测试用，等以后有计费操作再完善)
        // 5. 请求转发，调用模拟接口 + 响应日志(在6中已经包含 把装饰过的 response对象设置进去，这下面的一行代码就是在真正执行interface的方法)
        // 这里有个小问题：chain.filter是个异步操作，我们本想等chain.filter方法调用完成再往下走，可惜的是，只有当return filter后
        // 才调用chain.filter的方法(也就是我们interface项目中真正的方法)
        return handleResponse(exchange, chain, interfaceInfo.getId(), invokeUser.getId());
    }

    /**
     * 处理响应
     */
    public Mono<Void> handleResponse(ServerWebExchange exchange, GatewayFilterChain chain, long interfaceInfoId, long userId) {
        try {
            ServerHttpResponse originalResponse = exchange.getResponse();
            // 缓存数据的工厂
            DataBufferFactory bufferFactory = originalResponse.bufferFactory();
            // 拿到响应码
            HttpStatus statusCode = originalResponse.getStatusCode();
            if (statusCode == HttpStatus.OK) {
                // new 一个Response的装饰器对象,并封装之前的Response对象 进行能力增强
                ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
                    // 等调用完转发的接口后才会执行
                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        log.info("body instanceof Flux: {}", (body instanceof Flux));
                        if (body instanceof Flux) {
                            Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                            // 往返回值中写数据
                            // 拼接字符串
                            return super.writeWith(fluxBody.map(dataBuffer -> {
                                // 6. 调用成功，接口调用次数 + 1 invokeCount
                                try {
                                    innerUserInterfaceInfoService.invokeCount(interfaceInfoId, userId);
                                } catch (Exception e) {
                                    log.error("invokeCount error", e);
                                }
                                byte[] content = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(content);
                                DataBufferUtils.release(dataBuffer);//释放掉内存
                                // 构建日志
                                StringBuilder sb2 = new StringBuilder(200);
                                List<Object> rspArgs = new ArrayList<>();
                                rspArgs.add(originalResponse.getStatusCode());
                                // 这个data就是我们真正的响应结果
                                String data = new String(content, StandardCharsets.UTF_8);//data
                                sb2.append(data);
                                // 打印日志
                                log.info("响应结果：" + data);
                                return bufferFactory.wrap(content);
                            }));
                        } else {
                            // 8. 调用失败，返回一个规范的错误码
                            log.error("<--- {} 响应code异常", getStatusCode());
                        }
                        return super.writeWith(body);
                    }
                };
                // 把装饰过的 response对象设置进去
                return chain.filter(exchange.mutate().response(decoratedResponse).build());
            }
            return chain.filter(exchange);//降级处理返回数据
        } catch (Exception e) {
            log.error("网关处理响应异常。异常信息为：" + e);
            return chain.filter(exchange);
        }
    }


    @Override
    public int getOrder() {
        return -1;
    }

    public Mono<Void> handleNoAuth(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    public Mono<Void> handleInvokeError(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return response.setComplete();
    }
}
```

## 六、跑通RPC

### 如何调用其他项目的方法

1.复制代码和依赖、环境
2.HTTP请求（提供一个接口，供其他项目调用）
3.RPC
4.把公共的代码打个jar包，其他项目去引用（客户端SDK)

### HTTP请求如何调用

1.提供方开发一个接口（地址、请求方法、参数、返回值）
2.调用方使用HTTP Client之类的代码包去发送HTTP请求

### RPC

作用：像调用本地方法一样调用远程方法。
1.对开发者更透明，减少了很多的沟通成本。
2.RPC向远程服务器发送请求时，未必要使用HTTP（7层）协议，比如还可以用TCP/IP，性能更高。（内部服务更适合）
http不是rpc，rpc可能会用到http协议

### Dubbo框架(RPC实现)

GRPC、TRPC，最好的学习方式：阅读官方文档[https://dubbo.incubator.apache.org/zh/docs3-v2/java-sdk/quick-start/spring-boot/](https://dubbo.incubator.apache.org/zh/docs3-v2/java-sdk/quick-start/spring-boot/)

两种使用方式：
1.Spring Boot代码（注解+编程式）：写Java接口，服务提供者和消费者都去引用这个接口。
2.IDL接口调用语言)：创建一个公共的接口定义文件（.proto），服务提供者和消费者读取这个文件。优点是跨语言，所有的框架都认识。

底层是triple协议：[https://cn.dubbo.apache.org/zh/docs3-v2/java-sdk/concepts-and-architecture/triple/](https://cn.dubbo.apache.org/zh/docs3-v2/java-sdk/concepts-and-architecture/triple/)

### 整合运用

1.backend项目作为服务提供者，提供3个方法：
a.去数据库中查是否已分配给用户
b.从数据库中查询模拟接口是否存在，以及请求方法是否匹配（还可以校验请求参数）
c.调用成功，接口调用次数+1 invokeCount gateway项目作为服务调用者，调用这3个方法

在这里我们使用Nacos作为注册中心
[https://dubbo.apache.org/zh/docs3-v2/java-sdk/reference-manual/registry/nacos/](https://dubbo.apache.org/zh/docs3-v2/java-sdk/reference-manual/registry/nacos/)

注意：
1.服务接口类必须要在同一个包下，建议是抽象出一个公共项目（放接口、实体类等）
2.设置注解（比如启动类的EnableDubbo、接口实现类和Bean引用的注解）
3.添加配置
4.服务调用项目和提供者项目尽量引入相同的依赖和配置(避免冲突，尤其是slf4j)

5. 如果使用 Dubbo3 那么Nacos至少使用2.0.0的版本

```xml
<dependencies>
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo</artifactId>
        <version>3.0.9</version>
    </dependency>
    <dependency>
        <groupId>com.alibaba.nacos</groupId>
        <artifactId>nacos-client</artifactId>
        <version>2.1.0</version>
    </dependency>
</dependencies>
```

## 七、完成项目

### 网关业务逻辑

去数据库中查是否已分配给用户秘钥(ak、sk是否合法)
1.先根据accessKey判断用户是否存在，查到secretKey.对比secretKey和用户传的加密后的secretKey是否一致
2.从数据库中查询模拟接口是否存在，以及请求方法是否匹配（还可以校验请求参数）
3.调用成功，接口调用次数+1 invokeCount
上面这块逻辑写到公共服务里面。

### 公共服务

目的是让方法、实体类在多个项目间复用，重复编写。
1.数据库中查是否已分配给用户秘钥（根据accessKey拿到用户信息，返回用户信息，为空表示不存在）
2.从数据库中查询模拟接口是否存在（请求路径、请求方法、请求参数，返回接口信息，为空表示不存在）
3.接口调用次数+1 invokeCount(accessKey、secretKey(标识用户)，请求接口路径)

步骤：
1.新建干净的maven项目，只保留必要的公共依赖
2.抽取service和实体类
3.install本地maven包
4.让服务提供者引l入common包，测试是否正常运行
5.让服务消费者引入common包

### 问题：如何获取接口转发服务器的地址

网关启动时，获取所有的接口信息，维护到内存的hashmap中；有请求时，根据请求的url路径或者其他参数(比如host请求头)来判断应该转发到哪台服务器、以及用于校验接口是否存在。

### 开发统计分析

#### 需求

各接口的总调用次数占比（饼图）取调用最多的前3个接口，从而分析出哪些接口没有人用（降低资源、或者下线)，高频接口（增加资源、提高收费）

#### 实现

#### 前端

需求：展示饼图推荐要用现成的库！！！
ECharts（推荐）:[https://echarts.apache.org/zh/index.html](https://echarts.apache.org/zh/index.html)
(AntV:[https://antv.vision/zh(](https://antv.vision/zh()蚂蚁，最推荐)、BizCharts（更炫酷一点）

用法贼简单：
1.看官网
2.找到快速入门、按文档去引入库
3.进入示例页面
4.找到你要的图
5.在线调试
6.复制代码
7.改为真实数据

如果是React项目，用这个库：[https://github.com/hustcc/echarts-for-react](https://github.com/hustcc/echarts-for-react)

#### 后端

写一个接口，得到下列示例数据：
接口A:2次
接口B:3次
1.SQL查询调用数据：select interfacelnfold,sum(totalNum)as totalNum from user_interface_info groupby interfacelnfold order by
totalNum desc limit 3;
2.业务层去关联查询接口信息

### 上线

前端：略
后端：

- backend项目：web项目，部署spring boot的jar包（对外的）
- gateway网关项目：web项目，部署spring boot的jar包（对外的）
- interface模拟接口项目：web项目，部署spring boot的jar包（不建议对外暴露的）

关键：网络必须要连通
如果自己学习用：单个服务器部署这三个项目就足够
如果你是搞大事，多个服务器建议在同一内网，内网交互会更快、且更安全

### 扩展

1.怎么让其他用户也上传接口？
a.需要提供一个机制（界面），让用户输入自己的接口host(服务器地址)、接口信息，将接口信息写入数据库。
b.将接口信息写入数据库之前，要对接口进行校验（比如检查他的地址是否遵循规则，测试调用），保证他是正常的。
c.将接口信息写入数据库之前遵循咱们的要求（并且使用咱们的Sdk),在接入时，平台需要测试调用这个接口，保证他是正常的。

2.在interfacelnfo表里加个host字段，区分服务器地址，让接口提供者更灵活地接入系统。

3.网关判断是否还有调用次数

4.网关限流、提高性能等


## 1. 如果要添加新的远程接口

使用gateway配置转发路径，并且在数据库中上线接口（可以手动填写）试试？ 因为网关的过滤器要匹配用户是否具有某个接口的权限。如何判断是哪个用户呢？通过ak唯一匹配userID。

## 2、dubbo和nacos在本项目的作用

nacos作为dubbo的注册中心存在
dubbo在网关api-gateway调用了后端api-backend项目的接口




