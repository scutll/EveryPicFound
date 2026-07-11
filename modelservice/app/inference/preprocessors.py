import io

from PIL import Image


def preprocess_image_sync(image_bytes: bytes, preprocess):
    """解码图片字节，并执行 OpenCLIP 的 CPU 图片 transform。

    Args:
        image_bytes: 图片二进制内容。
        preprocess: OpenCLIP 加载出的图片预处理函数。

    Returns:
        预处理后的单张图片 Tensor，形状通常为 ``[C,H,W]``。
    """

    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    return preprocess(image)
