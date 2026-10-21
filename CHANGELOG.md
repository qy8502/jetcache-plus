# Changelog

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [1.0.1] - 2026-10-21

### 新增

- `@MultiCached` / `@Cached` 的 `$$each$$` 支持在元素后继续访问属性或拼接表达式，可用于对象集合、联合主键和多参数 key，例如：
  - 对象集合：`#schools[$$each$$].id`
  - 联合主键：`#classes[$$each$$].teacherId+':'+#classes[$$each$$].lessonId`
  - 多参数 key：`#year+':'+#schools[$$each$$].id`
- 示例项目新增对象集合查询、按对象集合批量失效、联合主键和多参数 key 的接口与 HTTP 示例。
- README 新增“对象集合、联合主键与多参数 key”章节，补充 `condition` / `postCondition` 和 `Map` 返回值的说明。

### 修复

- 修复 multi key 场景下 `condition`、`postCondition` 或 `value` 为非逐项表达式时，`MultiCacheMap` 类型强转导致的 `ClassCastException`。
- 修复 `$$each$$` 参数名使用 `contains` 匹配的问题，改为精确 `equals`，避免参数名互为子串时匹配到错误参数。
- 修复批量读取时 `cache.getAll` 返回 `null` 导致的 `NullPointerException`；空 key 集合不再调用批量读取。
- 兼容集合中的 `null` 元素：表达式可求值时保留原有的 null key 行为，仅在属性访问等无法处理 null 的情况下跳过该元素，避免整批失败。

### 变更

- Maven Central 发布迁移至 Central Publisher Portal（`com.gradleup.nmcp`），替代已停服的 OSSRH。
- 返回类型为 `Map` 时改用 `LinkedHashMap`，结果 key 为请求集合元素，数量和顺序与请求集合一致，缺失值仍以 `null` 保留。
- JetCache 升级至 2.7.9。

### 兼容性说明

- 仅使用注解的现有代码无需修改，注解语义、`$$each$$` 语法及 `condition` / `postCondition` 行为保持兼容。
- 直接调用内部 `MultiCacheMap#getValues`、`getConditions`、`setValues`、`setConditions`、`defaultValueMap` 的代码需要适配：相关签名由 `MultiCacheMap` 调整为 `Map`。
- Caffeine、Redis 等标准缓存实现仍不接受真正的 null key，该限制与旧版一致；拼接表达式中的 null 会由 SpEL 转换为字符串 `"null"`（如 `"2024:null"`），可以正常作为缓存 key。
- `Map` 返回值的内容不变，但迭代顺序和 JSON 字段顺序由原来的 `HashMap` 顺序改为请求集合顺序。

[1.0.1]: https://github.com/qy8502/jetcache-plus/compare/v1.0.0...v1.0.1
