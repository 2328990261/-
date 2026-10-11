# EPUB 分卷插画存储方案：插画表 + volume_uuid + 落盘目录

后台「书籍上架」原为两步（先建主卷、再传分卷），且 EPUB 解析只保留纯文本、插图被丢弃；
现将两步合并为一步，并为 EPUB 内插图建立「落盘 + 入库 + 阅读页原位呈现」的完整链路。

## Status
accepted

## 决策

- **一步式上架**：新增 `POST /api/admin/publishBook`，主卷字段与 EPUB 文件一次提交，
  后端先建主卷、再逐卷解析入库，一次返回主卷 ID 与各分卷结果，避免「主卷建好但分卷失败」的中间态。
- **分卷业务键**：`novel_book_volume` 增加 `volume_uuid`（唯一索引）。内部外键仍用自增
  `id`（FK 关联安全、标准做法）；`volume_uuid` 作为公开引用与插画目录名，URL 中的数字 ID 保持不变
  （阅读历史存的是数字 `chapter_id`）。
- **插画表**：新建 `novel_book_illustration`，`volume_id` 外键关联分卷自增主键
  （`ON DELETE CASCADE`），`(volume_id, sort_order)` 唯一约束保证正文顺序，存插图 URL 与原始文件名。
- **落盘布局**：插图写入 `uploads/epub-images/{主卷ID}/{volume_uuid}/`，文件名展平为
  `{四位序号}_{原文件名}`（防跨章节同名覆盖，阅读顺序即序号）；静态映射 `/epub-images/**` 提供访问。
- **正文定位**：解析 XHTML 时把 `<img>` 替换为 `[插图n]` 标记（n = sort_order），
  `content` 仍是纯文本；`/novel/chapter/{id}` 返回分卷时附带 `illustrations` 数组，阅读页按标记渲染 `<img>`。
- **删除清理**：删除分卷时，插画表行由外键级联删除，磁盘目录由后端同步删除，避免孤儿文件。
- **容错**：单张插图提取/落盘失败则跳过并记日志，不拖垮整卷；整卷解析失败才判定该卷上传失败。

## Consequences

- 阅读页 `BookReader` 需把 `[插图n]` 行渲染为图片，分页中图片按一整行计数。
- 后台暂不做插画管理 UI（后续主卷查看/编辑分卷内容时再扩展，呈现方式与阅读页一致）。
- 现有 `uploadVolumes`（给已有书籍补分卷）保留并复用同一套插图解析逻辑，`BookManageView` 无需改动。
