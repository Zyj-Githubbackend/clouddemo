# 文档中心

项目文档已按主题拆分到 `docs/` 下，根目录只保留总入口 [`README.md`](../README.md)。

当前仓库的推荐运行与部署方式已经对齐为：

- 本机 Nginx 开发模式
- Docker A/B 双栈模式（`compose.shared.yml` + `compose.stack.yml` + `compose.edge.yml`）
- 运行期日志统一落到仓库根目录 `log/`

## 总览

- [项目交付摘要](overview/PROJECT_SUMMARY.md)
- [目录结构](overview/DIRECTORY_STRUCTURE.md)
- [技术说明](overview/TECHNOLOGY_STACK.md)
- [架构说明](architecture/ARCHITECTURE.md)

## 上手

- [快速开始](setup/QUICKSTART.md)
- [验收清单](setup/CHECKLIST.md)

## 部署

- [部署说明](deploy/DEPLOY.md)

## 可观测

- [可观测栈说明](observability/OBSERVABILITY.md)

## 测试

- [API 测试](testing/API_TEST.md)

## MCP

- [MCP 连接说明](mcp/MCP_CONNECTION.md)

## 前端

- [前端工程说明](../frontend2/README.md)
- [前端页面清单](ui-page-inventory.md)
- [Stitch 页面规格](stitch-page-spec.md)
