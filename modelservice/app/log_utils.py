import logging
from logging.handlers import RotatingFileHandler


from config import LOG_DIR, LOG_FILE_PATH

def get_model_service_logger() -> logging.Logger:
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    
    logger = logging.getLogger("model-service")
    
    if logger.handlers:
        return logger
    
    logger.setLevel(getattr(logging, "INFO", logging.INFO))
    logger.propagate = False
    
    formatter = logging.Formatter(
        fmt="%(asctime)s | %(levelname)s | %(name)s | %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )
    
    file_handler = RotatingFileHandler(
        filename=LOG_FILE_PATH,
        maxBytes=20 * 1024 * 1024,
        backupCount=5,
        encoding='utf-8'
    )
    file_handler.setFormatter(formatter)
    
    logger.addHandler(file_handler)
    
    return logger