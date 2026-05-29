from pathlib import Path
import torch

BASE_DIR = Path(__file__).resolve().parent

MODEL_NAME = "ViT-B-32"

MODEL_DIR = BASE_DIR / "models" / "openclip-vit-32B"

# 指定 safetensors 文件
MODEL_PATH = MODEL_DIR / "open_clip_model.safetensors"

# tokenizer 文件目录，用于检查
TOKENIZER_DIR = MODEL_DIR

DEVICE = "cuda" if torch.cuda.is_available() else "cpu"

VECTOR_DIM = 512

WARMUP_IMAGE_PATH = BASE_DIR / "warmup.png"
WARMUP_TEXT = "An anime-style illustration of a blonde girl sitting by the window in a cozy cafe. She rests her face on her hand and looks outside at the rainy or snowy night city. A cup of hot coffee is on the table, and the scene feels quiet, warm, and slightly lonely."
ENABLE_WARMUP = True

LOG_DIR = BASE_DIR / "logs"
LOG_FILE_PATH = LOG_DIR / "model-service.log"
