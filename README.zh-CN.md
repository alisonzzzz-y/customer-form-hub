# Customer Forms Hub | 中文说明

> 一个使用 Java 和 Spring Boot 处理客户安全及合规问卷的应用。

面向客户的团队常常需要在很短时间内完成很长的问卷。相关信息可能分散在政策文档里，有些问题也需要专家确认。这个项目把这些工作放进同一个审核流程。

AI 帮助整理问题和寻找相关的内部资料。审核者可以接受答案、修改答案，或将问题交给 SME / AE。系统不会自行批准或发送 AI 生成的答案。

[英文 README](README.md) · [前端仓库](https://github.com/alisonzzzz-y/customer-form-hub-frontend)

<!--
截图应放在前端 README，因为前端仓库是项目的主要展示入口。
后端 README 只链接到前端 README，不重复放相同图片。
-->

## 应用可以做什么

```text
创建工单
  -> 上传 Excel 或 Word 问卷
  -> 读取问题，并按部门归类
  -> 找到相关且已批准的知识来源
  -> 接受、编辑，或交给 SME / AE 处理未解决问题
  -> 完成最终审核
  -> 导出回复
```

React 前端提供主要工作台、知识库页面、报告页，以及给 Manager 使用的 AI Performance 页面。

## 部署

在线演示由四个小型服务组成：

| 部分 | 服务 | 作用 |
|---|---|---|
| 前端 | Vercel | 托管 React 应用 |
| 后端 API | Render | 运行 Spring Boot API 和文档处理流程 |
| 数据库 | Railway MySQL | 保存工单、问题、知识条目和审核数据 |
| AI 服务 | OpenAI API | 对问题分类，并为知识搜索创建 embedding |

本地开发时，可以通过下方环境变量连接任意 MySQL 8 或更高版本的数据库。

[打开在线演示](https://customer-form-hub.vercel.app/)

## 我的贡献

后端由我独立完成，包括 API、文档处理、AI 集成、数据模型、审核流程、检索检查和自动化测试。

## AI 如何使用

### 整理上传的问题

应用使用 `gpt-4o-mini` 把上传的问题归到 InfoSec、Legal、HR、Finance、ESG 等部门。系统会先检查返回的数据结构，再写入工作流程。

### 找到有用的资料

知识库内容会使用 `text-embedding-3-small` 处理。审核者打开一个问题时，后端会在已批准的知识条目中搜索，并返回最接近的 3 个结果。每个结果都保留来源编号，因此审核者知道资料来自哪里。

底层实现使用 stored embeddings 和 cosine similarity。它是帮助内部搜索的功能，不是一个会自己写答案和批准答案的系统。

### 让人保留最终控制权

审核者可以直接接受建议、修改后批准、交给 SME，或者要求 AE 澄清。AI Performance 页面展示每个 AI 辅助问题最新的处理结果。

## 一个小型检索检查

仓库内有一组带版本的模拟测试数据，位于 `src/main/resources/ai-performance/retrieval-benchmark-v1.json`。它会检查预期的知识来源是否出现在前 1 个或前 3 个搜索结果中。

| 本地演示运行结果 | 数值 |
|---|---:|
| 测试案例 | 12 |
| 预期来源排在第 1 位 | 100% |
| 预期来源出现在前 3 位 | 100% |
| 失败或跳过案例 | 0 |

这是一组小型模拟检查，用于确认改动后搜索功能仍然正常。它不衡量真实线上回答质量，也不表示 AI 可以独立回答客户问题。

## 技术栈

| 模块 | 技术 |
|---|---|
| 后端 | Java 21、Spring Boot 3.5、Spring Web |
| 数据 | Spring Data JPA、Hibernate、MySQL |
| AI | OpenAI REST API、`gpt-4o-mini`、`text-embedding-3-small` |
| 文件 | Apache POI，用于 Excel 和 Word 文件 |
| 测试和检查 | JUnit 5、Mockito、H2、GitHub Actions |

## 本地运行

需要 Java 21、MySQL 8 或更高版本，以及 OpenAI Platform API key。

```sql
CREATE DATABASE formhub;
```

```bash
export OPENAI_API_KEY="your-key"
export DB_URL="jdbc:mysql://localhost:3306/formhub"
export DB_USERNAME="root"
export DB_PASSWORD="your-password"
./mvnw spring-boot:run
```

默认 API 地址是 `http://localhost:8080/api`。

## 测试

```bash
./mvnw clean verify
```

测试覆盖检索评分、来源链接、审核结果、重新打开的问题、空状态、文件上传和兼容读取。同时也包含 MockMvc API 集成测试，验证工单状态更新、AI 建议升级给 AE、按部门分发 SME 请求，以及读取检索评估结果。这些接口测试使用独立的 H2 测试数据库，不会调用 OpenAI。GitHub Actions 会在推送到 `main` 或向 `main` 提交 PR 时运行 Maven 检查。

## 当前范围

空数据库首次启动时，应用会生成演示工单、知识条目、SME 请求和 8 条演示 AI 审核结果：5 条直接接受、2 条编辑后接受、1 条升级处理。这些数据只用于帮助浏览界面。

项目暂时没有完整的登录和权限系统、Flyway 数据库迁移、完整事件历史或已验证的邮件发送。这些是后续可以继续补上的内容，而不是目前已经完成的功能。
