# hotel-demo

一个基于 Spring Boot + Elasticsearch 的酒店搜索示例项目，支持酒店数据检索、条件筛选、自动补全、地理位置距离排序，以及通过 RabbitMQ 同步酒店索引数据。

## 项目功能

- 酒店全文搜索
- 品牌、城市、星级、价格区间筛选
- 酒店名称自动补全
- 按评分、价格排序
- 按用户当前位置进行距离排序
- 酒店新增/删除后同步更新 Elasticsearch 索引
- 前端地图展示酒店位置

## 技术栈

- Java 8
- Spring Boot 2.3.10
- MyBatis-Plus 3.4.2
- Elasticsearch 7.12.1
- RabbitMQ
- MySQL
- Vue.js
- 高德地图 JS API

## 项目结构

- `src/main/java/cn/itcast/hotel/web`：接口控制层
- `src/main/java/cn/itcast/hotel/service`：业务层
- `src/main/java/cn/itcast/hotel/mq`：MQ 消费监听
- `src/main/java/cn/itcast/hotel/pojo`：实体和返回对象
- `src/main/java/cn/itcast/hotel/constants`：索引和消息队列常量
- `src/main/resources/static`：前端页面和静态资源
- `src/tb_hotel.sql`：数据库表结构和示例数据
- `src/test/java/cn/itcast/hotel`：ES 索引、文档和搜索测试

## 环境要求

- JDK 8
- Maven 3.6+
- MySQL 5.7+
- Elasticsearch 7.12.1
- RabbitMQ
- 浏览器

## 配置说明

项目默认配置位于 [`src/main/resources/application.yaml`](src/main/resources/application.yaml)：

- 服务端口：`8089`
- MySQL：`jdbc:mysql://localhost:3306/data`
- 用户名：`root`
- 密码：`root`
- Elasticsearch：`http://192.168.150.101:9200`

前端地图页面还依赖高德地图 JS API，入口在 [`src/main/resources/static/index.html`](src/main/resources/static/index.html) 中，页面里已经写入了地图 key。

如果你的 ES 或 RabbitMQ 地址和当前配置不一致，需要同步修改：

- [`src/main/java/cn/itcast/hotel/HotelDemoApplication.java`](src/main/java/cn/itcast/hotel/HotelDemoApplication.java)
- [`src/main/resources/application.yaml`](src/main/resources/application.yaml)

## 数据库初始化

1. 在 MySQL 中创建数据库，例如 `data`
2. 执行 [`src/tb_hotel.sql`](src/tb_hotel.sql) 导入 `tb_hotel` 表和示例数据

导入后，表结构对应实体类 [`Hotel`](src/main/java/cn/itcast/hotel/pojo/Hotel.java)。

## Elasticsearch 索引

项目中的索引名固定为 `hotel`。

索引 mapping 模板定义在 [`HotelIndexConstants`](src/main/java/cn/itcast/hotel/constants/HotelIndexConstants.java) 中，主要字段包括：

- `name`：分词检索，复制到 `all`
- `brand`、`city`、`starName`：筛选字段
- `location`：地理位置字段
- `all`：全文检索字段

你可以通过测试类先创建索引：

- [`HotelIndexTest`](src/test/java/cn/itcast/hotel/HotelIndexTest.java)

## RabbitMQ 同步

项目使用 Topic Exchange 同步酒店索引变更：

- Exchange：`hotel.topic`
- 新增队列：`hotel.insert.queue`
- 删除队列：`hotel.delete.queue`
- 新增路由键：`hotel.insert`
- 删除路由键：`hotel.delete`

监听逻辑位于 [`HotelListener`](src/main/java/cn/itcast/hotel/mq/HotelListener.java)：

- 收到新增消息时，把酒店写入 Elasticsearch
- 收到删除消息时，从 Elasticsearch 删除对应文档

## 启动步骤

1. 启动 MySQL，并导入 `src/tb_hotel.sql`
2. 启动 Elasticsearch
3. 启动 RabbitMQ
4. 检查 `application.yaml` 中的数据库和 ES 地址
5. 启动 Spring Boot 应用

启动命令：

```bash
mvn spring-boot:run
```

或者先打包再运行：

```bash
mvn clean package
java -jar target/hotel-demo-0.0.1-SNAPSHOT.jar
```

## 接口说明

基础路径：`/hotel`

### 1. 酒店搜索

- `POST /hotel/list`

请求体示例：

```json
{
  "key": "上海 如家",
  "page": 1,
  "size": 5,
  "sortBy": "default",
  "brand": "",
  "city": "",
  "starName": "",
  "minPrice": 0,
  "maxPrice": 0,
  "location": ""
}
```

返回值：

- `total`：总条数
- `hotels`：酒店列表

### 2. 筛选项聚合

- `POST /hotel/filters`

作用：根据当前搜索条件，返回可选的品牌、城市、星级筛选项。

### 3. 自动补全

- `GET /hotel/suggestion?key=xxx`

作用：根据输入关键词返回自动补全建议。

## 索引文档字段

索引文档由 [`HotelDoc`](src/main/java/cn/itcast/hotel/pojo/HotelDoc.java) 负责封装，核心字段如下：

- `id`
- `name`
- `address`
- `price`
- `score`
- `brand`
- `city`
- `starName`
- `business`
- `location`
- `pic`
- `distance`
- `isAD`

其中：

- `distance` 用于前端展示当前位置与酒店的距离
- `isAD` 用于广告酒店加权排序

## 测试说明

项目提供了 3 类测试：

- [`HotelIndexTest`](src/test/java/cn/itcast/hotel/HotelIndexTest.java)：索引创建、删除、存在性检查
- [`HotelDocumentTest`](src/test/java/cn/itcast/hotel/HotelDocumentTest.java)：文档新增、查询、更新、删除、批量写入
- [`HotelSearchTest`](src/test/java/cn/itcast/hotel/HotelSearchTest.java)：搜索、过滤、分页、排序、高亮

注意：测试类里使用的 ES 地址和账号密码与主程序配置不同，运行前请按你的本地环境修改。

## 常见问题

### 1. 页面能打开，但没有搜索结果

- 检查 Elasticsearch 是否启动
- 检查索引 `hotel` 是否已经创建并导入数据
- 检查 `application.yaml` 中的 ES 地址是否正确

### 2. 启动后数据库连接失败

- 检查 MySQL 是否启动
- 检查 `data` 库是否存在
- 检查用户名和密码是否正确

### 3. 地图不显示或定位失败

- 检查高德地图 JS API key 是否有效
- 检查浏览器控制台是否有脚本加载错误

### 4. MQ 同步没有生效

- 检查 RabbitMQ 是否启动
- 检查交换机、队列和路由键是否一致
- 检查消息是否真的发送到了 `hotel.topic`

## 说明

这是一个酒店搜索教学/demo 项目，前端页面和接口是配套实现的，默认直接打开根页面即可使用。
