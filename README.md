# Qi-API

## 项目简介

「Qi-API」是一款高效、可靠和安全的接口开放平台，为广大用户提供高质量、可靠、安全的接口服务，帮助用户轻松实现各种功能和数据交互，提高工作效率和用户体验。

本项目为**前后端分离项目**，前端主要采用TypeScript、React、Ant Design Pro等主流开发框架。后端采用Spring Cloud SpringBoot
作为业务框架。通过Spring Cloud Gateway作为全局网关进行路由管理、流量染色和负载均衡，并通过全链路日志以及WebFlux回调配置, 支持**异步接口调用**
 。使用Mybatis-plus作为持久层技术。使用Apache Dubbo做高性能**远程服务调用**
。使用Nacos作为注册中心，完成服务注册与发现，通过各模块主要功能以及业务进行模块的合理划分。使用 Sentinel 进行**接口流量管理**
，通过限流策略有效保障服务的稳定性与安全性。同时使用 RocketMQ 对涉及第三方回调接口的业务进行 **异步通知**，实现服务链路解耦。

项目客户端依赖「**qiapi-clientSDK-spring-boot-starter**」已上传至Maven Central
Repository，用户可以选择在接口平台在线调用或者在项目中引入依赖并传入accessKey、secretKey进行API调用。

> 在线体验地址：[Qi-API](https://www.rickey-qiapi.cn/)
>
>项目前端开源地址：[Qi-API-frontend](https://github.com/rickey-c/qiapi-frontend)

## 项目背景

我的初衷是尽可能帮助和服务更多的用户和开发者，让他们更加方便快捷的获取他们想要的信息和功能。
接口平台可以帮助开发者快速接入一些常用的服务，从而提高他们的开发效率，比如随机头像，随机壁纸，随机动漫图片(二次元爱好者专用)
、实时天气等服务，他们是一些应用或者小程序常见的功能，所以提供这些接口可以帮助开发者更加方便地实现这些功能。这些接口也可以让用户在使用应用时获得更加全面的功能和服务，从而提高他们的用户体验。

## 系统架构

![image](https://github.com/rickey-c/qiapi-backend/blob/master/qiapi-doc/Architecture%20Diagram/Qi-API-Architecture%20Diagram.png)

## 技术栈

### 前端技术栈

+ 开发框架：React、Umi
+ 脚手架：Ant Design Pro
+ 组件库：Ant Design、Ant Design Components
+ 语法扩展：TypeScript、Less
+ 打包工具：Webpack
+ 代码规范：ESLint、StyleLint、Prettier

### 后端技术栈

+ 主语言：Java
+ 框架：SpringBoot 2.7.0、Mybatis-plus、Spring Cloud
+ 数据库：Mysql8.0
+ 中间件：RocketMQ、Redis
+ 注册中心：Nacos
+ 服务限流：Sentinel
+ 服务调用：Dubbo
+ 网关：Spring Cloud Gateway
+ 负载均衡：Spring cloud Loadbalancer

### 项目模块

+ qiapi-frontend ：为项目前端，前端项目启动具体看前端仓库的README.md文档
+ qiapi-common ：为公共封装类（如公共实体、公共常量，统一响应实体，统一异常处理）
+ qiapi-backend ：为接口管理平台，主要包括用户、接口相关的功能
+ qiapi-gateway ：为网关服务，涉及到**统一鉴权，流量染色，统一日志处理，接口统计，接口数据一致性处理**
+ qiapi-order ：为订单服务，主要涉及到接口的购买等
+ qiapi-third-party：为第三方服务，主要涉及到腾讯云短信、支付宝沙箱支付功能
+ qiapi-interface：为接口服务，提供可供调用的接口
+ qiapi-sdk：提供给开发者的SDK

### 功能模块

> 🌟 亮点功能 🚀 未来计划

+ 用户、管理员
  + 🌟登录注册：使用令牌桶算法实现手机短信(邮箱)接口的限流，保护下游服务（🚀）
  + 🌟登录：使用Cookie+分布式Session进行用户鉴权，单点登录
  + 个人主页，包括上传头像，显示密钥，重新生成ak,sk
  + 管理员：用户管理
  + 管理员：接口管理
  + 管理员：接口分析（定时任务）
+ 接口
  + 浏览接口信息
  + 🌟 数字签名校验接口调用权限
  + 🌟 SDK调用接口
  + 🌟 接口流控
  + 接口搜索 (🚀 )
  + 购买接口
  + 下载SDK
  + 用户上传自己的接口（🚀）
+ 订单
  + 创建订单
  + 订单超时回滚
  + 支付宝沙箱支付
