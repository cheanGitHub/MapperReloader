package com.cc.mapper.reloader;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.reflect.Field;

/**
 * mapper文件监听
 */
@Component
public class FileMonitor {

    @Autowired
    private MybatisProperties mybatisProperties;
    @Autowired
    private MapperReloader mapperReloader;
    @Autowired
    private FileListener fileListener;

    @Value("${mapper.reload.interval:1000}")
    private int reloadInterval;

    private FileAlterationMonitor monitor;

    /**
     * mapper文件路径获取和监听
     */
    @PostConstruct
    public void init() throws Exception {
        String xmlPath;
        Resource[] resources = mybatisProperties.resolveMapperLocations();
        if (resources != null && resources.length > 0) {
            xmlPath = resources[0].getFile().getParentFile().getAbsolutePath();
        } else {
            Class mapperType = ((Field) mapperReloader.beanAndMapperFields.values().toArray()[0]).getType();
            String xmlResource = mapperType.getName().replace('.', '/') + ".xml";
            xmlPath = mapperType.getResource("/" + xmlResource).getFile();
            xmlPath = xmlPath.substring(1, xmlPath.lastIndexOf("/"));
        }

        monitor(xmlPath, fileListener);
        start();
    }

    /**
     * 添加文件监听
     *
     * @param path     文件路径
     * @param listener 文件监听器
     */
    public void monitor(String path, FileAlterationListener listener) {
        FileAlterationObserver observer = new FileAlterationObserver(new File(path));
        observer.addListener(listener);
        monitor = new FileAlterationMonitor(reloadInterval);
        monitor.addObserver(observer);
    }

    public void stop() throws Exception {
        monitor.stop();
    }

    public void start() throws Exception {
        monitor.start();
    }
}
