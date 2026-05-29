import time
from pathlib import Path
from dataclasses import dataclass

import torch
import open_clip


@dataclass
class OpenClipRuntime:
    model: torch.nn.Module
    preprocess: object
    tokenizer: object
    device: str
    model_name: str
    checkpoint_path: Path
    vector_dim: int
    load_cost_ms: int


class OpenClipModelLoader:
    def __init__(
        self,
        model_name: str,
        checkpoint_path: Path,
        tokenizer_dir: Path,
        device: str,
        vector_dim: int,
    ):
        self.model_name = model_name
        self.checkpoint_path = Path(checkpoint_path)
        self.tokenizer_dir = Path(tokenizer_dir)
        self.device = device
        self.vector_dim = vector_dim

    def check_model_file(self) -> None:
        print("检查权重文件：")
        if not self.checkpoint_path.exists():
            raise FileNotFoundError(f"模型文件不存在: {self.checkpoint_path}")

        if self.checkpoint_path.suffix != ".safetensors":
            raise ValueError(f"当前要求加载 safetensors 文件，但实际文件是: {self.checkpoint_path}")
        

    def check_tokenizer_files(self) -> None:
        """
        这里主要做本地 tokenizer 文件存在性检查。
        open_clip.get_tokenizer(MODEL_NAME) 会创建与 openCLIP 模型匹配的 tokenizer。
        """
        expected_files = [
            "tokenizer.json",
            "tokenizer_config.json",
            "vocab.json",
            "merges.txt",
            "special_tokens_map.json",
        ]

        exists_files = []
        missing_files = []

        for file_name in expected_files:
            file_path = self.tokenizer_dir / file_name
            if file_path.exists():
                exists_files.append(file_name)
            else:
                missing_files.append(file_name)

        print("检查 Tokenizer 文件：")
        # print(f"  已存在: {exists_files}")
        print(f"  未找到: {missing_files}")

        if not exists_files:
            print("  警告：未发现本地 tokenizer json/vocab 文件，但 open_clip 仍可通过 MODEL_NAME 创建 tokenizer。")

    def load(self) -> OpenClipRuntime:
        self.check_model_file()
        self.check_tokenizer_files()

        print("加载模型中: ")
        start = time.perf_counter()

        model, _, preprocess = open_clip.create_model_and_transforms(
            model_name=self.model_name,
            pretrained=str(self.checkpoint_path),
        )

        tokenizer = open_clip.get_tokenizer(self.model_name)

        model.to(self.device)
        model.eval()

        load_cost_ms = int((time.perf_counter() - start) * 1000)
        print(f"模型加载完成! 耗时 {load_cost_ms} ms")

        
        return OpenClipRuntime(
            model=model,
            preprocess=preprocess,
            tokenizer=tokenizer,
            device=self.device,
            model_name=self.model_name,
            checkpoint_path=self.checkpoint_path,
            vector_dim=self.vector_dim,
            load_cost_ms=load_cost_ms,
        )
        
        
