# 源码学习

[参考wiki](https://github.com/alipay/sofa-rpc/wiki/Structure-Intro)相关内容

重点：<br/>

1. 需要关注的包是core,core-impl,extension-impl
1. 阅读的入口位于`com.alipay.sofa.rpc.quickstart`包中
1. core包中是各种基本流程接口、消息、上下文、扩展接口等，很多的默认是实现是在coreImpl和extensionImpl中，默认的实现使用配置文件定义的在
`com/alipay/sofa/rpc/common/rpc-config-default.json`中，这个配置文件可以自行修改。
1. 由于使用了bolt做为底层的通信框架（基于netty），暂且对于这部分内容不会讨论（com.alipay.remoting.*）。



## 调用流程

客户端：


ConsumerConfig.refer():调用

服务端：



1. ProviderConfig.export()：发布
2. ProviderBootStrap=BootStraps.from(ProviderConfig):from方法使用自己的SPI加载默认的ProviderBootstrap
3. ProviderBootStrap.export():默认实现是DefaultProviderBootstrap，可以设置延迟加载时间，调用内部的doExport()
- 1. DefaultProviderBootstrap.doExport():中new ProviderProxyInvoker(ProviderConfig)
- 1. 在上面的构造器中会构造过滤器链，[Filter的调用过程](##Filter的调用过程)
- 1. 构造server并将上一步的调用链注册到server中（默认server是bolt）



----






## Filter的调用过程

这里只是主题流程，每个环节做了哪些细节，还是需要查看代码的。
首先相关代码的位置在`com.alipay.sofa.rpc.filter`包当中。

几个类介绍下：
1. AutoActive，注解在自定义的Filter上，表示是够默认启用这个Filter
1. Filter，自定义过滤器需要继承这个类
1. ExcludeFilter，字面意思，但是在代码中没有看到使用的地方，意思不是很明白，参照解析的地方理解这个类的意思，`FilterChain.parseExcludeFilter()`
1. FilterInvoker,包装了在调用链当中的参数
1. FilterChain，字面意思，除了提供链的调用方法invoke(调用FilterInvoker.invoke()方法)，还提供了构造链的方法，分别是buildProvideChain和buildConsumerChain，
1. ConsumerInvoker和ProviderInvoker都是FilterInvoker的子类，这两个类都是在调用链的最后

下面主要讲解构建过程

> 从Provider角度，在new ProviderProxyInvoker()对象时调用FilterChain.buildProviderChain(1，2)，注意这个方法的参数2是`new ProviderInvoker(providerConfig)`
这个对象处于整个链的最底层。

> 在buildProviderChain()中，先获取通过代码方式配置的Filter，然后根据excludeFilter排除掉，排序后创建FilterChain。

> new FilterChain()部分，请看下面代码：

```java

/**
 * 调用链，注这就是被FilterChain.invoke()调用的最外层的Filter
 */
private FilterInvoker invokerChain;

/**
 * 过滤器列表，从底至上排序，异步调用时使用
 */
private List<Filter>  loadedFilters;
    
/**
 * 构造执行链
 *
 * @param filters     包装过滤器列表
 * @param lastInvoker 最终过滤器
 * @param config      接口配置
 */
protected FilterChain(List<Filter> filters, FilterInvoker lastInvoker, AbstractInterfaceConfig config) {
    // 调用过程外面包装多层自定义filter
    // 前面的过滤器在最外层
    invokerChain = lastInvoker;
    if (CommonUtils.isNotEmpty(filters)) {
        //可见这个list是有序的
        loadedFilters = new ArrayList<Filter>();
        for (int i = filters.size() - 1; i >= 0; i--) {
            try {
                Filter filter = filters.get(i);
                if (filter.needToLoad(invokerChain)) {
                    //TODO 这里是需要注意的点，invokerChain是一层一层包装的，最外面的最后才会被调用
                    invokerChain = new FilterInvoker(filter, invokerChain, config);
                    // cache this for filter when async respond
                    loadedFilters.add(filter);
                }
            } catch (Exception e) {
                LOGGER.error("Error when build filter chain", e);
                throw new SofaRpcRuntimeException("Error when build filter chain", e);
            }
        }
    }
}
```

> 从Consumer角度，本质上是一样的，不同点在于使用的是ConsumerInvoker作为Filter的底层



1. ConsumerInvoker的作用：使用client发送数据给server
1. ProviderInvoker的作用：通过反射调用提供的方法


## 使用的算法

1. 在Compressor部分使用了google的snappy算法,该算法在很多下项目中使用如MapReduce,BigTable,RPC...，比较适合永久存储和实时传输等场景。使用的是github开源项目，`sofa-rpc-codec`
1. 


## 问题

1. 如何实现高可伸缩性。<br/>
1. 如何实现高容错性。<br/>
1. 如何实现负载均衡。<br/>
1. 如何实现流量转发。<br/>
1. 如何实现链路追踪。<br/>
1. 如何实现链路数据透传。<br/>
1. 如何实现故障剔除。<br/>
1. 如何实现自定义拓展。<br/>
使用java的SPI机制，但是没有是会用java原生提供的工具而是自己写的，在`com.alipay.sofa.rpc.ext.ExtensionLoader`中。由于是自己的解析方式，所以文件的格式
更加的灵活，能够满足自定义的需求。



