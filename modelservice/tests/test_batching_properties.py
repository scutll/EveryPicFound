from app.batching.properties import BatchingProperties


def test_batching_properties_defaults_are_positive():
    properties = BatchingProperties()

    assert properties.text.max_batch_size > 0
    assert properties.text.max_wait_ms > 0
    assert properties.image.max_batch_size > 0
    assert properties.image.preprocess_worker_count > 0
    assert properties.queue.text_pending_maxsize > 0
    assert properties.executors.gpu_max_workers == 1
