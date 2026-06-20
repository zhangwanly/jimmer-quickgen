# jimmer-quickgen

数据库逆向工程工具 -- 从已有数据库 schema 自动生成 [Jimmer ORM](https://babyfish-ct.github.io/jimmer-doc/) 实体 Java 接口。

通过 JDBC 读取数据库元数据（表、列、主键），基于命名约定推断表间关联关系，生成带有完整 Jimmer 注解的 `@Entity` 接口源文件，免去手写实体类的工作。

## 功能特性

- **4 阶段流水线**：方言检测 -> 数据库内省 -> Schema 分析 -> 代码生成
- **6 种关联类型自动识别**：`@ManyToOne`、`@OneToMany`、`@OneToOne`（双向）、`@ManyToMany`（双向）
- **BaseEntity 自动提取**：将 `id`、`created_at`、`updated_at` 等公共列抽取到 `@MappedSuperclass` 基类
- **关联表自动检测**：复合主键且所有列为外键引用的表自动识别为关联表，生成 `@ManyToMany` 关联而非实体
- **自关联支持**：`parent_id`、`root_id` 等列自动生成 `parent()` / `children()` 自引用关联
- **命名约定推断关联**：通过 `{table}_id` 列名模式推断表间关系，无需真实外键约束
- **多数据库方言**：支持 MySQL、PostgreSQL，自动检测并应用方言特定类型映射
- **三层类型映射**：用户自定义 > 方言覆盖 > 默认映射 > String 兜底
- **可插拔策略**：数据库内省、BaseEntity 提取、关联表检测、关联解析、Schema 验证均为策略接口，可扩展替换

## 快速开始

### 最小示例

```java
import io.github.zhangwanly.jimmer.quickgen.QuickGen;
import config.io.github.zhangwanly.jimmer.quickgen.QuickGenConfig;
import javax.sql.DataSource;
import java.nio.file.Path;

// 1. 准备数据源
DataSource dataSource = ...; // 你的 JDBC DataSource

// 2. 配置生成参数
QuickGenConfig config = QuickGenConfig.builder()
    .basePackage("com.example.entity")
    .outputDir(Path.of("src/main/generated"))
    .build();

// 3. 执行生成
QuickGen.generate(dataSource, config);
```

### 自定义配置

```java
import org.babyfish.jimmer.sql.GenerationType;

QuickGenConfig config = QuickGenConfig.builder()
    .basePackage("com.example.entity")
    .outputDir(Path.of("src/main/generated"))
    .generationType(GenerationType.IDENTITY)
    .baseEntityConfig(b -> b
        .enabled(true)
        .name("BaseEntity")
        .columnPatterns("id", "create_time", "update_time", "is_deleted"))
    .selfRefPatterns(List.of("parent_id", "root_id", "pid"))
    .joinTableConfig(new JoinTableConfig(2, false))
    .schemaValidatorConfig(b -> b.addNonFkIdColumns("device_id", "tenant_id"))
    .tableRefOverride("order_info", "user_id", "user_info")
    .build();

QuickGen.generate(dataSource, config);
```

### 自定义类型映射

```java
TypeMappingRegistry registry = TypeMappingRegistry.defaults()
    .register("JSON", String.class)
    .register("UUID", java.util.UUID.class);

QuickGenConfig config = QuickGenConfig.builder()
    .basePackage("com.example.entity")
    .typeMappingRegistry(registry)
    .build();
```

## 配置参考

### QuickGenConfig.Builder

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `basePackage` | `String` | `"com.example.entity"` | 生成实体的 Java 包名 |
| `outputDir` | `Path` | `Path.of("generated")` | 输出目录 |
| `generationType` | `GenerationType` | `USER` | Jimmer 主键生成策略 |
| `baseEntityConfig` | `BaseEntityConfig` | 见下方 | BaseEntity 提取配置 |
| `typeMappingRegistry` | `TypeMappingRegistry` | 默认映射 | SQL 类型到 Java 类型的映射 |
| `selfRefPatterns` | `List<String>` | `["parent_id", "root_id"]` | 自关联列名模式 |
| `joinTableConfig` | `JoinTableConfig` | 见下方 | 关联表检测配置 |
| `schemaValidatorConfig` | `SchemaValidatorConfig` | 见下方 | Schema 验证配置（非外键 `_id` 列白名单） |
| `tableRefOverride` | 见下方 | 空 | 存量库外键引用覆盖（可多次调用） |

### BaseEntityConfig

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enabled` | `boolean` | `true` | 是否提取 BaseEntity |
| `name` | `String` | `"BaseEntity"` | BaseEntity 接口名称 |
| `columnPatterns` | `List<String>` | `["id", "created_at", "updated_at"]` | 需提取的列名模式，支持精确匹配（忽略大小写）或正则表达式（`^...$` 格式） |

### JoinTableConfig

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `requiredForeignKeyCount` | `int` | `2` | 判定为关联表所需的最小外键列数 |
| `allowExtraNonFkColumns` | `boolean` | `false` | 关联表是否允许存在非外键列 |

### SchemaValidatorConfig

Schema 验证器检测以 `_id` 结尾但实际不是外键的列（如 `open_id`、`session_id`），对未解析的列生成警告报告。

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nonFkIdColumns` | `List<String>` | `open_id`, `union_id`, `session_id`, `request_id`, `trace_id`, `app_id`, `correlation_id` | 非外键 `_id` 列白名单 |

```java
// 追加自定义列到白名单
.schemaValidatorConfig(b -> b.addNonFkIdColumns("device_id", "tenant_id"))

// 完全替换默认白名单
.schemaValidatorConfig(b -> b.customNonFkIdColumns(List.of("open_id", "my_custom_id")))
```

### TableRefOverride

存量库中列名不遵循严格的 `{table}_id` 约定时，需要手动指定外键引用关系。

例如 `order_info.user_id` 实际引用 `user_info` 表而非 `user` 表，按默认约定会从 `user_id` 提取出引用表 `user`，导致关联错误。通过 `tableRefOverride` 三元组 `(tableName, columnName, actualRefTable)` 修正：

```java
QuickGenConfig config = QuickGenConfig.builder()
    .basePackage("com.example.entity")
    .tableRefOverride("order_info", "user_id", "user_info")
    .tableRefOverride("coupon_range", "range_id", "coupon_range_type")
    .tableRefOverride("promotion", "shop_id", "merchant_shop")
    .build();
```

| 参数 | 说明 | 示例 |
|------|------|------|
| `tableName` | 拥有外键列的表名 | `"order_info"` |
| `columnName` | 外键列名 | `"user_id"` |
| `actualRefTable` | 实际引用的目标表名 | `"user_info"` |

> 所有参数自动转为小写，无需手动处理大小写。可多次调用 `tableRefOverride()` 添加多个覆盖规则。

### TypeMappingRegistry 类型解析优先级

1. **列名模式匹配**：`is_*` 等模式命中的列直接映射为 `boolean`
2. **用户自定义映射**：通过 `register(dbType, javaClass)` 注册
3. **方言映射**：自动检测的数据库方言类型映射
4. **默认映射**：内置的 30+ 种标准 SQL 类型映射
5. **兜底**：`String`

## 生成输出

### BaseEntity（可选）

当 `baseEntityConfig.enabled = true` 且存在匹配公共列时，生成 `@MappedSuperclass` 接口：

```java
@MappedSuperclass
public interface BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.USER)
    long id();

    @Nullable
    @Column(name = "created_at")
    LocalDateTime createdAt();

    @Nullable
    @Column(name = "updated_at")
    LocalDateTime updatedAt();
}
```

### 实体接口

每个非关联表生成一个 `@Entity` 接口，包含：

- 标量属性方法（带 `@Id`、`@Column`、`@Nullable` 等注解）
- 关联属性方法（`@ManyToOne`、`@OneToMany`、`@OneToOne`、`@ManyToMany`）

```java
@Entity
@Table(name = "product")
public interface Product extends BaseEntity {
    String name();

    @Nullable
    String description();

    @Nullable
    @ManyToOne
    @JoinColumn(name = "category_id", foreignKeyType = ForeignKeyType.FAKE)
    Category category();

    @OneToMany(mappedBy = "product")
    List<ProductSku> skus();
}
```

> 所有 `@JoinColumn` 使用 `foreignKeyType = ForeignKeyType.FAKE`，表明关联关系基于命名约定推断，而非真实数据库外键约束。FAKE 外键要求属性始终标记 `@Nullable`；当数据库列 NOT NULL 时，通过 `@ManyToOne(inputNotNull = true)` 在保存时强制非空校验。

> 当属性名与 JavaBean 规范存在歧义时，会自动添加 `@Column` 显式指定列名。典型场景：① boolean `is` 前缀（如 `is_deleted` → `boolean isDeleted()`，Jimmer 会剥离 `is` 推断为 `deleted`）；② `get`/`set` 前缀（如 `get_type` → `getType()`，Jimmer 会剥离 `get` 推断为 `type`）；③ 连续大写缩写词（如 `serverURL`，Jimmer 可能推断为 `server_u_r_l`）。

### 关联表处理

被识别为关联表的表（如 `user_role_mapping`）不会生成实体文件，而是在关联的两个实体上分别生成 `@ManyToMany` 方法。

## 关联解析规则

| 条件 | 生成结果 |
|------|----------|
| 列 `{table}_id` 且表 `{table}` 存在 | 当前实体 `@ManyToOne` + 目标实体 `@OneToMany(mappedBy)` |
| 列 `{table}_id` 且为唯一主键 | 当前实体 `@OneToOne` + 目标实体 `@OneToOne(mappedBy)` |
| 列匹配 `selfRefPatterns`（如 `parent_id`） | 当前实体 `@ManyToOne parent()` + `@OneToMany children()` |
| 关联表检测命中 | 字母序靠前的表 `@ManyToMany` + `@JoinTable`，靠后的表 `@ManyToMany(mappedBy)` |

## 构建与测试

```bash
# 编译并运行所有测试
./gradlew build

# 仅运行测试
./gradlew test
```

### 发布
```bash
./gradlew clean :lib:test :lib:publishMavenPublicationToSonatypeRepository
```

## 项目结构

```
jimmer-quickgen/
└── lib/
    └── src/
        ├── main/java/io/github/zhangwanly/jimmer/quickgen
        │   ├── QuickGen.java              # 入口门面，编排完整流水线
        │   ├── config/                    # 配置层
        │   │   ├── QuickGenConfig         #   主配置（Builder 模式）
        │   │   ├── TypeMappingRegistry    #   SQL→Java 三层类型映射
        │   │   ├── BaseEntityConfig       #   BaseEntity 提取配置
        │   │   ├── JoinTableConfig        #   关联表检测配置
        │   │   └── SchemaValidatorConfig  #   非外键 _id 列白名单
        │   ├── db/                        # 数据库内省层
        │   │   ├── DatabaseIntrospectionStrategy  # 策略接口
        │   │   ├── JdbcMetadataIntrospector       # JDBC DatabaseMetaData 实现
        │   │   ├── TableModel / ColumnModel       # 表/列数据记录
        │   │   └── DatabaseIntrospector           # 外观类
        │   ├── analysis/                  # Schema 分析层
        │   │   ├── SchemaAnalyzer                 # 分析流水线编排
        │   │   ├── BaseEntityExtractionStrategy   # BaseEntity 提取策略
        │   │   ├── JoinTableDetectionStrategy     # 关联表检测策略
        │   │   ├── AssociationResolverStrategy    # 关联解析策略
        │   │   ├── SchemaValidationStrategy       # Schema 验证策略
        │   │   ├── ConventionAssociationResolver  # 基于命名约定的关联解析（3 阶段）
        │   │   ├── NamingConventions              # snake_case ↔ CamelCase 工具
        │   │   └── AssociationModel               # sealed interface + 6 种关联 record
        │   ├── codegen/                   # 代码生成层
        │   │   ├── JimmerCodeGenerator            # 编排分析→JavaFile 生成
        │   │   ├── EntityBuilder                  # @Entity TypeSpec 构建
        │   │   ├── BaseEntityBuilder              # @MappedSuperclass 构建
        │   │   ├── AnnotationFactory              # Jimmer 注解集中工厂
        │   │   └── PropertyMethodBuilder          # 标量属性方法构建
        │   ├── dialect/                   # 数据库方言层
        │   │   ├── Dialect                        # 方言接口（纯契约）
        │   │   ├── DialectFactory                 # 方言检测工厂
        │   │   ├── MysqlDialect / PostgresDialect # 方言实现
        │   │   └── GenericDialect                 # 通用兜底方言
        │   └── output/                    # 文件输出层
        │       ├── OutputWriter                   # JavaFile 文件 I/O
        │       ├── SourcePostProcessor            # 源码后处理（注解压缩 + import 分组）
        │       └── WarningReportWriter            # Schema 警告 Markdown 报告
        └── test/                          # 单元测试与集成测试
```
