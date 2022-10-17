package org.nefure.fxscaffold.manager;

import javafx.scene.image.Image;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Objects;

/**
 * @author nefure
 * @date 2022/7/7 22:56
 */
public class ResourceManager {

    private static final String IMAGE_PATH = "data/imgs/";

    private static final String CSS_PATH = "data/style/";

    private Class<?> clazz;

    public ResourceManager(Class<?> clazz){
        this.clazz = clazz;
    }

    public ResourceManager(){

    }

    public URL getResource(String name){
        if (clazz != null){
            return clazz.getResource(name);
        }
        return ResourceManager.class.getClassLoader().getResource(name);
    }

    public InputStream getResourceAsStream(String name) {
        if (clazz != null){
            return clazz.getResourceAsStream(name);
        }
        return ResourceManager.class.getClassLoader().getResourceAsStream(name);
    }

    public Class<?> load(String name) throws ClassNotFoundException {
        return ResourceManager.class.getClassLoader().loadClass(name);
    }


    public Enumeration<URL> getResources(String name) throws IOException {
        return Objects.requireNonNullElse(clazz, ResourceManager.class).getClassLoader().getResources(name);
    }

    public Image getImage(String fileName){
        return new Image(getResourceAsStream(IMAGE_PATH + fileName));
    }

    public String getCss(String cssPath) {
        return getResource(CSS_PATH+cssPath).toExternalForm();
    }
}
