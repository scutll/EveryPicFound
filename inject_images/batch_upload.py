#!/usr/bin/env python
"""
批量图片上传脚本 - EveryPicFound 图片入库工具

用法:
    # 测试模式: 只上传前5张图片
    python batch_upload.py --test

    # 测试模式: 上传指定数量
    python batch_upload.py --test --test-count 10

    # 正式批量上传
    python batch_upload.py

    # 指定并发数
    python batch_upload.py --workers 8

    # 指定图片目录和API地址
    python batch_upload.py --image-dir ./images --base-url http://localhost:8080

依赖: pip install requests tqdm
"""

import argparse
import json
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional

import requests
from tqdm import tqdm


# ============================================================
# 配置
# ============================================================

ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}
MAX_FILE_SIZE = 10 * 1024 * 1024  # 10MB
DEFAULT_BASE_URL = "http://localhost:8080"
UPLOAD_PATH = "/api/images/upload"
DEFAULT_WORKERS = 4
REQUEST_TIMEOUT = 60  # 秒


# ============================================================
# 数据结构
# ============================================================

@dataclass
class UploadResult:
    """单张图片上传结果"""
    file_path: str
    relative_path: str
    file_size: int
    success: bool
    status_code: Optional[int] = None
    response_body: Optional[dict] = None
    error_message: Optional[str] = None
    elapsed_seconds: float = 0.0


@dataclass
class BatchReport:
    """批量上传报告"""
    total: int = 0
    success: int = 0
    failed: int = 0
    skipped: int = 0
    start_time: str = ""
    end_time: str = ""
    total_elapsed: float = 0.0
    failures: list = field(default_factory=list)
    skip_reasons: list = field(default_factory=list)


# ============================================================
# 工具函数
# ============================================================

def should_upload(file_path: str) -> tuple[bool, Optional[str]]:
    """
    检查文件是否应该上传。
    返回 (是否上传, 跳过原因)
    """
    ext = os.path.splitext(file_path)[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        return False, f"不支持的文件扩展名: {ext}"

    try:
        size = os.path.getsize(file_path)
    except OSError as e:
        return False, f"无法获取文件大小: {e}"

    if size > MAX_FILE_SIZE:
        return False, f"文件过大: {size / 1024 / 1024:.1f}MB > 10MB"

    if size == 0:
        return False, "文件为空"

    return True, None


def upload_single(
    file_path: str,
    base_url: str,
    timeout: int = REQUEST_TIMEOUT,
) -> UploadResult:
    """上传单张图片"""
    url = f"{base_url.rstrip('/')}{UPLOAD_PATH}"
    file_size = os.path.getsize(file_path)
    result = UploadResult(
        file_path=file_path,
        relative_path=file_path,
        file_size=file_size,
        success=False,
    )

    start = time.time()
    try:
        with open(file_path, "rb") as f:
            files = {"imageFile": (os.path.basename(file_path), f, _guess_mime(file_path))}
            resp = requests.post(url, files=files, timeout=timeout)

        result.elapsed_seconds = time.time() - start
        result.status_code = resp.status_code

        try:
            result.response_body = resp.json()
        except (ValueError, json.JSONDecodeError):
            result.response_body = {"raw": resp.text[:500]}

        if resp.status_code == 200:
            inner_code = result.response_body.get("code", "")
            if inner_code == "SUCCESS" or inner_code == 0:
                result.success = True
            else:
                result.error_message = (
                    f"业务错误: code={inner_code}, "
                    f"message={result.response_body.get('message', '')}"
                )
        else:
            result.error_message = (
                f"HTTP {resp.status_code}: {resp.text[:300]}"
            )

    except requests.Timeout:
        result.elapsed_seconds = time.time() - start
        result.error_message = "请求超时"
    except requests.ConnectionError as e:
        result.elapsed_seconds = time.time() - start
        result.error_message = f"连接失败: {e}"
    except FileNotFoundError:
        result.elapsed_seconds = time.time() - start
        result.error_message = "文件不存在"
    except Exception as e:
        result.elapsed_seconds = time.time() - start
        result.error_message = f"未知错误: {type(e).__name__}: {e}"

    return result


def _guess_mime(file_path: str) -> str:
    """根据扩展名猜测 MIME 类型"""
    ext = os.path.splitext(file_path)[1].lower()
    mapping = {
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".png": "image/png",
        ".webp": "image/webp",
    }
    return mapping.get(ext, "application/octet-stream")


def collect_images(image_dir: str) -> tuple[list[str], list[dict]]:
    """
    收集目录下所有需要上传的图片文件。
    返回 (待上传列表, 跳过列表)
    """
    to_upload = []
    skipped = []

    for root, dirs, files in os.walk(image_dir):
        for fname in files:
            file_path = os.path.join(root, fname)
            ok, reason = should_upload(file_path)
            if ok:
                to_upload.append(file_path)
            else:
                skipped.append({
                    "file_path": file_path,
                    "reason": reason,
                })

    return to_upload, skipped


# ============================================================
# 主流程
# ============================================================

def run_batch_upload(
    image_dir: str,
    base_url: str,
    workers: int = DEFAULT_WORKERS,
    test_count: Optional[int] = None,
    report_path: Optional[str] = None,
):
    """执行批量上传"""
    print("=" * 60)
    print("EveryPicFound 批量图片上传工具")
    print("=" * 60)
    print(f"图片目录:   {image_dir}")
    print(f"API地址:    {base_url}{UPLOAD_PATH}")
    print(f"并发数:     {workers}")
    if test_count:
        print(f"模式:       测试 (仅上传前 {test_count} 张)")
    else:
        print(f"模式:       正式批量上传")
    print()

    # 1. 检查API可达性
    print("[1/4] 检查服务连通性...")
    try:
        resp = requests.get(f"{base_url.rstrip('/')}/api/images/upload", timeout=5)
    except requests.ConnectionError:
        print("⚠ 警告: 无法连接到后端服务，请确认服务已启动")
        print(f"   尝试连接: {base_url}")
        ans = input("是否继续? [y/N]: ")
        if ans.lower() != "y":
            print("已取消")
            return None
    except Exception as e:
        print(f"⚠ 警告: {e}")

    # 2. 收集图片文件
    print("[2/4] 扫描图片文件...")
    to_upload, skipped = collect_images(image_dir)

    print(f"  待上传: {len(to_upload)} 张")
    print(f"  跳过:   {len(skipped)} 张")
    for s in skipped[:5]:
        print(f"    - {os.path.basename(s['file_path'])}: {s['reason']}")
    if len(skipped) > 5:
        print(f"    ... 共 {len(skipped)} 条跳过记录")

    if not to_upload:
        print("没有需要上传的图片")
        return None

    # 3. 测试模式截取
    if test_count and test_count < len(to_upload):
        to_upload = to_upload[:test_count]
        print(f"\n  [测试模式] 实际上传: {len(to_upload)} 张")

    # 4. 上传
    print(f"\n[3/4] 开始上传...")
    report = BatchReport(
        total=len(to_upload),
        start_time=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    )

    start_all = time.time()
    results: list[UploadResult] = []

    with ThreadPoolExecutor(max_workers=workers) as executor:
        futures = {
            executor.submit(upload_single, fp, base_url): fp
            for fp in to_upload
        }

        pbar = tqdm(
            total=len(futures),
            desc="上传进度",
            unit="张",
            ncols=100,
        )

        for future in as_completed(futures):
            try:
                r = future.result()
            except Exception as e:
                fp = futures[future]
                r = UploadResult(
                    file_path=fp,
                    relative_path=fp,
                    file_size=0,
                    success=False,
                    error_message=f"线程异常: {e}",
                )
            results.append(r)

            if r.success:
                report.success += 1
                pbar.set_postfix_str(f"✓ OK {os.path.basename(r.file_path)[:20]}")
            else:
                report.failed += 1
                pbar.set_postfix_str(f"✗ FAIL {os.path.basename(r.file_path)[:20]}")

            pbar.update(1)

        pbar.close()

    report.total_elapsed = time.time() - start_all
    report.end_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    # 收集失败详情
    for r in results:
        if not r.success:
            report.failures.append({
                "file_path": r.file_path,
                "file_size": r.file_size,
                "error_message": r.error_message,
                "status_code": r.status_code,
                "response_body": r.response_body,
                "elapsed_seconds": round(r.elapsed_seconds, 2),
            })

    report.skipped = len(skipped)
    report.skip_reasons = skipped

    # 打印报告
    print_report(report)

    # 保存报告
    if report_path is None:
        report_path = f"upload_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"

    save_report(report, report_path)

    return report


def print_report(report: BatchReport):
    """打印上传报告"""
    print()
    print("=" * 60)
    print("上传报告")
    print("=" * 60)
    print(f"开始时间:   {report.start_time}")
    print(f"结束时间:   {report.end_time}")
    print(f"总耗时:     {report.total_elapsed:.1f} 秒")
    print(f"总图片数:   {report.total}")
    print(f"上传成功:   {report.success} ✓")
    print(f"上传失败:   {report.failed} ✗")
    print(f"跳过:       {report.skipped}")

    if report.total > 0:
        rate = report.success / report.total * 100
        print(f"成功率:     {rate:.1f}%")
        if report.total_elapsed > 0:
            qps = report.total / report.total_elapsed
            print(f"平均速度:   {qps:.1f} 张/秒")

    if report.failures:
        print()
        print("-" * 60)
        print(f"失败详情 ({len(report.failures)} 条):")
        print("-" * 60)
        # 按错误类型分组统计
        from collections import Counter
        error_types = Counter(
            f.get("error_message", "未知")[:80] for f in report.failures
        )
        print("错误类型分布:")
        for err_msg, count in error_types.most_common():
            print(f"  [{count:4d}] {err_msg}")

        print()
        print("前20条失败记录:")
        for i, f in enumerate(report.failures[:20], 1):
            fname = os.path.basename(f["file_path"])
            print(f"  {i:2d}. {fname}")
            print(f"      错误: {f['error_message']}")
            if f.get("status_code"):
                print(f"      HTTP状态: {f['status_code']}")


def save_report(report: BatchReport, path: str):
    """保存JSON报告"""
    data = {
        "start_time": report.start_time,
        "end_time": report.end_time,
        "total_elapsed_seconds": round(report.total_elapsed, 2),
        "total": report.total,
        "success": report.success,
        "failed": report.failed,
        "skipped": report.skipped,
        "failures": report.failures,
        "skip_reasons": [
            {"file_path": s["file_path"], "reason": s["reason"]}
            for s in report.skip_reasons
        ],
    }
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"\n详细报告已保存到: {path}")


# ============================================================
# CLI 入口
# ============================================================

def main():
    parser = argparse.ArgumentParser(
        description="EveryPicFound 批量图片上传工具",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "--image-dir",
        default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "images"),
        help="图片目录路径 (默认: 脚本所在目录下的 images/)",
    )
    parser.add_argument(
        "--base-url",
        default=DEFAULT_BASE_URL,
        help=f"后端API基础地址 (默认: {DEFAULT_BASE_URL})",
    )
    parser.add_argument(
        "--workers",
        type=int,
        default=DEFAULT_WORKERS,
        help=f"并发上传数 (默认: {DEFAULT_WORKERS})",
    )
    parser.add_argument(
        "--test",
        action="store_true",
        help="测试模式: 仅上传少量图片进行验证",
    )
    parser.add_argument(
        "--test-count",
        type=int,
        default=5,
        help="测试模式下上传的图片数量 (默认: 5)",
    )
    parser.add_argument(
        "--report-path",
        default=None,
        help="自定义报告输出路径",
    )

    args = parser.parse_args()

    if not os.path.isdir(args.image_dir):
        print(f"错误: 图片目录不存在: {args.image_dir}")
        sys.exit(1)

    test_count = args.test_count if args.test else None

    report = run_batch_upload(
        image_dir=args.image_dir,
        base_url=args.base_url,
        workers=args.workers,
        test_count=test_count,
        report_path=args.report_path,
    )

    if report is None:
        sys.exit(1)

    if report.failed > 0:
        sys.exit(1)


if __name__ == "__main__":
    main()
