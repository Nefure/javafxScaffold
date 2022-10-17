package org.nefure.fxscaffold.container;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.util.Callback;
import org.nefure.fxscaffold.annotion.Component;
import org.nefure.fxscaffold.manager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author nefure
 * @date 2022/10/14 12:03
 */
@Component
public class SceneFactory {


    private Callback<Class<?>, Object> controllerFactory;

    private String mainCss;

    private final ConcurrentHashMap<String, Scene> sceneCaches = new ConcurrentHashMap<>();

    private final Logger LOG = LoggerFactory.getLogger(SceneFactory.class);

    private ResourceManager defaultManager;


    protected void init(Class<?> context,Callback<Class<?>, Object> callback){
        defaultManager = new ResourceManager(context);
        controllerFactory = callback;
    }

    public Scene getScene(String fxmlPath, String... css) {

        Scene scene = sceneCaches.get(fxmlPath);
        if (scene != null) {
            return scene;
        }

        sceneCaches.computeIfAbsent(fxmlPath, key -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader();
                fxmlLoader.setControllerFactory(controllerFactory);
                Scene loading = new Scene(fxmlLoader.load(defaultManager.getResourceAsStream(fxmlPath+".fxml")));
                if (mainCss != null){
                    loading.getStylesheets().add(mainCss);
                }
                if (css != null){
                    loading.getStylesheets().addAll(css);
                }
                return loading;
            } catch (IOException e) {
                LOG.error("load fxml failed ! msg: {}", e.getMessage());
            }
            return null;
        });
        return sceneCaches.get(fxmlPath);
    }

    public void clearSceneCaches(){
        sceneCaches.clear();
    }

    public void setMainCss(String mainCss) {
        this.mainCss = mainCss;
    }
}
