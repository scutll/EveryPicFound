from PIL import Image

from app.inference.preprocessors import preprocess_image_sync


def test_preprocess_image_sync_converts_to_rgb(tmp_path):
    image_path = tmp_path / "sample.png"
    Image.new("RGBA", (16, 16), color=(255, 0, 0, 128)).save(image_path)

    def fake_preprocess(image):
        assert image.mode == "RGB"
        return "tensor"

    assert preprocess_image_sync(image_path.read_bytes(), fake_preprocess) == "tensor"
