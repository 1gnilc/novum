# 国际化落地方案

## 1. 设计边界

本方案按当前项目的 Spring Boot 3.2、MyBatis-Plus、MySQL、Redis 和 Vue 3/Vben Admin 结构设计。

| 内容 | 来源 | 最终显示方 |
| --- | --- | --- |
| 页面、按钮、表单标题 | 前端 JSON 语言包 | 前端 |
| 后端响应 message/error、校验文案、后端硬编码提示 | Spring `MessageSource` 静态资源文件 | 后端生成文本，前端展示 |
| 菜单标题、后台可配置业务文案 | `sys_i18n` 数据表 | 前端加载 key/语言包后通过 `vue-i18n` 显示 |

菜单翻译虽然存储在数据库，但后端不把菜单标题拼成页面文本。后端返回 key 或按分类返回语言包，前端负责合并和显示。

`R`、`Preconditions` 和现有响应结构不修改。需要国际化的调用方显式调用 `I18nMessageService`，然后把得到的字符串传给现有方法。

## 2. 消息身份与分类

动态国际化消息由 Message Key 全局标识。分类是消息的必填、可修改属性，用于管理查询、筛选和限定运行时语言包的加载范围，不参与身份判定。

当前分类值由后端常量维护：

```text
default
admin
```

`POST /sys/i18n-message/categories` 按上述顺序返回字符串数组。分类值不区分内部代码和页面名称，也不做国际化映射；数据库、接口和页面直接使用 `default`、`admin`。当前不新增分类表或分类管理页面。

新增消息时分类必填，默认选择 `/categories` 返回的第一项 `default`；分类列表为空时前端保持空值并禁止提交。已有消息允许修改分类，修改表示把同一个 Message Key 及其全部语言值移动到另一个运行时语言包。

运行端通过路径显式指定加载分类：

```http
POST /sys/i18n-message/bundle/admin
```

管理端固定调用 `/bundle/admin`。不再使用 `X-Client` 或 `X-Category`，通用请求拦截器也不为国际化增加请求头。分类不是身份凭证或权限边界，所有接口仍遵循现有认证和 RBAC 权限控制。

## 3. 数据库表

只使用一张配置记录表，不建立语言表或 key 主表。

```sql
CREATE TABLE sys_i18n (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    category varchar(64) NOT NULL COMMENT '消息分类，例如 default、admin',
    message_key varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin
        NOT NULL COMMENT '国际化 key，大小写敏感',
    locale varchar(20) NOT NULL COMMENT '语言代码，例如 zh-CN',
    i18n_value text NOT NULL COMMENT '翻译值',
    create_time datetime NOT NULL,
    update_time datetime DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_key_locale (message_key, locale),
    KEY idx_category_message_key (category, message_key),
    KEY idx_category_locale_key (category, locale, message_key)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='动态国际化消息';
```

最核心的字段仍然只有：

```text
category + message_key + locale + i18n_value
```

领域上的消息身份是 `message_key`。由于一种语言使用一条物理记录，数据库以 `(message_key, locale)` 区分同一消息的不同语言行；同一个 Message Key 的全部语言行必须具有相同分类，由保存事务统一维护。升级旧客户端数据时，`client = admin` 优先保留并迁移为 `category = admin`，其他客户端迁移到 `default`；同一 Key 和语言存在多份旧值时优先保留 `admin` 值，其余情况按最早记录确定。重跑种子脚本补齐缺失语言时沿用该 Key 的现有分类；只有整个 Key 不存在时才默认写入 `admin`。

表只是记录表，不强制约束业务表：

- key 被菜单或业务表引用后仍然允许修改。
- 删除 key 前不检查引用关系。
- `sys_i18n` 不使用逻辑删除；删除操作物理删除当前 Message Key 的全部语言记录。
- 业务表只保存字符串 key，不建立外键。
- 删除后是否出现页面显示 key，由业务方自行负责。

key 最长 191 个字符，使用大小写敏感的点分路径；每个路径段必须以字母开头，仅允许字母、数字和下划线，因此允许 `camelCase`；禁止首尾点、连续点和空路径段。路径段不得使用 `__proto__`、`prototype` 或 `constructor`，后端和前端都应使用同一套规则校验。例如：

```text
menu.dashboard.title
menu.system.user.title
profile.form.avatarPlaceholder
business.orderStatus.pending
```

`message_key` 列使用 `utf8mb4_bin` 保证数据库唯一约束、路径查询和 key 模糊查询与代码层一样大小写敏感。

全部分类共享同一个 Message Key 命名空间；相同 Key 始终定位并更新同一条消息，其他 Key 不得与其形成叶子和路径前缀冲突。例如，`dashboard` 和 `dashboard.title` 不能同时存在，因为 bundle 中的 `dashboard` 不能同时是字符串和 JSON 对象。即使冲突 Key 分属不同分类或语言，也必须拒绝。`save` 必须在事务内全局检查目标 Key 的所有祖先路径和后代路径，并通过数据库锁串行化命名空间校验与写入；发现冲突时整次写入失败，不在 bundle 组装时临时选择覆盖方。

## 4. 代码层固定语言

后端和前端分别在代码中固定支持的语言，不查询数据库。

后端可以使用枚举：

```java
public enum SupportedLocale {
    ZH_CN("zh-CN"),
    EN_US("en-US");

    private final String code;

    SupportedLocale(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
```

前端继续维护与当前 `SupportedLanguagesType` 对应的常量。增加语言需要代码发布，但可以保证语言集合可控。前端固定使用 `en-US` 作为 `fallbackLocale`，不随用户当前语言变化。

`sys_i18n.locale` 只能取这组代码层常量；写入时校验，查询时按常量顺序输出，不在数据库中新增语言定义。

## 5. Spring Boot 后端静态国际化

### 5.1 推荐格式：`.properties`

静态文案按所属 Maven 模块分开维护，公共模块不承载 RBAC 或系统业务文案：

```text
gnilc-common-core: i18n/common/messages.properties
                   i18n/common/messages_zh_CN.properties
                   i18n/common/messages_en_US.properties

gnilc-auth-rbac:   i18n/rbac/messages.properties
                   i18n/rbac/messages_zh_CN.properties
                   i18n/rbac/messages_en_US.properties

novum-core:      i18n/system/messages.properties
                   i18n/system/messages_zh_CN.properties
                   i18n/system/messages_en_US.properties
```

配置：

```yaml
spring:
  messages:
    basename: i18n/common/messages,i18n/rbac/messages,i18n/system/messages
    encoding: UTF-8
    fallback-to-system-locale: false
    use-code-as-default-message: false
```

请求语言由 Spring MVC 的 `AcceptHeaderLocaleResolver` 根据 `Accept-Language` 解析，默认语言配置为 `app.i18n.default-locale=en-US`；业务服务通过 `LocaleContextHolder` 读取当前请求语言。解析器的支持列表与代码层 `SupportedLocale` 常量一致，只接受 `zh-CN` 和 `en-US`；显式 `zh-CN` 请求返回中文，`Accept-Language` 缺失、格式错误、包含不支持的语言或仅是受支持语言的其他变体时回退到 `en-US`，不返回参数错误。该解析发生在 Bean Validation 之前，因此注解校验和 `I18nMessageService` 使用同一语言规则。

每个 basename 都提供不带语言后缀的默认 `messages.properties`；至少一个默认 bundle 必须存在，否则 Spring Boot 的 `MessageSource` 自动配置可能不会生效。应用组合层负责在 `spring.messages.basename` 中列出实际装配的模块语言包；`I18nMessageService` 仍放在 `gnilc-common-core`，只依赖统一 `MessageSource`。

后端静态 message key 使用模块归属前缀：`common.*`、`rbac.*`、`system.*`； `validation.*` 仅用于公共校验文案。不同 basename 不得定义同一 key，同一 basename 的默认、 `zh-CN` 和 `en-US` 文件必须包含相同 key 集合。实施时增加轻量构建校验，重复 key 或语言 key 集不一致时直接失败，不依赖 Spring 的 basename 顺序静默覆盖。

示例：

```properties
common.unexpected.error=An unexpected error occurred.
validation.argument.invalid=The request contains invalid fields.
system.auth.authentication.failed=Authentication failed.
rbac.menu.title.required=Menu title is required.
```

参数使用 Spring `MessageFormat` 位置参数：

```properties
system.admin.nickname.tooLong=Nickname must be at most {0} characters.
```

### 5.2 Spring 支持的格式和区别

Spring 国际化的格式由具体 `MessageSource` 实现决定，不是所有格式都由 Spring Boot 自动配置支持。

| 格式 | 支持方式 | 特点 | 建议 |
| --- | --- | --- | --- |
| `.properties` | `ResourceBundleMessageSource`、Boot 自动配置原生支持 | 简单、稳定、适合 classpath 资源、支持 locale 后缀 | 当前项目首选 |
| `.xml` | `ReloadableResourceBundleMessageSource` 可支持 | 可使用 XML 表达 properties，适合已有 XML 配置，但需要显式配置 MessageSource | 不建议新项目使用 |
| `ListResourceBundle` Java 类 | `ResourceBundleMessageSource` 支持 | 编译期类型安全，但每次改文案需要重新编译 | 不建议用于文案 |
| YAML/JSON | Spring `MessageSource` 不直接支持 | 需要自行加载、转换并实现 MessageSource 适配器 | 前端可用，后端不建议 |

因此后端采用 `.properties`，前端继续采用 JSON。不要为了统一文件格式而让后端额外实现 JSON MessageSource。

### 5.3 `I18nMessageService` 具体代码

建议新增到 `gnilc-common-core`，作为一个深模块的调用接口。业务代码只需要知道 key 和参数，不需要知道 Spring 的 locale 解析细节。

```java
package com.gnilc.common.i18n;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

/**
 * 读取当前请求语言对应的后端静态国际化文案。
 */
@Service
public class I18nMessageService {

    private static final Logger log = LoggerFactory.getLogger(I18nMessageService.class);
    private static final Locale FALLBACK_LOCALE = SupportedLocale.EN_US.toLocale();
    private final MessageSource messageSource;
    private final Locale defaultLocale;

    public I18nMessageService(
            MessageSource messageSource,
            @Value("${app.i18n.default-locale:en-US}") String defaultLocale) {
        this.messageSource = Objects.requireNonNull(messageSource, "messageSource");
        this.defaultLocale = toLocale(defaultLocale);
    }

    /**
     * 使用当前请求的 Locale 获取文案。
     */
    public String get(String code) {
        return get(code, LocaleContextHolder.getLocale(), new Object[0]);
    }

    /**
     * 使用当前请求的 Locale 和 MessageFormat 参数获取文案。
     */
    public String get(String code, Object... args) {
        return get(code, LocaleContextHolder.getLocale(), args);
    }

    /**
     * 显式指定 Locale 获取文案，便于导出、通知等非当前请求场景。
     */
    public String get(String code, Locale locale, Object... args) {
        String messageCode = requireCode(code);
        Object[] messageArgs = args == null ? new Object[0] : args;
        Locale targetLocale = normalize(locale);
        String message = messageSource.getMessage(
                messageCode,
                messageArgs,
                null,
                targetLocale
        );
        if (message == null) {
            log.warn("Missing i18n message: code={}, locale={}", messageCode, targetLocale);
            return messageCode;
        }
        return message;
    }

    /**
     * 找不到 key 时使用调用方提供的默认文案。
     */
    public String getOrDefault(
            String code,
            String defaultMessage,
            Object... args) {
        String messageCode = requireCode(code);
        Object[] messageArgs = args == null ? new Object[0] : args;
        return messageSource.getMessage(
                messageCode,
                messageArgs,
                defaultMessage,
                normalize(LocaleContextHolder.getLocale())
        );
    }

    private Locale normalize(Locale locale) {
        return SupportedLocale.normalize(locale, defaultLocale);
    }

    private static Locale toLocale(String languageTag) {
        return SupportedLocale.supports(languageTag)
                ? Locale.forLanguageTag(languageTag)
                : FALLBACK_LOCALE;
    }

    private static String requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Internationalization code must not be blank.");
        }
        return code;
    }
}
```

`get` 找不到 key 时不抛出 `NoSuchMessageException`，记录 warning 并返回原始 key；调用方需要可读默认文案时使用 `getOrDefault`。

调用方式：

```java
return R.error(
        ResponseCode.AUTHENTICATION_FAILED,
        i18nMessageService.get("system.auth.authentication.failed")
);
```

```java
Preconditions.checkArgument(
        StringUtils.isNotBlank(dto.getTitle()),
        i18nMessageService.get("rbac.menu.title.required")
);
```

注意：`I18nMessageService` 只返回字符串，不修改 `R` 和 `Preconditions` 的接口。

## 6. 后端校验和字段错误

使用 Bean Validation 的标准占位符：

```java
@NotBlank(message = "{system.admin.nickname.required}")
@Size(max = 255, message = "{system.admin.nickname.max}")
private String nickname;
```

`RestExceptionHandlingConfiguration` 继续作为统一异常捕获入口。除校验异常外，其他异常处理分支也只需通过 `I18nMessageService` 取得 `error/message` 文案；只有校验异常额外把字段错误列表放入现有 `R.data`，不新增 `R` 字段。

### 6.1 处理流程

以一个请求 DTO 为例：

```java
public record ProfileDto(
        @NotBlank(message = "{system.admin.nickname.required}")
        String nickname
) {
}
```

这项调整解决两个问题：公共的 `R` 结构保持兼容，同时让前端可以根据字段名定位并显示具体错误；错误文案仍由后端统一按当前语言解析，不要求每个业务控制器重复拼装校验错误。

请求发送空的 `nickname` 后，Spring MVC 按下面的顺序处理：

1. Spring MVC 先把 `Accept-Language` 归一化为受支持的当前 `Locale`。
2. Bean Validation 发现 `nickname` 违反 `@NotBlank`，并通过 Spring `MessageSource` 将注解中的 key 解析成当前语言文本，例如“昵称不能为空”。
3. Spring 创建 `org.springframework.validation.FieldError`，其中包含字段名 `nickname`、校验码 `NotBlank` 和已经解析的最终消息。
4. `RestExceptionHandlingConfiguration` 捕获 `MethodArgumentNotValidException`。
5. 异常处理器遍历 `BindingResult.getFieldErrors()`，把每个 Spring `FieldError` 转成响应使用的 `FieldError`。
6. 异常处理器使用现有的 `R.error(code, error, data)`，把字段错误列表放入 `data`。
7. 前端先显示 `error`，再根据 `field` 把同一条错误设置到表单的 `nickname` 字段。

自定义的 `FieldError` 只是 `R.data` 中的数据类型，不是对 `R` 增加字段：

```java
@Data
@AllArgsConstructor
public class FieldError {
    private String field;
    private String code;
    private String message;
}
```

异常处理器的核心逻辑可以抽象成：

```java
List<FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
        .map(error -> new FieldError(
                error.getField(),
                error.getCode(),
                error.getDefaultMessage()))
        .toList();

String error = fieldErrors.stream()
        .map(FieldError::getMessage)
        .findFirst()
        .orElse(i18nMessageService.get("validation.argument.invalid"));

return R.error(ResponseCode.ARGUMENT_INVALID.getCode(), error, fieldErrors);
```

自定义 `FieldError` 与 Spring 的同名类型需要放在不同包中；异常处理器不要同时导入两个同名类型，Spring 类型可依靠 `getFieldErrors()` 的泛型推断或使用完整类名。

这样做的结果是：`R` 的 JSON 结构完全不变，但 `data` 在校验错误时具有明确的字段错误类型。

### 6.2 响应示例

```json
{
  "code": 10001,
  "data": [
    {
      "field": "nickname",
      "code": "NotBlank",
      "message": "昵称不能为空"
    }
  ],
  "error": "昵称不能为空",
  "message": "昵称不能为空"
}
```

JSON 字段名本身不国际化。业务响应中的状态、字典和菜单标题返回 code/key，由前端负责显示；只有后端错误和校验信息由 `MessageSource` 生成最终文字。

## 7. 全量国际化 JSON 接口

### 7.1 请求

```http
POST /sys/i18n-message/bundle/admin
```

路径参数 `category` 必填且必须属于后端常量维护的分类集合。该接口不传 locale，返回指定分类的所有支持语言；某个语言尚无记录时仍返回该语言对应的空对象。

### 7.2 返回格式

返回结构直接是标准国际化 JSON 对象：顶层 key 是语言代码，值是该语言的 JSON 对象。

```json
{
  "zh-CN": {
    "pageTitle": "Novum 管理端",
    "loginSubtitle": "请输入后台管理员账号信息",
    "dashboard": {
      "title": "首页"
    }
  },
  "en-US": {
    "pageTitle": "Novum Admin",
    "loginSubtitle": "Enter your administrator account details",
    "dashboard": {
      "title": "Dashboard"
    }
  }
}
```

数据库中的 key 直接作为返回 JSON 的 dot path：

```text
message_key  = dashboard.title
i18n_value = 首页
```

转换后为：

```json
{
  "dashboard": {
    "title": "首页"
  }
}
```

### 7.3 前端处理

前端按以下顺序处理：

1. 应用启动时先执行 `setupI18n(app)` 并加载本地静态 JSON，未登录页面不请求数据库 bundle。
2. 登录成功或恢复有效会话后，在生成动态路由和菜单之前请求一次 `/sys/i18n-message/bundle/admin`。
3. `setupI18n` 完成后使用 `setLocaleMessage()` 为每个支持语言替换重建后的完整消息树，更新会响应式地传递到已渲染组件。
4. 对每个 locale 按“数据库 bundle 在前、本地 JSON 在后”做深度合并，再写入 `vue-i18n`，不能直接用后加载的 bundle 覆盖本地值。
5. 确保所有支持语言都已加载，并将 `vue-i18n.fallbackLocale` 显式设为 `en-US`。
6. 动态菜单的 `title` 继续作为 key，通过 `$t()` 显示。

当前语言缺少某个 key 时显示 `en-US` 值；`en-US` 也缺失时才显示原始 key。

第一版不实现翻译配置的实时推送、轮询或 Redis 发布。前端在当前会话首次进入已认证区域时请求一次 bundle 并缓存在当前页面内存中；管理页面保存或删除成功后重新加载完整 bundle，使当前单页应用中的文案立即更新；其他浏览器页面在下次刷新或重新进入应用时获取新配置。如果 bundle 加载失败，不阻断登录、路由生成或页面访问；前端继续使用本地 JSON，数据库动态文案按回退语言或原始 key 显示。

这里的“全量”指目标分类在 `sys_i18n` 中登记的全部语言数据。前端仓库中的静态 JSON 仍然由前端本地加载，不要求后端复制一份；前端把本地语言包和该接口返回的数据库语言包合并。

本地语言包与数据库语言包出现同名 key 时，本地 JSON 固定优先。前端先合并数据库 bundle，再合并本地 JSON；数据库只能补充本地不存在的 key，不能覆盖随代码发布的页面、按钮等静态文案。前后端都不维护或校验静态 key 冲突，管理页面和 `I18nMessageInput` 也不阻止保存；与静态语言包同名而导致动态消息不生效的情况由人为命名约束规避。

当前默认菜单使用的 `page.dashboard.title` 和 `page.auth.profile` 已属于本地 JSON。为避免动态消息被静态消息覆盖，实施时保持 `az_menu.title` 字段不变，只将默认菜单的字段值迁移为：

```text
Dashboard -> menu.dashboard.title
Profile   -> menu.profile.title
```

同时在 `sys_i18n` 中为 `category = admin` 初始化这两个 key 的 `zh-CN` 和 `en-US` 记录。`page.*` 继续由前端本地 JSON 管理，`menu.*` 用于数据库可配置菜单文案。

菜单表单与国际化组件各自调用菜单接口和 `/sys/i18n-message/save`，不建立跨模块事务，也不新增组合接口。允许翻译暂时未被菜单引用，也允许菜单暂时只有 key 而无翻译；两个保存操作的失败独立提示，不相互回滚。

## 8. 国际化消息管理接口

`/bundle/{category}` 通过路径参数指定运行时语言包分类；管理接口把分类作为消息属性处理，而不是消息身份。

动态消息代码统一使用 `I18nMessage` 命名，不以单独的 `I18n` 代替消息资源：后端入口为 `I18nMessageController`，数据库业务服务为 `DynamicI18nMessageService`，传输对象使用 `I18nMessage*Dto` / `I18nMessage*Vo`；前端使用 `I18nMessageApi`、`getI18nMessage*`、`saveI18nMessage` 和 `removeI18nMessage`。消息 key 的代码和接口字段统一命名为 `messageKey`，数据库列命名为 `message_key`；`setupI18n`、`SupportedLocale` 和 `sys_i18n` 继续表示国际化机制、语言与既定存储结构。

```http
POST /sys/i18n-message/bundle/{category}
POST /sys/i18n-message/categories
POST /sys/i18n-message/page
POST /sys/i18n-message/values/{messageKey}
POST /sys/i18n-message/create
POST /sys/i18n-message/save
POST /sys/i18n-message/remove/{messageKey}
```

接口命名沿用当前 RBAC 模块的动词路径风格，例如 `/tree`、`/page`、`/save`、`/remove`，不强制采用 REST 资源风格。

`POST /sys/i18n-message/categories` 返回后端常量维护的分类值列表 `['default', 'admin']`。新增消息时分类必填，前端默认选择列表第一项；列表为空时保持空值并禁止提交。

`POST /sys/i18n-message/values/{messageKey}` 按全局 Message Key 查询分类和各语言值。`messageKey` 作为路径参数传递并由前端进行 URL 编码，不发送查询请求体或分类。

响应示例：

```json
{
  "category": "admin",
  "messageKey": "menu.dashboard.title",
  "values": [
    { "locale": "zh-CN", "value": "首页" },
    { "locale": "en-US", "value": "Dashboard" }
  ]
}
```

查询的 `messageKey` 不存在时，接口仍返回成功，但现有 `R.data` 为 `null`；标准的前端 `load` 因而返回 `null` 并保留当前草稿。查询结果为 `null` 或 `I18nMessage.values = []` 都表示没有可回填的语言值，组件保留现有输入；只有返回至少一种语言时才先清空全部语言输入，再回填查询结果。

`POST /sys/i18n-message/create` 只创建尚不存在的 Message Key；目标 Key 已存在时返回参数错误，不修改已有分类或语言值。国际化消息管理 Drawer 的新增操作使用该接口，避免过期列表或重复操作覆盖已有消息。

`POST /sys/i18n-message/save` 保持原有行为，统一处理新增、修改语言值和移动分类。Message Key 是不可变更的全局消息身份，管理页面编辑时只读；需要更换 Key 时先删除再新增。保存相同 Key 表示更新同一条消息，提交不同分类时在同一事务内更新该 Key 的全部语言行；另一 Key 与目标 Key 的祖先/后代路径冲突必须全局拒绝，检查与写入属于同一事务。

`save.values` 采用局部保存语义：只覆盖请求中提交的语言，未提交语言保持不变。同一请求内每个 locale 最多出现一次，且必须属于代码层支持的语言；重复 locale 或不支持的 locale 使整个请求参数校验失败，不使用数组先后顺序决定覆盖值。该校验在所有数据库操作之前完成，失败时不写入任何记录。每个非空 `value` 最长 4,000 个字符；数据库仍使用 `text`，由前端提供即时提示、后端执行最终长度校验。超过该上限的长内容不属于 UI 国际化消息，应由独立内容模块管理。提交语言的 `value` 去除首尾空白后为空时，表示删除该语言记录，不保存空字符串；未出现在 `values` 中的语言仍保持不变。`en-US` 必须在每次保存中提交非空值，作为所有消息的兜底语言；其他语言允许为空。当前语言和 `en-US` 都缺失时，前端显示原始 key。

所有查询接口返回 `values` 时，均按代码层支持语言的固定顺序排列。批量保存使用事务：

```json
{
  "category": "admin",
  "messageKey": "menu.dashboard.title",
  "values": [
    { "locale": "en-US", "value": "Dashboard" },
    { "locale": "zh-CN", "value": "首页" }
  ]
}
```

`POST /sys/i18n-message/remove/{messageKey}` 只接受路径参数 `messageKey`，不发送请求体或分类；物理删除该 Key 的全部语言记录，不支持指定语言删除。单语言删除统一通过 `POST /sys/i18n-message/save` 提交空白值完成。该接口使用幂等语义：目标 Key 原本不存在时仍返回成功，不因重复点击、请求重试或列表数据过期返回额外错误。

管理列表 `POST /sys/i18n-message/page` 支持以下查询参数：

按 RBAC 模块的 `POST /page` 约定，这些参数放在分页请求体中；它们仍是列表查询条件，不设计为 URL 资源路径。

```text
key       key 模糊查询
value     翻译值模糊查询
category  分类查询
locale    语言查询
```

`page` 未传 `category` 时查询全部分类；传入时按分类筛选。列表按全局 `messageKey` 分组分页，避免同一个 Key 的不同语言被拆到不同页。

`locale` 和 `value` 只用于筛选哪些 key 分组入选，不裁剪返回数据。后端先完成 key 分组筛选和分页，再一次性查询当前页全部 key 的所有语言，不逐项调用 `/values`。响应使用现有 `PageResult`，`list` 中每项按 key 分组：

```json
{
  "currentPage": 1,
  "pageSize": 10,
  "totalCount": 1,
  "totalPage": 1,
  "list": [
    {
      "category": "admin",
      "messageKey": "menu.dashboard.title",
      "values": [
        { "locale": "zh-CN", "value": "首页" },
        { "locale": "en-US", "value": "Dashboard" }
      ]
    }
  ]
}
```

分页项、单条查询和保存结果统一包含 `category`、`messageKey`、`values`。并发编辑采用最后一次保存生效，不新增版本号或乐观锁；每次保存仍在一个事务中统一维护分类和语言值。

## 9. 前端国际化消息输入模块

模块建议命名为 `I18nMessageInput`，目录为：

```text
packages/effects/common-ui/src/components/i18n-message-input
```

该模块作为标准表单控件使用，不直接依赖具体接口。默认 `v-model` 绑定业务表保存的 `messageKey`，使用方只需要注入加载和保存函数：

```vue
<I18nMessageInput
  v-model="form.title"
  :load="loadMenuTitle"
  :save="saveMenuTitle"
/>
```

### 9.1 数据类型

```ts
interface I18nMessage {
  messageKey: string;
  values: I18nMessageValue[];
}

interface I18nMessageValue {
  locale: string;
  value: string;
}
```

```ts
type I18nMessageLoader = (messageKey: string) => Promise<I18nMessage | null>;

type I18nMessageSaver = (message: I18nMessage) => Promise<I18nMessage>;
```

组件只保留一个外部数据入口 `v-model`，其值始终是 `messageKey`。Popover 外、平时在业务表单中显示的触发输入框只读，依次使用当前界面语言文本、`en-US` 文本、Message Key；它仍然属于组件本身。组件内部独立维护 key、各语言值和加载状态，编辑草稿不会提前写入父表单。

每次打开浮层时，如果 `v-model` 非空，组件都调用一次 `load(v-model)` 获取最新数据；`v-model` 为空时直接创建空白草稿，不调用接口。`load` 返回 `null` 或返回 `I18nMessage` 且 `values: []` 时保留组件当前已提交值；返回至少一种语言时清空语言编辑区并回填结果；抛出异常时显示加载失败和重试操作。加载期间禁用编辑和保存。

### 9.2 编辑和保存

浮层中的 Key 始终可编辑，包括修改菜单时。打开时 `v-model` 非空会自动查询一次；打开时为空不查询。打开后修改 Key 不会自动查询，并保留已填写的语言内容；必须点击 Key 右侧搜索按钮并查询成功后才能保存。查询返回 `null` 或空 `values` 时保留现有输入，返回至少一种语言时先清空全部语言输入再回填结果，并把右侧按钮切换为重置；重置只清空语言输入，保留 Key 和本次成功查询状态。空查询结果不进入重置状态。查询失败时禁止保存。组件忽略 Key 改动、关闭或后续查询之前发出的过期异步响应，并在查询或保存期间禁用重复操作。

点击保存时，组件调用 `save({ messageKey, values })`。保存成功后才使用返回结果更新 `v-model` 和外部显示内容并关闭浮层；保存失败时保持浮层和草稿不变。父表单因此只会看到已经保存成功的 key。

点击取消或浮层外部时：

- 草稿没有变化：直接关闭。
- 草稿已经变化：提示“未保存，确认取消吗？”，确认取消后关闭，选择继续编辑则保持浮层。

### 9.3 模块参数

```text
v-model          业务表保存的 messageKey
load             按 v-model 加载最新语言值
save             保存当前内部草稿
locales          可编辑语言；编辑区固定将 en-US 排在第一项
disabled         禁用整个控件
placeholder      外部输入框占位文案
rows             每个语言输入框的初始行数，默认 2
size             内外输入控件的统一尺寸，支持 small、default、large，默认 default
```

不再提供 `v-model:message-key`、`presetKey`、`data`、`onChange`、`position`、`width`、`height`、`texts`、`open()` 和 `close()`。浮层统一从表单控件下方展开，宽度和移动端最大尺寸由组件内部处理。组件文案使用公共前端语言包，不要求每个调用方重复传入。

组件已注册到 `apps/admin` 的 Vben Form 适配器，可像 `Input`、`TimePicker` 一样在 Schema 中使用：

```ts
async function loadMenuTitle(messageKey: string) {
  return getI18nMessageValues(messageKey);
}

async function saveMenuTitle(message: I18nMessage) {
  return saveI18nMessage({ category: 'admin', ...message });
}

{
  component: 'I18nMessageInput',
  componentProps: {
    load: loadMenuTitle,
    save: saveMenuTitle,
  },
  fieldName: 'title',
  label: '菜单标题',
}
```

公共组件不新增 `category` Prop，也不内置 `admin`。菜单页面的保存适配器负责追加 `category: 'admin'`；如果查询到的 Key 原属 `default`，从菜单组件保存时会把整条消息移动到 `admin`。

### 9.4 首个实际使用场景

本轮在 `apps/admin` 提供国际化消息管理页面 `/system/i18n-message`。该页面直接使用分页 Grid 和表单 Drawer，不内嵌 `I18nMessageInput`；该组件用于菜单标题等需要绑定 Message Key 的业务表单。

页面提供按 Key、翻译值、分类和语言查询的分组分页列表，并通过新增、编辑、删除操作完整调用：

```text
POST /sys/i18n-message/page
POST /sys/i18n-message/categories
POST /sys/i18n-message/values/{messageKey}
POST /sys/i18n-message/create
POST /sys/i18n-message/save
POST /sys/i18n-message/remove/{messageKey}
```

新增 Drawer 必须选择分类，默认使用 `/categories` 返回的第一项 `default`；返回空列表时保持空并禁止新增，提交时调用 `/create`。编辑 Drawer 调用 `/save`，分类可修改，Message Key 只读；修改 Key 必须删除后重新新增。`en-US` 必填且排在第一项，其他语言可以为空；清空非英文语言表示删除该语言值。整条消息删除另行确认。

保存消息时，只要修改前或修改后的分类是 `admin`，就调用 `reloadDynamicMessages()`；删除 `admin` 消息时也调用，删除 `default` 消息时不调用。重载固定请求 `/bundle/admin`，按“数据库 bundle 在前、本地静态 JSON 在后”重建每个支持语言的完整消息对象，再通过 `setLocaleMessage()` 替换运行时消息，最后刷新管理列表。操作只涉及 `default` 时只刷新管理列表，也不通知其他浏览器。

`/save` 是 `save` 回调的保存成败边界。`/save` 失败时回调抛出异常并保留组件草稿；`/save` 成功后，即使后续 `/bundle` 重载失败，回调仍返回保存结果并关闭组件，同时提示“保存成功，但当前页面语言包刷新失败”。bundle 重载属于落库后的本地同步，不参与数据库事务。

同时初始化两级后台动态菜单、菜单标题翻译和接口权限数据：

```text
System（CATALOG）
└── I18nMessage（MENU）
```

`System` 目录使用路径 `/system`、组件 `BasicLayout` 和标题 key `menu.system.title`；`I18nMessage` 子页面使用路径 `/system/i18n-message`、组件 `/system/i18n-message/index` 和标题 key `menu.i18nMessage.title`。两个标题 key 都在 `sys_i18n` 中初始化 `category = admin` 的中英文值。

权限按使用目的分开：

- `/sys/i18n-message/bundle/{category}` 为非公开接口，授予内置 `admin` 基线角色，供所有已登录管理端加载动态语言包。
- `/sys/i18n-message/categories`、`/sys/i18n-message/page`、`/sys/i18n-message/values/{messageKey}`、`/sys/i18n-message/create`、`/sys/i18n-message/save` 和 `/sys/i18n-message/remove/{messageKey}` 均为非公开接口，只授予新增的 `i18n:manager` 角色。
- `System` 目录和 `/system/i18n-message` 子菜单都绑定 `i18n:manager` 角色，满足菜单层级闭包；后续系统管理页面可以复用该目录。
- `i18n:manager` 是 `built_in = 1` 的内置角色，角色代码和角色本身不可修改、删除；用户、菜单和权限关系仍按现有 RBAC 机制维护。
- 初始化脚本恢复该角色及其默认菜单、权限关系，并只为默认 `admin` 账号恢复初始绑定；其他后台管理员不会自动获得国际化配置维护权限。

## 10. 实施顺序

1. 固定后端分类值和前后端支持的语言常量。
2. 删除国际化客户端字段、常量和请求头，将运行时接口改为 `/bundle/{category}`。
3. 增加 Spring `MessageSource` properties 和 `I18nMessageService`。
4. 将需要国际化的后端硬编码文案逐步改为 `i18nMessageService.get(...)`。
5. 使用 Bean Validation 的 `{message.key}` 方式改造字段校验文案。
6. 创建 `sys_i18n` 单表和 MyBatis-Plus 访问模块。
7. 实现全局 Message Key、可变分类、按 Key 查询、统一保存、删除、分类列表和按分类加载的全量 bundle 接口。
8. 保持 `az_menu.title` 字段不变，将默认菜单的静态 `page.*` key 迁移到数据库 `menu.*` key，并初始化中英文翻译。
9. 前端加载 `admin` 分类的全量 bundle 并合并到现有 `vue-i18n` 结构；不增加静态 Key 保存校验。
10. 实现表单控件 `I18nMessageInput`，使用标准 `v-model` 绑定 key，并在内部完成加载、草稿、取消确认和保存。
11. 新增 `/system/i18n-message` 国际化消息管理页面，使用分页 Grid 和直接编辑 Drawer；在菜单表单中接入 `I18nMessageInput`，并初始化对应菜单、翻译、内置 `i18n:manager` 角色和权限数据。

按深模块原则，外部调用方只需要学习后端静态文案的 `I18nMessageService`、动态国际化消息管理接口和 `I18nMessageInput` 的少量接口；locale 解析、JSON 组装、分类过滤、保存事务和前端语言包合并都集中在实现内部。
