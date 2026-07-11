import torch

from app.inference.batch_inference_engine import BatchInferenceEngine


class FakeModel:
    def encode_text(self, tokens):
        return tokens.float()

    def encode_image(self, images):
        return images.float().view(images.shape[0], -1)[:, :2]


class FakeRuntime:
    model = FakeModel()
    device = "cpu"


def test_infer_text_batch_returns_cpu_embeddings():
    engine = BatchInferenceEngine(FakeRuntime())

    result = engine.infer_text_batch(torch.tensor([[3.0, 4.0], [5.0, 12.0]]))

    assert result.batch_size == 2
    assert result.vector_dim == 2
    assert torch.allclose(result.embeddings.norm(dim=-1), torch.ones(2))
