package com.cc.mapper.reloader;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * mapper字段扫描
 */
@Component
public class MapperFieldBeanPostProcessor implements BeanPostProcessor {

    @Autowired
    private MapperReloader mapperReloader;

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        while (beanClass != Object.class) {
            registerMapperFields(bean, beanClass);
            beanClass = beanClass.getSuperclass();
        }

        return bean;
    }

    private void registerMapperFields(Object bean, Class beanClass) {
        Field[] fields = beanClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().isAnnotationPresent(Mapper.class)) {
                Object target = AopProxyUtils.getSingletonTarget(bean);
                if (target != null) {
                    field.setAccessible(true);
                    Set<Field> fieldSet = mapperReloader.beanAndMapperFields.getOrDefault(bean, new HashSet<>());
                    fieldSet.add(field);
                }
            }
        }
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}