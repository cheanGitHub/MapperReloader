package com.cc.mapper.reloader;

import org.apache.ibatis.session.Configuration;
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
            Configuration configuration = (Configuration) getMember(sqlSessionFactoryBean, "configuration");
            ((Set)getMember(configuration, "loadedResources")).clear();
            ((Map)getMember(configuration, "mappedStatements")).clear();

            Method method = SqlSessionFactoryBean.class.getDeclaredMethod("buildSqlSessionFactory");
            method.setAccessible(true);
            SqlSessionFactory sqlSessionFactoryNew = (SqlSessionFactory) method.invoke(sqlSessionFactoryBean);

            SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactoryNew);
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

    private Object getMember(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
