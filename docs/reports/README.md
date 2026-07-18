# 测试与实验报告

本目录保存压测、实验、故障演练和方案对比的可复现记录。

## 现有报告

- [ModelService 动态批处理压测对比报告（ModelService 端）](EveryPicFound_ModelService动态批处理压测对比报告_modelService端.md)
- [ModelService 动态批处理压测对比报告（全链路）](EveryPicFound_ModelService动态批处理压测对比报告_全链路测试.md)

## 收录规则

报告应明确记录测试日期、代码或配置版本、环境、数据集、执行命令、场景、原始指标、结论和限制。无法复现的主观感受不作为报告结论。

性能测试必须遵守“单脚本、单场景、单时间窗、单批次串行”原则。上一项测试完整结束并完成结果落盘后，才能开始下一项；受到其他测试污染的批次必须标记无效并重新执行。

报告只陈述实际验证结果。由结果产生的项目方案变化应更新 [`../project/`](../project/) 或 [`../modules/`](../modules/)；可复用的测试和调优方法应提炼到 [`../technologies/`](../technologies/)。
