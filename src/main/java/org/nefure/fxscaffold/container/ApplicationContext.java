package org.nefure.fxscaffold.container;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.nefure.fxscaffold.Entrance;
import org.nefure.fxscaffold.manager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;

/**
 * 获取资源时，总是以main方法所在路径为起点
 * @author nefure
 * @date 2022/10/13 11:43
 */
public class ApplicationContext {

    private final ResourceManager defaultManager;
    private final SceneFactory sceneFactory;
    private final BeanFactory FACTORY = BeanFactory.INSTANCE;

    private final Logger LOG = LoggerFactory.getLogger(ApplicationContext.class);


    private ApplicationContext(Class<?> context) {
        defaultManager = new ResourceManager(context);
        sceneFactory = getBean(SceneFactory.class);

        Callback<Class<?>, Object> callback = this::getBean;
        sceneFactory.init(context,callback);
    }

    public static ApplicationContext run(Class<? extends Entrance> root, String... args) {
        StackTraceElement[] stackTrace;
        stackTrace = new Exception().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if ("main".equals(element.getMethodName())) {
                try {
                    Class<?> clazz = Class.forName(element.getClassName());
                    ApplicationContext applicationContext = new ApplicationContext(clazz);
                    Entrance.setBeforeStart(stage -> {
                        applicationContext.FACTORY.registerBean(Stage.class,stage,Stage.class.getSimpleName());
                        return applicationContext;
                    });
                    Entrance.launch(root, args);
                    return applicationContext;
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        return null;
    }

    public <T> T getBean(Class<T> clazz) {
        T bean = null;
        try {
            bean = FACTORY.getBean(clazz);
        } catch (Exception e) {
            LOG.error("exceptions happen when getting bean : {},msg:{}", clazz.getName(), e.getMessage());
        }
        return bean;
    }

    public <T> T getBean(String name) {
        T bean = null;
        try {
            bean = FACTORY.getBean(name);
        } catch (Exception e) {
            LOG.error("exceptions happen when getting bean : {},msg:{}", name, e.getMessage());
        }
        return bean;
    }

    public Scene getScene(String fxmlPath,String... css) {
        return sceneFactory.getScene(fxmlPath,css);
    }

    public void clearSceneCaches(){
        sceneFactory.clearSceneCaches();
    }

    public URL getResource(String path){
        return defaultManager.getResource(path);
    }

    public InputStream getResourceAsStream(String path){
        return defaultManager.getResourceAsStream(path);
    }

    /**
     *
     * @param name 加载main方法所在路径下的 /data/imgs/name 文件
     */
    public Image loadImage(String name){
        return defaultManager.getImage(name);
    }
    /**
     *
     * @param fileName 加载main方法所在路径下的 /data/style/fileName 文件
     */
    public String loadCss(String fileName){
        return defaultManager.getCss(fileName);
    }

    public void setMainCss(String css){
        sceneFactory.setMainCss(css);
    }

}
