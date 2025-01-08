package com.cc.mapper.reloader;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * mapper热加载
 */
@Service
public class MapperReloader {

    public static SqlSessionFactoryBean sqlSessionFactoryBean;

    public final Map<Object, Set<Field>> beanAndMapperFields = new HashMap<>();

    public void reloadMapper() {
        try {
            Method method = sqlSessionFactoryBean.getClass().getDeclaredMethod("buildSqlSessionFactory");
            method.setAccessible(true);
            SqlSessionFactory sqlSessionFactoryOwn = (SqlSessionFactory) method.invoke(sqlSessionFactoryBean);

            SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactoryOwn);
            Collection<Class<?>> mapperClasses = sqlSessionTemplate.getConfiguration().getMapperRegistry().getMappers();
            beanAndMapperFields.forEach((bean, mapperFields) -> {
                mapperFields.forEach(mapperField -> {
                    try {
                        Class<?> mapperClass = mapperField.getType();
                        if (mapperClasses.contains(mapperClass)) {
                            Object mapper = sqlSessionTemplate.getMapper(mapperClass);
                            mapperField.set(bean, mapper);
                        }
                    } catch (Throwable t) {
                        System.err.println("reload field" + mapperField.getName() + " error:");
                        t.printStackTrace();
                    }
                });
            });
            System.out.println("reload end");
        } catch (Throwable t) {
            System.err.println("reload error:");
            t.printStackTrace();
        }
    }
}
