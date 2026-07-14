import argparse
import json
import os
import socket
import subprocess
import sys
import threading
import time
from pathlib import Path
from urllib.request import urlopen

import uvicorn


REPO_ROOT = Path(__file__).resolve().parents[2]
MODELSERVICE_DIR = REPO_ROOT / "modelservice"
RESULTS_DIR = REPO_ROOT / "performance-test" / "results"


def wait_for_health(base_url: str, timeout_seconds: int) -> None:
    """等待进程内 Uvicorn server 可以接收 HTTP 请求。

    Args:
        base_url: ModelService 基础 URL。
        timeout_seconds: 最大等待秒数。

    Returns:
        None。

    Raises:
        RuntimeError: 超时仍未通过健康检查时抛出。
    """

    deadline = time.time() + timeout_seconds
    last_error = None
    while time.time() < deadline:
        try:
            with urlopen(f"{base_url}/health", timeout=10) as response:
                body = json.loads(response.read().decode("utf-8"))
                if response.status == 200 and body.get("success") is True:
                    return
        except Exception as exc:
            last_error = exc
        time.sleep(1)
    raise RuntimeError(f"modelservice did not become healthy: {last_error}")


def assert_port_available(host: str, port: int) -> None:
    """确认压测端口未被旧进程占用。

    Args:
        host: 需要绑定的主机地址。
        port: 需要绑定的端口。

    Returns:
        None。

    Raises:
        RuntimeError: 端口已经被占用时抛出。
    """

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.settimeout(1)
        if sock.connect_ex((host, port)) == 0:
            raise RuntimeError(f"port already in use: {host}:{port}")


def wait_for_server_started(server: uvicorn.Server, thread: threading.Thread, timeout_seconds: int) -> None:
    """确认本次启动的 Uvicorn server 已完成 startup。

    Args:
        server: 当前脚本创建的 Uvicorn server。
        thread: 执行 server.run 的线程。
        timeout_seconds: 最大等待秒数。

    Returns:
        None。

    Raises:
        RuntimeError: server 线程退出或超时未 started 时抛出。
    """

    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        if server.started:
            return
        if not thread.is_alive():
            raise RuntimeError("modelservice server thread exited before startup")
        time.sleep(1)
    raise RuntimeError("modelservice server did not report startup complete")


def run_k6(script_name: str, result_name: str, base_url: str, vus: int, duration: str) -> None:
    """运行一个 k6 场景，并把完整输出写入结果文件。

    Args:
        script_name: k6 脚本文件名。
        result_name: 结果文件名。
        base_url: 被压测的 ModelService 基础 URL。
        vus: k6 虚拟用户数。
        duration: k6 运行时长。

    Returns:
        None。

    Raises:
        RuntimeError: k6 进程返回非 0 状态码时抛出。
    """

    RESULTS_DIR.mkdir(parents=True, exist_ok=True)
    result_path = RESULTS_DIR / result_name
    command = [
        "k6",
        "run",
        str(REPO_ROOT / "performance-test" / "scripts" / script_name),
        "-e",
        f"BASE_URL={base_url}",
        "-e",
        f"VUS={vus}",
        "-e",
        f"DURATION={duration}",
        "-e",
        "PROFILE=modelservice-dynamic-batching",
    ]
    with result_path.open("w", encoding="utf-8") as output:
        completed = subprocess.run(
            command,
            cwd=str(REPO_ROOT),
            stdout=output,
            stderr=subprocess.STDOUT,
            text=True,
            check=False,
        )
    if completed.returncode != 0:
        raise RuntimeError(f"k6 failed for {script_name}; see {result_path}")
    print(f"wrote {result_path}")


def main() -> None:
    """启动一次 ModelService，然后串行运行文本和图片 k6 场景。

    Args:
        None。

    Returns:
        None。
    """

    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8001)
    parser.add_argument("--vus", type=int, default=25)
    parser.add_argument("--duration", default="40s")
    parser.add_argument("--health-timeout", type=int, default=180)
    args = parser.parse_args()

    sys.path.insert(0, str(MODELSERVICE_DIR))
    os.chdir(MODELSERVICE_DIR)
    import main as modelservice_main

    base_url = f"http://{args.host}:{args.port}"
    assert_port_available(args.host, args.port)
    server = uvicorn.Server(
        uvicorn.Config(
            modelservice_main.app,
            host=args.host,
            port=args.port,
            log_level="warning",
            access_log=False,
        )
    )
    thread = threading.Thread(target=server.run, daemon=True)
    thread.start()

    try:
        wait_for_server_started(server, thread, args.health_timeout)
        wait_for_health(base_url, args.health_timeout)
        run_k6(
            "modelservice-text-vectorize-baseline.js",
            f"modelservice-text-vectorize-vu{args.vus}-{args.duration}.txt",
            base_url,
            args.vus,
            args.duration,
        )
        run_k6(
            "modelservice-image-vectorize-baseline.js",
            f"modelservice-image-vectorize-vu{args.vus}-{args.duration}.txt",
            base_url,
            args.vus,
            args.duration,
        )
    finally:
        server.should_exit = True
        thread.join(timeout=30)


if __name__ == "__main__":
    main()
