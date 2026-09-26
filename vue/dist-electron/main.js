import { app, BrowserWindow } from "electron";
import path from "node:path";
import fs from "node:fs";
import http from "node:http";
import { fileURLToPath } from "node:url";
const __dirname$1 = path.dirname(fileURLToPath(import.meta.url));
const isDev = !!process.env.VITE_DEV_SERVER_URL;
const BACKEND = process.env.VITE_BACKEND_URL || "http://localhost:8081";
let win = null;
let localServer = null;
const MIME = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".svg": "image/svg+xml",
  ".ico": "image/x-icon",
  ".webp": "image/webp",
  ".woff": "font/woff",
  ".woff2": "font/woff2",
  ".ttf": "font/ttf",
  ".map": "application/json"
};
function rewriteApiPath(urlPath) {
  if (urlPath.startsWith("/api/user/") || urlPath.startsWith("/api/recommend") || urlPath.startsWith("/api/admin")) {
    return urlPath;
  }
  if (urlPath.startsWith("/api")) {
    return urlPath.replace(/^\/api/, "") || "/";
  }
  return urlPath;
}
function proxyToBackend(req, res, targetPathWithQuery) {
  const target = new URL(targetPathWithQuery, BACKEND);
  const headers = { ...req.headers, host: target.host };
  delete headers["origin"];
  const proxyReq = http.request(
    target,
    { method: req.method, headers },
    (proxyRes) => {
      res.writeHead(proxyRes.statusCode || 502, proxyRes.headers);
      proxyRes.pipe(res);
    }
  );
  proxyReq.on("error", () => {
    res.writeHead(502, { "Content-Type": "text/plain; charset=utf-8" });
    res.end("后端未启动：请先启动 Spring Boot（端口 8081）");
  });
  req.pipe(proxyReq);
}
function startStaticServer(distDir) {
  return new Promise((resolve, reject) => {
    const server = http.createServer((req, res) => {
      try {
        const u = new URL(req.url || "/", "http://127.0.0.1");
        const pathname = decodeURIComponent(u.pathname);
        if (pathname.startsWith("/api") || pathname.startsWith("/novel") || pathname.startsWith("/cover") || pathname.startsWith("/auth")) {
          const rewritten = pathname.startsWith("/api") ? rewriteApiPath(pathname) : pathname;
          proxyToBackend(req, res, rewritten + u.search);
          return;
        }
        let filePath = path.join(distDir, pathname === "/" ? "index.html" : pathname);
        const resolved = path.resolve(filePath);
        if (!resolved.startsWith(path.resolve(distDir))) {
          res.writeHead(403);
          res.end();
          return;
        }
        if (!fs.existsSync(resolved) || fs.statSync(resolved).isDirectory()) {
          filePath = path.join(distDir, "index.html");
        } else {
          filePath = resolved;
        }
        const ext = path.extname(filePath).toLowerCase();
        res.writeHead(200, { "Content-Type": MIME[ext] || "application/octet-stream" });
        fs.createReadStream(filePath).pipe(res);
      } catch {
        res.writeHead(500);
        res.end("Server error");
      }
    });
    server.once("error", reject);
    server.listen(0, "127.0.0.1", () => {
      const addr = server.address();
      const port = typeof addr === "object" && addr ? addr.port : 0;
      resolve({ server, url: `http://127.0.0.1:${port}` });
    });
  });
}
function resolveIconPath() {
  const candidates = [
    path.join(__dirname$1, "../build/icon.ico"),
    path.join(__dirname$1, "../build/icon.png"),
    path.join(process.resourcesPath || "", "build/icon.ico")
  ];
  return candidates.find((p) => fs.existsSync(p));
}
async function createWindow() {
  const icon = resolveIconPath();
  win = new BrowserWindow({
    width: 1280,
    height: 840,
    minWidth: 960,
    minHeight: 640,
    title: "轻小说推荐系统",
    icon,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true
    },
    autoHideMenuBar: true,
    show: false
  });
  win.once("ready-to-show", () => win?.show());
  if (isDev) {
    await win.loadURL(process.env.VITE_DEV_SERVER_URL);
    win.webContents.openDevTools({ mode: "detach" });
  } else {
    const distDir = path.join(__dirname$1, "../dist");
    const { server, url } = await startStaticServer(distDir);
    localServer = server;
    await win.loadURL(url);
  }
  win.on("closed", () => {
    win = null;
  });
}
app.whenReady().then(() => {
  createWindow();
});
app.on("window-all-closed", () => {
  if (localServer) {
    localServer.close();
    localServer = null;
  }
  if (process.platform !== "darwin") {
    app.quit();
  }
});
app.on("activate", () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});
