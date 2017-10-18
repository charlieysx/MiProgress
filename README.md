# MiProgress  

仿小米手环首页自定义view

## 效果:  
![gif](/raw/miprogress.gif)

## 使用:

* xml
```xml
<cb.miprogress.MiProgress
    android:id="@+id/miProgress"
    android:layout_width="300dp"
    android:layout_height="300dp"/>
```

* java
```java
miProgress = (MiProgress) findViewById(R.id.miProgress);
// 开始连接
miProgress.startLoading();
// 完成连接
// progress 为进度
miProgress.loadingComplete(progress);
```

