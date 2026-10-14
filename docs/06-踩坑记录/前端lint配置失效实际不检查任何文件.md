# 前端 lint 配置失效，实际不检查任何文件

记录 `npm run lint` 看似可用、其实什么都没检查的原因，以及临时怎么对改动文件做静态检查。

- 日期：2026-10-14
- 涉及：前端

## 现象

- `cat2bug-platform-ui` 下 `npm run lint` 报配置错误：`vue/max-attributes-per-line` 的选项 `allowFirstLine` 不被接受。
- 即使去掉该选项，`eslint --ext .js,.vue src` 也不报任何问题——所有 `.js` / `.vue` 文件都被忽略了。

## 根因

- `.eslintrc.js` 沿用 Vue 2 时代的规则写法，`eslint-plugin-vue` 升到 9.x 后 `max-attributes-per-line` 的 `multiline.allowFirstLine` 已删除；另有 `vue/name-property-casing` 规则在 9.x 中已不存在。
- `.eslintignore` 里有 `*.js`、`*.vue` 两行（注释写的是「忽略当前目录下」，但 eslint 的忽略规则不带 `/` 时匹配任意层级），等于整个 `src` 都被忽略。

## 解决办法

临时对改动文件做检查（不修改仓库配置）：

1. 复制一份 `.eslintrc.js` 到临时文件，删掉 `allowFirstLine` 和 `vue/name-property-casing`。
2. 运行 `npx eslint --no-eslintrc -c <临时配置> --no-ignore <改动的文件>`，分别对改动前后的文件跑一次，只看新增的错误。
3. 用完删除临时配置。

## 以后怎么避免

- 不要把 `npm run lint` 通过当成代码已检查；在修好配置前，评审时按上面的办法对改动文件单独跑。
- 待确认：修复 `.eslintrc.js` 与 `.eslintignore`（`*.js` / `*.vue` 改为 `/*.js` / `/*.vue`）后，存量文件会出现大量风格错误，需要单独一次变更统一处理。
