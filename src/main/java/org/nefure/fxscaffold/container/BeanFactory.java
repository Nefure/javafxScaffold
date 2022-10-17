package org.nefure.fxscaffold.container;

import javafx.stage.Stage;
import org.nefure.fxscaffold.annotion.Component;
import org.nefure.fxscaffold.annotion.Config;
import org.nefure.fxscaffold.annotion.Resource;
import org.nefure.fxscaffold.manager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author nefure
 * @date 2022/10/10 19:44
 */
public enum BeanFactory {

    /**
     * 单例实现
     */
    INSTANCE;

    private final ConcurrentHashMap<Class<?>, Holder> singletonPool = new ConcurrentHashMap<>();

    private final HashMap<String, Class<?>> roster = new HashMap<>();

    private final ConcurrentHashMap<Class<?>, Object> embryoPool = new ConcurrentHashMap<>();

    private final Map<String, Object> ymlConfig;

    static class Holder {

        public volatile Object value;

        public Holder() {
        }

        public Holder(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        private final AtomicInteger state = new AtomicInteger(0);

        public boolean startInit() {
            return state.compareAndSet(0, 1);
        }

        public Holder finishInit() {
            state.set(2);
            return this;
        }

        public boolean isFinished() {
            return state.get() == 2;
        }
    }

    public <T> T getBean(Class<T> clazz) throws Exception {
        Component component = null;
        if (clazz.getAnnotation(Config.class) == null && (component = clazz.getAnnotation(Component.class)) == null) {
            throw new IllegalArgumentException("bean must be annotated by @Config or @Component !");
        }
        if (component != null) {
            String registerName = "".equals(component.value()) ? clazz.getSimpleName() : component.value();
            if (!clazz.equals(roster.get(registerName))) {
                throw new IllegalArgumentException("can't find bean for class " + clazz.getName());
            }
        }
        //此方法必须获取完全体
        //如果未获得创建权，则等待到实例完全初始化成功
        T instance = null;
        Exception exception = null;
        singletonPool.putIfAbsent(clazz, new Holder());
        Holder holder = singletonPool.get(clazz);
        if (holder.startInit()) {
            try {
                instance = clazz.cast(createBean(clazz));
            } catch (Exception e) {
                exception = e;
            } finally {
                holder.setValue(instance);
                holder.finishInit();
            }
        } else {
            while (!holder.isFinished()) {
                Thread.yield();
            }
            instance = clazz.cast(holder.getValue());
        }
        if (exception != null) {
            throw exception;
        }
        return instance;
    }

    public Class<?> getBeanClass(String name) {
        return roster.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) throws Exception {
        Class<T> clazz = (Class<T>) getBeanClass(name);
        if (clazz == null) {
            throw new IllegalArgumentException("bean '" + name + "' is not exist");
        }
        return getBean(clazz);
    }

    /**
     * 已加锁holder
     */
    private Object createBean(Class<?> clazz) {
        Object bean = null;
        Config config = clazz.getAnnotation(Config.class);
        try {
            if (config != null) {
                String prefix = config.value();
                StringBuilder builder = new StringBuilder(prefix.length());
                Map<String, Object> val = getMap(prefix, builder, -1, ymlConfig);
                if (val != null) {
                    Yaml yaml = new Yaml(new Constructor(clazz));
                    String yamlStr = yaml.dump(val);
                    bean = yaml.load(yamlStr);
                }
            } else {
                bean = createComponent(clazz);
            }
            if (bean == null) {
                bean = clazz.getConstructor().newInstance();
            }
        } catch (Exception e) {
            throw new IllegalStateException("创建bean失败！ bean:" + clazz.getName());
        }
        return bean;
    }

    /**
     * 调用时应该保证相同class只有一个线程调用此方法
     */
    private Object createComponent(Class<?> clazz) throws Exception {
        Object instance = clazz.getConstructor().newInstance();
        embryoPool.put(clazz, instance);

        injectFields(instance, clazz);
        return instance;
    }

    /**
     * 已加锁：holder
     */
    private void injectFields(Object instance, Class<?> clazz) throws Exception {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            Resource resource = field.getAnnotation(Resource.class);
            if (resource != null) {
                field.setAccessible(true);
                Class<?> type = field.getType();
                if (type.getAnnotation(Config.class) != null) {
                    field.set(instance, getBean(type));
                    continue;
                }

                String beanName = "".equals(resource.value()) ? field.getType().getSimpleName() : resource.value();
                Class<?> fieldClass = roster.get(beanName);
                if (fieldClass == null) {
                    throw new IllegalArgumentException("can't inject field of class " + clazz.getName() + " because there is no bean named " + beanName);
                }
                Object fieldObj = null;
                singletonPool.putIfAbsent(fieldClass, new Holder());
                Holder holder = singletonPool.get(fieldClass);
                if (holder.startInit()) {
                    try {
                        fieldObj = createComponent(fieldClass);
                    } catch (Exception e) {
                        fieldObj = e;
                    } finally {
                        holder.setValue(fieldObj);
                        holder.finishInit();
                    }
                } else {
                    while (fieldObj == null) {
                        fieldObj = embryoPool.get(fieldClass);
                        if (fieldObj == null) {
                            fieldObj = holder.getValue();
                        }
                        if (fieldObj == null) {
                            Thread.yield();
                        }
                    }
                }
                if (fieldObj instanceof Exception) {
                    throw (Exception) fieldObj;
                }
                field.set(instance, fieldObj);
            }
        }
    }

    private Map<String, Object> getMap(String prefix, StringBuilder builder, int idx, Map<String, Object> node) {
        Map<String, Object> rt;
        while (++idx < prefix.length()) {
            char ch = prefix.charAt(idx);
            if (ch == '.') {
                break;
            }
            builder.append(ch);
        }
        String key = builder.toString();
        Object val = node.get(key);
        builder.setLength(0);
        if (!(val instanceof Map)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> value = (Map<String, Object>) val;
        if (idx == prefix.length()) {
            node.remove(key);
            return value;
        }
        rt = getMap(prefix, builder, idx, value);
        if (value.isEmpty()) {
            node.remove(key);
        }
        return rt;
    }

    BeanFactory() {
        Logger log = LoggerFactory.getLogger(BeanFactory.class);

        roster.put(Stage.class.getSimpleName(), Stage.class);

        ResourceManager resourceManager = new ResourceManager();
        Yaml yaml = new Yaml();
        ymlConfig = yaml.load(resourceManager.getResourceAsStream("application.yml"));
        try {
            String basePackage = BeanFactory.class.getPackageName() + "," + ymlConfig.get("nefure-scanner-base-package");
            String[] packages = basePackage.split(",");
            for (String pack : packages) {
                String prefix = pack.substring(0,pack.lastIndexOf('.'));
                String path = pack.replace('.', '/');
                Enumeration<URL> packageUrl;
                try {
                    packageUrl = resourceManager.getResources(path);
                } catch (Exception e) {
                    log.warn("package [{}] not find !", path);
                    continue;
                }
                loadClasses(packageUrl, resourceManager, prefix,path, log);
            }
        } catch (Exception ignored) {
        }
    }

    private void loadClasses(Enumeration<URL> packageUrl, ResourceManager resourceManager, String prefix, String path, Logger log) {
        while (packageUrl.hasMoreElements()) {
            URL url = packageUrl.nextElement();
            String protocol = url.getProtocol();
            try {
                if ("jar".equals(protocol)) {
                    JarURLConnection conn = ((JarURLConnection) url.openConnection());
                    JarFile jarFile = conn.getJarFile();
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()){
                        JarEntry jarEntry = entries.nextElement();
                        String entryName = jarEntry.getName();
                        if (jarEntry.isDirectory() || !entryName.startsWith(path) || !entryName.endsWith(".class")){
                            continue;
                        }
                        String className = entryName.substring(0, entryName.lastIndexOf('.')).replace('/', '.');
                        registerClass(Class.forName(className));
                    }
                } else if ("file".equals(protocol)) {
                    loadClasses(new File(url.toURI()), resourceManager, prefix);
                }
            } catch (Exception e) {
                log.error("class load failed ,msg: {}", e.getMessage());
            }
        }
    }

    private void registerClass(Class<?> clazz) {
        Component component = clazz.getAnnotation(Component.class);
        if (component != null) {
            String name = component.value();
            if ("".equals(name)) {
                name = clazz.getSimpleName();
            }
            Class<?> aClass = roster.putIfAbsent(name, clazz);
            if (aClass != null && !clazz.equals(aClass)) {
                throw new IllegalArgumentException("exist two class[" + aClass.getName() + "," + clazz.getName() + "] with same beanName: {}");
            }
        }
    }

    public void registerBean(Class<?> clazz, Object bean, String name) {
        if (roster.containsKey(name)) {
            singletonPool.computeIfAbsent(clazz, key -> new Holder(bean).finishInit());
        }
    }

    /**
     * 注册beanName
     */
    private void loadClasses(File root, ResourceManager resourceManager, String path) {
        if (root.isFile()) {
            final String suffix = ".class";
            if (root.getName().endsWith(suffix)) {
                String fileName = root.getName();
                Class<?> clazz;
                String className = path + "." + fileName.substring(0, fileName.lastIndexOf('.'));
                try {
                    clazz = resourceManager.load(className);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("class loaded failed! class:" + className);
                }
                registerClass(clazz);
            }
        } else {
            File[] files = root.listFiles();
            if (files != null) {
                for (File file : files) {
                    loadClasses(file, resourceManager, path + "." + root.getName());
                }
            }
        }
    }
}
