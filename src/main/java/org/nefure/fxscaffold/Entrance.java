package org.nefure.fxscaffold;

import javafx.application.Application;
import javafx.stage.Stage;
import org.nefure.fxscaffold.container.ApplicationContext;

/**
 * @author 9528
 */
public abstract class Entrance extends Application {

    private static BeforeStartListener listener;

    public interface BeforeStartListener{
        /**
         * 在javafx调用start时，实际执行启动逻辑前调用
         * 供容器调用并返回容器
         * @param stage javafx 传来的窗口
         * @return ApplicationContext 当前bean容器
         */
        ApplicationContext beforeStart(Stage stage);
    }

    @Override
    public void start(Stage primaryStage) {
        onStart(primaryStage,listener.beforeStart(primaryStage));
        listener = null;
    }

    /**
     * 实现具体是实现逻辑
     * @param primaryStage javafx传来的窗口
     * @param applicationContext 容器（此方法后将自动删除私有的容器属性）
     */
    public abstract void onStart(Stage primaryStage, ApplicationContext applicationContext);

    public static void setBeforeStart(BeforeStartListener listener) {
        Entrance.listener = listener;
    }
}