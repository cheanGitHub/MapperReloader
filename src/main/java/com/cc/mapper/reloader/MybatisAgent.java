package com.cc.mapper.reloader;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.ProtectionDomain;

/**
 * 修改SqlSessionFactoryBean
 */
public class MybatisAgent {

    public static void premain(String arg, Instrumentation instrumentation) throws Exception {
        System.out.println("agent premain , args = " + arg + ", instrumentation = " + instrumentation);

        // 注册
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String classFile, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) {
                return getBytesSqlSessionFactoryBean(loader, classFile, classBeingRedefined, protectionDomain, classFileBuffer);
            }
        });
    }

    /**
     * 修改org.mybatis.spring.SqlSessionFactoryBean#getObject，辅助实现热加载 mybatis mapper
     */
    private static byte[] getBytesSqlSessionFactoryBean(ClassLoader loader, String classFile, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) {
        if (classFile.equals("org/mybatis/spring/SqlSessionFactoryBean")) {
            System.out.println("loader = " + loader);
            System.out.println("classFile = " + classFile);
            System.out.println("classBeingRedefined = " + classBeingRedefined);
            System.out.println("protectionDomain = " + protectionDomain);
            System.out.println("classFileBuffer = " + classFileBuffer);

            try {
                ClassPool classPool = new ClassPool();
                classPool.insertClassPath(new LoaderClassPath(loader));

                CtClass ctClass = classPool.get(classFile.replace("/", "."));
                CtMethod ctMethod = ctClass.getDeclaredMethod("getObject");
                // ctMethod.insertBefore("System.out.println(\"after  set = \" + com.cc.agent.mapper.filelistener.MapperReloader.sqlSessionFactoryBean );");
                ctMethod.insertBefore("com.cc.agent.mapper.filelistener.MapperReloader.sqlSessionFactoryBean = this;");
                // ctMethod.insertBefore("System.out.println(\"before set = \" + com.cc.agent.mapper.filelistener.MapperReloader.sqlSessionFactoryBean );");

                // 返回重新编译的字节码
                return ctClass.toBytecode();
            } catch (Throwable e) {
                System.err.println("update SqlSessionFactoryBean error:");
                e.printStackTrace();
            }
        }
        return null;
    }
}
