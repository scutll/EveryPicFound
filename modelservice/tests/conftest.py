import sys
import types
import importlib.util
from pathlib import Path

import numpy as np


ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))


if importlib.util.find_spec("torch") is None:
    class Tensor:
        def __init__(self, data):
            self._array = np.array(data)

        @property
        def shape(self):
            return self._array.shape

        def float(self):
            return Tensor(self._array.astype(float))

        def to(self, device):
            return self

        def norm(self, dim=-1, keepdim=False):
            return Tensor(np.linalg.norm(self._array, axis=dim, keepdims=keepdim))

        def detach(self):
            return self

        def cpu(self):
            return self

        def tolist(self):
            return self._array.tolist()

        def view(self, *shape):
            return Tensor(self._array.reshape(shape))

        def __truediv__(self, other):
            other_array = other._array if isinstance(other, Tensor) else other
            return Tensor(self._array / other_array)

        def __getitem__(self, item):
            return Tensor(self._array[item])

    class InferenceMode:
        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, tb):
            return False

    torch_stub = types.ModuleType("torch")
    torch_stub.Tensor = Tensor
    torch_stub.long = "long"
    torch_stub.float = "float"
    torch_stub.cuda = types.SimpleNamespace(
        is_available=lambda: False,
        synchronize=lambda: None,
    )
    torch_stub.tensor = lambda data, dtype=None: Tensor(data)
    torch_stub.ones = lambda shape, dtype=None: Tensor(np.ones(shape))
    torch_stub.stack = lambda tensors: Tensor(np.stack([tensor._array for tensor in tensors]))
    torch_stub.allclose = lambda left, right: np.allclose(left._array, right._array)
    torch_stub.inference_mode = InferenceMode
    sys.modules["torch"] = torch_stub
