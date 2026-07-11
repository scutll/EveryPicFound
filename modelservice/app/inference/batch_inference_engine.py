import time

import torch

from app.batching.models import BatchInferenceResult


class BatchInferenceEngine:
    """负责执行批量推理, 根据设计文档, 任务参数只涉及基本信息流比如tokens/image, 不带入EventLoop信息"""

    def __init__(self, runtime):
        """初始化批量推理引擎。

        Args:
            runtime: OpenCLIP 模型、preprocess、tokenizer 和设备信息。

        """

        self.runtime = runtime

    def infer_text_batch(self, tokens: torch.Tensor) -> BatchInferenceResult:
        """编码文本 token 批次，并返回归一化后的 CPU 向量。

        Args:
            tokens: 文本 token 批量 Tensor, 形状为 ``[B,L]``。

        Returns:
            BatchInferenceResult, 包含 ``[B,D]`` CPU embedding、批大小和耗时。
        """

        start = time.perf_counter()
        tokens = tokens.to(self.runtime.device)
        with torch.inference_mode():
            embeddings = self.runtime.model.encode_text(tokens)
            embeddings = embeddings / embeddings.norm(dim=-1, keepdim=True)
        self._sync_cuda()
        return self._result(embeddings, start)

    def infer_image_batch(self, images: torch.Tensor) -> BatchInferenceResult:
        """编码图片批次，并返回归一化后的 CPU 向量。

        Args:
            images: 图片批量 Tensor, 形状为 ``[B,C,H,W]``。

        Returns:
            BatchInferenceResult, 包含 ``[B,D]`` CPU embedding、批大小和耗时。
        """

        start = time.perf_counter()
        images = images.to(self.runtime.device)
        with torch.inference_mode():
            embeddings = self.runtime.model.encode_image(images)
            embeddings = embeddings / embeddings.norm(dim=-1, keepdim=True)
        self._sync_cuda()
        return self._result(embeddings, start)

    def _sync_cuda(self) -> None:
        """在 CUDA 设备上同步 GPU, 保证耗时统计覆盖真实推理时间。
        """

        if str(self.runtime.device).startswith("cuda"):
            torch.cuda.synchronize()

    @staticmethod
    def _result(embeddings: torch.Tensor, start: float) -> BatchInferenceResult:
        """把 GPU 输出整理成 CPU 上的批量推理结果。

        Args:
            embeddings: 模型输出 embedding Tensor。
            start: 推理开始时间，使用 ``time.perf_counter()`` 记录。

        Returns:
            BatchInferenceResult。
        """

        embeddings = embeddings.detach().cpu()
        return BatchInferenceResult(
            embeddings=embeddings,
            batch_size=embeddings.shape[0],
            vector_dim=embeddings.shape[1],
            inference_cost_ms=int((time.perf_counter() - start) * 1000),
        )
