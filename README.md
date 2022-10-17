# javafxScaffold

## 介绍

  此项目为了解决繁琐的对象创建与配置读取但不想使用庞大的spring而生，是java模块化项目。

提供了实用的单例bean的管理功能，仅使用了openjx的必要依赖与常用的slf4j、snakeyaml，可以方便地对组件进行管理，并提供了资源加载读取相关接口。

可以直接把这些代码加入到项目中稍作修改就能直接使用（因为代码文件很少），或把zip文件解压后添加到本地仓库中使用，

下面介绍的是后者的使用方法（添加到类路径或maven仓库中使用）

```xml
<dependency>
    <groupId>org.nefure</groupId>
    <artifactId>fxscaffold</artifactId>
    <version>1.0-ALPHA</version>
</dependency>
```

## 准备工作

注意：当前master分支使用的Java版本为17，依赖openfx版本为11.0.2，ikonli12.3.1，snakeyaml1.33

在模块化中使用时，需要在module-info.java添加如下内容

- snakeyaml、fxscaffold依赖

- 对fxscaffold的资源开发（fxml、css、配置文件等资源的目录）
- 视图类（Application子类）、控制类（controller）对javafx.fxml及fxcaffold的资源开放
- bean的目录（添加了fxscaffold的@component注解的类）对fxscaffold的资源开放

```java
module org.nefure.tools {

    requires transitive org.nefure.fxscaffold;
    requires org.yaml.snakeyaml;

    opens org.nefure.tools to javafx.fxml,org.nefure.fxscaffold,javafx.controls,javafx.graphics,javafx.base;

    opens org.nefure.tools.data.imgs to org.nefure.fxscaffold;
    opens org.nefure.tools.data.style to org.nefure.fxscaffold;

    exports org.nefure.tools;
    exports org.nefure.tools.view;
    exports org.nefure.tools.config;
    exports org.nefure.tools.controller;
    opens org.nefure.tools.controller to javafx.base, javafx.controls, javafx.fxml, javafx.graphics, org.nefure.fxscaffold;
}
```

## 使用

- 让视图类继承org.nefure.fxscaffold.Entrance 类并实现其onstart方法

  ```java
  public class MainMenu extends Entrance {
  
      @Override
      public void onStart(Stage stage,ApplicationContext context) {
          stage.setTitle("hello,fxscaffold");
          //注意：fxml文件读取目录为main函数所在的同级目录
          //如main函数类所在包为com.example.demo
          //将加载resource/com/example/demo/xxx.fxml文件
          //因此main方法所在函数请放在项目根目录下，如：com.example.demo包下
          stage.setScene(context.getScene("fxml文件名（不需要加.fxml后缀）"));
          stage.show();
      }
  }
  ```
  
- 在main函数中调用启动方法

  ```java
  public class Main{
  
      public static void main(String[] args){
          //如果当前类就是Entrance类的子类，可直接调用ApplicationContext(args);
          ApplicationContext.run(MainMenu.class, args);
      }
  
  }
  ```

  此时运行main函数即可使用

  ## 功能

  ### bean容器

  如果您需要，可以将单例的类交由容器管理，需要在resouce/application.yml文件中添加该类所在包配置：

  ```yml
  nefure-scanner-base-package:
    org.nefure.tools.controller,org.nefure.tools.service
  ```

  并在该类上标注@Component注解，注解的value为bean的名称，可以实现属性的依赖注入（需要提供无参构造）

  ```java
  //root为设置的bean名称，如果不填，将使用简单类名作为bean名称
  //比如此类如果不设置注解中的值，bean名称将为"RootController"
  @Component("root")
  public class RootController{
  
      //@Resource 将自动注入bean为属性值
      //容器中默认有注册一个名称为“Stage”的bean（在onstart方法中获得的Stage对象）
      //与一个名称为“SecenFactory”的bean，可用于直接根据fxml文件的名称获取scene对象
      @Resource
      private Stage stage;
  
      @FXML
      private Button treeBtn;
  
      @FXML
      private FlowPane root;
  
      @Resource
      private SceneFactory factory;
  
      public void openTreeModule() {
          Scene tree = factory.getScene("tree");
          stage.setScene(tree);
          System.out.println("onclick...");
      }
  
  }
  ```

- 还可以使用@Config注解来定义bean，此时该bean的所有属性值将从application.yml中读取，但如果类中存在的@Resource注解将没有任何效果，注意：每一个需要配置的属性都应该提供setter方法

  ```java
  //"stage"为yml文件中的前缀
  @Config("stage")
  public class StageConfig {
  
      private String title;
  
      private String rootFxml;
  
      private String icon;
  
      public String getTitle() {
          return title;
      }
  
      public String getRootFxml() {
          return rootFxml;
      }
  
      public String getIcon() {
          return icon;
      }
  
      public void setTitle(String title) {
          this.title = title;
      }
  
      public void setRootFxml(String rootFxml) {
          this.rootFxml = rootFxml;
      }
  
      public void setIcon(String icon) {
          this.icon = icon;
      }
  
  }
  ```

  对应application.yml配置：

  ```yml
  stage:
    icon: ico.png
    title: "  工具箱"
    rootFxml: root
  ```

  