# T7 前端依赖修复与拆包 - 构建报告

日期：2026-11-10 ｜ 范围：`health-web/`

## 升级前后对比

| 项目 | 升级前 | 升级后 |
|------|--------|--------|
| npm audit（生产依赖） | 3 个漏洞（2 high + 1 moderate） | **0** |
| npm audit（全量含 dev） | 8 个漏洞（7 high + 1 moderate，修复中曾达 14） | **0** |
| vite | 5.x（esbuild 内核） | 8.3.7（rolldown 内核） |
| vitest | 1.6 | 5.1.0 |
| vue | ^3.4.0 | 3.5.45（修复 @vue/server-renderer XSS） |
| echarts | ^5.5.0 | 6.1.0（修复 DoS） |
| 构建 | - | 通过，1.8s |
| 前端测试 | - | 8/8 通过 |

## 关键修复

1. **高危/严重清零**：vue/@vue/server-renderer（XSS，GHSA-g2v6-rqmx-r4w6）、vite/tinypool/vitest（RCE，GHSA-5gmw-xhrv-c9v3 等）全部升级到安全版本。
2. **中危清零**：echarts DoS 升 6.1.0；glob（GHSA-3g3m-2fjr-6pw4）、minimatch（GHSA-g3hv-75wg-m249）为传递依赖，通过 package.json `overrides` 强制 `glob@^14.0.4` + `minimatch@^11.0.5`，editorconfig/js-beautify 间接标记随之解除。
3. **lockfile 重新生成**：package-lock.json 与 package.json 一致，`npm audit` 0 漏洞。

## vite 8（rolldown）迁移

- `manualChunks` 对象形式已移除 → 改为函数形式（echarts+zrender / element-plus / vue-vendor 三组预分包，路由级 code-split 原有保留）。
- `defineConfig` 改从 `vitest/config` 导入（test 字段生效）。
- `esbuild.drop` → `oxc.drop`（console/debugger 生产剥离）。
- `__dirname` → `import.meta.dirname`（configLoader native 兼容）。

## 构建产物（分包生效）

| chunk | 大小 | gzip |
|-------|------|------|
| index（业务入口） | 77.2 kB | 28.1 kB |
| vue-vendor | 30.7 kB | 12.0 kB |
| echarts（懒加载页） | 1116.6 kB | 370.4 kB |
| element-plus | 1141.9 kB | 358.5 kB |
| 各视图（30+ 路由级 chunk） | 5.8-11.5 kB | 2.3-5.2 kB |

> echarts/element-plus 均为独立缓存 chunk，路由按需加载；>500kB 提示为大库固有体积，非回归。

## 验证记录

- `npm run build` ✅ 1.8s 零错误零警告（除大库体积提示）
- `npm test`（vitest 5.1）✅ 2 文件 8 用例全过
- `npm audit` ✅ 0 vulnerabilities（生产 + dev 全量）
