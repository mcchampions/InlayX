# InlayX - 一个简约的宝石镶嵌插件

InlayX 是一个简约的宝石镶嵌插件, 基于 Paper 1.21.11 开发

有着很高的自定义度: 下至宝石, 上至宝石类型以及显示效果均可自行配置

配置文件有着详细的注释, 不需要专门的文档就可以理解一切

## 环境要求

环境要求:
  - Paper 及其下游服务端(理论支持 Folia)
  - mc 1.21.3+(在 1.21.11 上通过测试)
  - Java 21+

依赖:
  - (可选) MythicMobs

## 命令

| 命令                               | 说明                                                         |
|------------------------------------|--------------------------------------------------------------|
| `/gem help`                        | 显示帮助信息                                                 |
| `/gem list`                        | 列出所有可用的宝石                                           |
| `/gem give <玩家> <宝石ID> [数量]` | 给予玩家指定的宝石                                           |
| `/gem addgem <宝石ID>`             | 直接向手持装备镶嵌指定宝石(管理员)                           |
| `/gem socket`                      | 打开宝石镶嵌界面                                             |
| `/gem extract [宝石ID]`            | 从手持装备中提取指定宝石, 不带参数时打开 GUi(失败时宝石碎裂) |
| `/gem removegem <宝石ID>`          | 直接移除手持装备上的指定宝石(管理员)                         |
| `/gem info`                        | 查看手持宝石或装备的宝石信息                                 |
| `/gem addslot <类型> [数量]`       | 为手持装备添加宝石槽位(管理员)                               |
| `/gem removeslot <类型> [数量]`    | 为手持装备移除宝石槽位(仅空槽位, 管理员)                     |
| `/gem reload`                      | 重新加载插件配置(管理员)                                     |

## 权限节点

| 权限                        | 说明                                              | 默认   |
|-----------------------------|---------------------------------------------------|--------|
| `inlayx.use`        | 基础使用权限(包含下方所有玩家命令权限)          | 所有人 |
| `inlayx.list`       | 查看宝石列表的权限                                | 所有人 |
| `inlayx.socket`     | 打开宝石镶嵌界面的权限                            | 所有人 |
| `inlayx.extract`    | 提取宝石的权限                                    | 所有人 |
| `inlayx.info`       | 查看宝石/装备信息的权限                           | 所有人 |
| `inlayx.give`       | 给予宝石的权限                                    | OP     |
| `inlayx.addgem`     | 直接向装备添加宝石的权限                          | OP     |
| `inlayx.addslot`    | 添加宝石槽位的权限                                | OP     |
| `inlayx.removeslot` | 移除宝石槽位的权限                                | OP     |
| `inlayx.removegem`  | 直接移除装备上宝石的权限                          | OP     |
| `inlayx.reload`     | 重载插件的权限                                    | OP     |
| `inlayx.admin`      | 管理员权限, 拥有所有子命令权限                    | OP     |
 
### 宝石镶嵌步骤

提供 3 种方式:
  - (可选) 手持宝石, 右键点击副手的装备可直接镶嵌
  - (可选) 玩家背包拖拽一键镶嵌
  - Gui 镶嵌

### 宝石提取步骤

1. 手持已镶嵌宝石的装备
2. 使用 `/gem extract` 命令打开提取 Gui 页面
3. 提取成功时宝石将返还给玩家, 装备上的宝石槽位恢复为空
4. 提取失败时宝石将碎裂消失, 不会返还

## 配置说明

见注释

## 原理说明

### 属性生效说明

本插件只负责添加对应的属性 Lore 进装备 Lore, 不负责属性生效

在使用本插件前你应该有一个自己的属性插件

### 宝石槽位识别

通过 `config.yml` 中的 `settings.socket.header` 为标识, 标记宝石槽位区的开始

#### 空槽位识别

通过 `config.yml` 中的 `settings.socket.display_pattern.empty` 为模板

将每种宝石类型通过该模板解析后的文本与 Lore 中当前行的文本进行对比, 若一致则判断其为该类型的宝石槽位

所以 `settings.socket.display_pattern.empty` 必须保证不同宝石类型渲染出的行互不相同

#### 已镶嵌槽位

通过 PDC 存储已镶嵌宝石的信息, 存储其宝石id及其在 宝石槽位区 中的相对位置

## 安装方法

1. 下载插件 JAR 文件(在 Releases, Actions 或其他渠道获取)
2. 将 JAR 文件放入服务器的 `plugins` 文件夹
3. 重启服务器或使用插件管理器加载插件
4. 配置 `config.yml` 文件(可选)
5. 使用 `/gem reload` 命令重新加载配置(可选)

## 开发

### 引入依赖 
[![](https://jitpack.io/v/mcchampions/InlayX.svg)](https://jitpack.io/#mcchampions/InlayX)
#### Maven

```xml
    <repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```

```xml
	<dependency>
	    <groupId>com.github.mcchampions</groupId>
	    <artifactId>InlayX</artifactId>
	    <version>24c3413256</version>
        <scope>provided</scope>
	</dependency>
```

#### Gradle

##### Groovy

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

```groovy
dependencies {
    compileOnly 'com.github.mcchampions:InlayX:24c3413256'
}
```

##### Kotlin

```kotlin
repositories {
    maven("https://jitpack.io")
}
```

```kotlin
dependencies {
    compileOnly("com.github.mcchampions:InlayX:24c3413256")
}
```

### API 入口

```java
import me.qscbm.inlayx.api.InlayXApi;
import me.qscbm.inlayx.gem.GemManager;
import org.bukkit.Bukkit;

InlayXApi api = Bukkit.getServicesManager().load(InlayXApi.class);
// 也可以用 InlayX.getApi() 获取
GemManager gemManager = api.getGemManager();
```

可监听的事件位于 `me.qscbm.inlayx.api.event` 包

### Javadoc [![Javadoc](https://img.shields.io/badge/JavaDoc-Online-green)](https://mcchampions.github.io/InlayX/javadoc/)

[Javadoc](https://mcchampions.github.io/InlayX/javadoc/)

### 克隆到本地

输入 `git clone https://github.com/mcchampions/InlayX.git` 即可

### 构建

```bash
# 跳过测试
mvn clean package -DskipTests
# 不跳过测试
mvn clean package
```

### 运行测试

```bash
mvn test
```

本项目使用 [MockBukkit](https://github.com/MockBukkit/MockBukkit) 编写单元测试

当你修改源码后运行测试不通过, 你应该确定你的改动是否理应改变逻辑, 与之对应的是你应该修改相对应的单元测试类

### 代码规范

当你进行源码修改后, 你应该运行 `mvn spotless:apply` 以自动应用项目编码规范

## 开源协议

本项目使用 MIT 协议开源
