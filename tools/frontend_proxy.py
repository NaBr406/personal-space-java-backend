#!/usr/bin/env python3
"""简易联调代理：
- /api/* 和 /uploads/* 转发到 Java 后端
- 其它路径直接从现有前端 public 目录读取

默认：
  前端目录: /root/.openclaw/workspace/personal-space-java-sandbox/public
  后端地址: http://127.0.0.1:3001
  监听地址: http://127.0.0.1:8081
"""

from __future__ import annotations

import argparse
import http.server
import mimetypes
import os
import socketserver
import urllib.error
import urllib.request
from urllib.parse import urlsplit


class FrontendProxyHandler(http.server.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    backend_base = "http://127.0.0.1:3001"
    public_dir = "."

    def do_GET(self):
        self.handle_all()

    def do_POST(self):
        self.handle_all()

    def do_PUT(self):
        self.handle_all()

    def do_PATCH(self):
        self.handle_all()

    def do_DELETE(self):
        self.handle_all()

    def do_OPTIONS(self):
        self.handle_all()

    def do_HEAD(self):
        self.handle_all(head_only=True)

    def handle_all(self, head_only: bool = False):
        if self.path.startswith("/api/") or self.path.startswith("/uploads/"):
            self.proxy_request(head_only=head_only)
        else:
            self.serve_static(head_only=head_only)

    def proxy_request(self, head_only: bool = False):
        length = int(self.headers.get("Content-Length", "0") or "0")
        body = self.rfile.read(length) if length else None

        req = urllib.request.Request(self.backend_base + self.path, data=body, method=self.command)
        skip_headers = {"host", "connection", "content-length"}
        for key, value in self.headers.items():
            if key.lower() not in skip_headers:
                req.add_header(key, value)

        try:
            with urllib.request.urlopen(req, timeout=60) as resp:
                data = resp.read()
                self.send_response(resp.status)
                for key, value in resp.headers.items():
                    if key.lower() in {"transfer-encoding", "connection", "content-encoding"}:
                        continue
                    self.send_header(key, value)
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                if not head_only:
                    self.wfile.write(data)
        except urllib.error.HTTPError as err:
            data = err.read()
            self.send_response(err.code)
            for key, value in err.headers.items():
                if key.lower() in {"transfer-encoding", "connection", "content-encoding"}:
                    continue
                self.send_header(key, value)
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            if not head_only:
                self.wfile.write(data)
        except Exception as err:  # noqa: BLE001
            data = f"proxy error: {err}".encode("utf-8")
            self.send_response(502)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            if not head_only:
                self.wfile.write(data)

    def serve_static(self, head_only: bool = False):
        request_path = urlsplit(self.path).path
        if request_path in ("", "/"):
            request_path = "/index.html"

        full_path = os.path.normpath(os.path.join(self.public_dir, request_path.lstrip("/")))
        if not full_path.startswith(self.public_dir):
            self.send_error(403)
            return

        if os.path.isdir(full_path):
            full_path = os.path.join(full_path, "index.html")

        if not os.path.exists(full_path):
            self.send_error(404)
            return

        with open(full_path, "rb") as file:
            data = file.read()

        content_type = mimetypes.guess_type(full_path)[0] or "application/octet-stream"
        self.send_response(200)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        if not head_only:
            self.wfile.write(data)

    def log_message(self, fmt, *args):
        return


def main():
    parser = argparse.ArgumentParser(description="给 Java 后端做前端联调代理")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8081)
    parser.add_argument(
        "--backend",
        default="http://127.0.0.1:3001",
        help="Java 后端地址，默认 http://127.0.0.1:3001",
    )
    parser.add_argument(
        "--public-dir",
        default="/root/.openclaw/workspace/personal-space-java-sandbox/public",
        help="现有前端 public 目录",
    )
    args = parser.parse_args()

    FrontendProxyHandler.backend_base = args.backend.rstrip("/")
    FrontendProxyHandler.public_dir = os.path.abspath(args.public_dir)

    with socketserver.ThreadingTCPServer((args.host, args.port), FrontendProxyHandler) as httpd:
        print(f"frontend proxy listening on http://{args.host}:{args.port}")
        print(f"backend -> {FrontendProxyHandler.backend_base}")
        print(f"public  -> {FrontendProxyHandler.public_dir}")
        httpd.serve_forever()


if __name__ == "__main__":
    main()
