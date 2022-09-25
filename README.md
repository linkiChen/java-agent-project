# Java探针技术
javaAgent是一个JVM插件，它能够利用jvm提 Instrumentation API(Java 1.5开始提供)实现字节码修改的功能。Agent分为2种：1.主程序运行前的Agent，2.主程序运行后的Agent(Jdk 1.6新增)。
JavaAgent常用于代码热更新，AOP，JVM监控等功能。

## 主程序运行前的Agent
### 1.编写探针程序
- 名称必须为premain
- 参数可以是premain(String agentOps,Instrumentation inst)，也可以是premain(String agentOps)
- 优先执行premain(String agentOps)

```
public class AgentTest{

    // 该方法在main方法之前运行，与main方法运行在同一个jvm中
    // 并被同一个System ClassLoader装载
    // 被统一的案例策略(security policy) 和上下文(context)管理
    public static void premain(String agentOps, Instrumentation inst) {
        System.out.println(" premain 两参数方法 执行... ");
        System.out.println(" 参数:" + agentOps);
    }

    // 如果不存在 premain(String agentOps, Instrumentation inst)
    // 则会执行premain(String agentOps)
    public static void premain(String agentOps) {
        System.out.println(" premain 一参数方法 执行... ");
        System.out.println(" 参数:" + agentOps);
    }
}
```
### 2.在MANIFEST.MF配置环境参数
- 普通项目配置
```
Manifest-Version: 1.0
Premain-Class: com.agent.AgentTest
Can-redefine-Class: true
Can-retransform-Class: true
```
- 属性说明
```
Premain-Class: 指定代理类
Agent-Class: 指定代理类
Boot-Class-Path: 指定bootstrap类加载器的搜索路径，在平台指定的查找路径失败的时候生效，可选
Can-redefine-Class: 是否需要重新定义所有类，默认为false，可选
Can-retransform-Class: 是否需要retransform，默认为false，可选
```
- maven项目这样配置，在manifestEntries里面的元素与普通项目对应
```
<build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <version>2.4</version>
        <configuration>
          <archive>
            <manifest>
              <addClasspath>true</addClasspath>
            </manifest>
            <manifestEntries>
              <Premain-Class>com.agent.AgentTest</Premain-Class>
              <Can-Redefine-Classes>true</Can-Redefine-Classes>
              <Can-retransform-Classes>true</Can-retransform-Classes>
              <Manifest-Version>true</Manifest-Version>
            </manifestEntries>
          </archive>
        </configuration>
      </plugin>
    </plugins>
  </build>
```

### 3.使用探针程序
- 打包探针项目为preAgent.jar
- 启动主函数的时候添加jvm参数
- params就对应premain函数中的agentOps参数
```
-javaagent: 路径/preAgent.jar=params
```
## 主程序这后运行的Agent
<p>启动前探针使用方式比较局限，而且每次探针更改的时候，都需要重启应用，而主程序这后的探针程序就可以直接连接到已经启动的jvm中。</p>
可以实现例如动态替换类，查看加载类信息的一些功能

### 实现一个指定动态类替换的功能
- 实现一个指定类，指定class文件动态替换，实现动态日志增加的功能
### 1.编写探针程序
- 主程序后的探针程序名称必须为agentmain
- 通过agentOps参数将需要替换的类名和Class类文件路径传递过来
- 然后获取全部加载的Class类，通过类名筛选出来要替换的Class
- 通过传递进来的Class类文件路径加载数据
- 通过redefineClasses进行类文件的热替换
- 使用redefineClasses函数必须将Can-Redefine-Classes环境变量设置为true

```
public static void agentmain(String agentOps,Instrumentation inst) {
    System.out.println("agentmain方法开始执行...");
    
    String[] split = agentOps.split(",");
    String className = split[0];
    String classFile = split[1];

    System.out.println("替换类为:" + className);

    Class<?> redefineClass = null;
    Class<?>[]  allLoadedClasses = inst.getAllLoadedClasses();
    for(Class<?> clazz: allLoadedClasses) {
        if(className.equals(clazz.getCanonicalName())) {
            redefineClass = clazz;
        }
    }

    if(redefineClass == null) {
            return;
    }

    // 热替换
    try {
        byte[] classBytes = Files.readAllBytes(Paths.get(classFile));
        ClassDefinition classDefinition = new ClassDefinition(redefineClass,classBytes);
        inst.redefineClass(classDefinition);
    } catch(ClassNotFoundException | UnmodifiableClassException | IOException e){
        e.printStackTrace();
    }
    System.out.println("agentmain方法执行结束...");
}
```

### 2.在MANIFEST.MF配置环境参数
- 普通项目配置
```
Manifest-Version: 1.0
Agent-Class: com.agent.AgentDynamic
Can-Refefine-Classes: true
Can-Retransform-Classes: true
```

- maven项目这样配置
```
<build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <version>2.4</version>
        <configuration>
          <archive>
            <manifest>
              <addClasspath>true</addClasspath>
            </manifest>
            <manifestEntries>
              <Agent-Class>com.agent.AgentDynamic</Agent-Class>
              <Can-Redefine-Classes>true</Can-Redefine-Classes>
              <Can-retransform-Classes>true</Can-retransform-Classes>
              <Manifest-Version>true</Manifest-Version>
            </manifestEntries>
          </archive>
        </configuration>
      </plugin>
    </plugins>
  </build>
```

### 3.使用探针程序
- 先使用jps指令或者ps -aus|grep java找到目标 JVM线程ID
- 编写使用探针程序
  - 将目标线程attach到VirtualMachine
  - 配置参数agentOps，加载探针，此时就会执行探针中的程序
  - 通过VirtualMachine还能获取到陈怡蓉jvm的系统参数，以及探针的一些参数

```
public static void main(String[] args) throws IOException, AttachNotSupportedException {
    VirtualMachine target = VirtualMachine.attach("96003"); // 目标vm线程ID

    String agentOps = "com.api.rcode.controller.HomeController,/Users/workspace/target/classes/com/api/rcode/controller/HomeController.class";
    target.loadAgent("/Users/workspace/target/agent-1.0-SNAPSHOT.jar")"";

    Properties agentPreperties = target.getAgentProperties();
    System.out.println(agentPreperties);

    Properties agentPreperties = target.getSystemProperties();
    System.out.println(agentPreperties);

    target.detach(); 
}
```