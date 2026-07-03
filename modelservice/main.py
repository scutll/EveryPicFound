from contextlib import asynccontextmanager

from fastapi import FastAPI

from config import(
    DEVICE,
    MODEL_NAME,
    MODEL_PATH,
    TOKENIZER_DIR,
    VECTOR_DIM
)

from app.api import router
from app.model_loader import OpenClipModelLoader
from app.batching.runtime import ModelBatchingRuntime
from app.log_utils import get_model_service_logger


logger = get_model_service_logger()


# 生命周期函数，负责启动和关闭时候执行的逻辑，以yield作为分界线
@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("event=MODEL_SERVICE_STARTING")
    batching_runtime = None
    try:
        loader = OpenClipModelLoader(
            model_name=MODEL_NAME,
            checkpoint_path=MODEL_PATH,
            tokenizer_dir=TOKENIZER_DIR,
            device=DEVICE,
            vector_dim=VECTOR_DIM
        )

        runtime = loader.load()
        batching_runtime = ModelBatchingRuntime(runtime)
        await batching_runtime.start()
        # 全局内注册Service
        app.state.model_batching_runtime = batching_runtime
        app.state.vectorization_service = batching_runtime.service

        logger.info(
            "event=MODEL_SERVICE_STARTED "
            f"modelName={runtime.model_name} "
            f"checkpoint={runtime.checkpoint_path} "
            f"device={runtime.device} "
            f"vectorDim={runtime.vector_dim} "
            f"loadCostMs={runtime.load_cost_ms}"
        )
        await batching_runtime.warm_up()

        yield
    finally:
        logger.info("event=MODEL_SERVICE_STOPPING")
        if batching_runtime is not None:
            await batching_runtime.shutdown()
        app.state.model_batching_runtime = None
        app.state.vectorization_service = None
        logger.info("event=MODEL_SERVICE_STOPPED")
    

app = FastAPI(
    title="EveryPicFound Model Service",
    description="openCLIP image/text vectorization service",
    version="0.1.0",
    lifespan=lifespan
)

app.include_router(router)
