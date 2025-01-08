# MapperReloader

项目启动后，若有修改mybatis mapper.xml文件，需要热更新，以避免重新启动整个项目，提升效率
mybatis mapper auto reload when mapper xml file is changed

1. 背景知识：
<br/>项目启动时：
<br/>&ensp;&ensp;Mapper注解的接口，会生成MapperProxy代理对象
<br/>&ensp;&ensp;自动配置类MybatisAutoConfiguration：
<br/>&ensp;&ensp;&ensp;&ensp;通过SqlSessionFactoryBean加载配置并调用其getObject，创建复用的DefaultSqlSessionFactory实例
<br/>&ensp;&ensp;&ensp;&ensp;通过复用的DefaultSqlSessionFactory实例，创建复用的SqlSessionTemplate实例
<br/>查询数据库时：
<br/>&ensp;&ensp;业务代码，通过MapperProxy代理对象，调用MapperMethod --> SqlSessionTemplate --> SqlSessionInterceptor代理：
<br/>&ensp;&ensp;&ensp;&ensp;调用SqlSessionUtils#getSqlSession，调用复用的DefaultSqlSessionFactory实例的openSession方法，得到新的DefaultSqlSession实例
<br/>&ensp;&ensp;&ensp;&ensp;反射调用DefaultSqlSession的selectOne等具体方法查询数据库
2. 实现方式：
<br/>MapperFieldBeanPostProcessor：
<br/>&ensp;&ensp;扫描Bean，把Mapper类型的字段(被@Mapper注解)，缓存到MapperReloader.beanAndMapperFields（类型Map<Object, Set<Field>>）
<br/>agent：
<br/>&ensp;&ensp;静态agent，运行时加-javaagent:/path/to/mapper-reloader.jar
<br/>&ensp;&ensp;premain方法中：修改org/mybatis/spring/SqlSessionFactoryBean的代码：在getObject()方法中，把MapperReloader.sqlSessionFactoryBean赋值为this
<br/>FileMonitor
<br/>&ensp;&ensp;通过MybatisProperties/MapperReloader.beanAndMapperField找到Mapper的xml文件路径
<br/>&ensp;&ensp;通过FileListener（继承FileAlterationListenerAdaptor）实现文件变化监听，变化时调用MapperReloader.reloadMapper()更新业务代码中注入的mapper字段
<br/>MapperReloader
<br/>&ensp;&ensp;reloadMapper：反射调用buildSqlSessionFactory()重新加载配置(包括xml)，构建新SqlSessionFactory，再获取新Mapper代理对象，再通过beanAndMapperField获取Mapper字段，最终反射更新该字段
