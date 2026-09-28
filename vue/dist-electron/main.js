import { app as m, BrowserWindow as u } from "electron";
import n from "node:path";
import h from "node:fs";
import g from "node:http";
import { fileURLToPath as W } from "node:url";
const w = n.dirname(W(import.meta.url)), x = !!process.env.VITE_DEV_SERVER_URL, E = process.env.VITE_BACKEND_URL || "http://localhost:8081";
let a = null, f = null;
const S = {
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
function y(t) {
  return t.startsWith("/api/user/") || t.startsWith("/api/recommend") || t.startsWith("/api/admin") ? t : t.startsWith("/api") ? t.replace(/^\/api/, "") || "/" : t;
}
function _(t, o, p) {
  const i = new URL(p, E), r = { ...t.headers, host: i.host };
  delete r.origin;
  const e = g.request(
    i,
    { method: t.method, headers: r },
    (c) => {
      o.writeHead(c.statusCode || 502, c.headers), c.pipe(o);
    }
  );
  e.on("error", () => {
    o.writeHead(502, { "Content-Type": "text/plain; charset=utf-8" }), o.end("后端未启动：请先启动 Spring Boot（端口 8081）");
  }), t.pipe(e);
}
function L(t) {
  return new Promise((o, p) => {
    const i = g.createServer((r, e) => {
      try {
        const c = new URL(r.url || "/", "http://127.0.0.1"), s = decodeURIComponent(c.pathname);
        if (s.startsWith("/api") || s.startsWith("/novel") || s.startsWith("/cover") || s.startsWith("/auth")) {
          const R = s.startsWith("/api") ? y(s) : s;
          _(r, e, R + c.search);
          return;
        }
        let d = n.join(t, s === "/" ? "index.html" : s);
        const l = n.resolve(d);
        if (!l.startsWith(n.resolve(t))) {
          e.writeHead(403), e.end();
          return;
        }
        !h.existsSync(l) || h.statSync(l).isDirectory() ? d = n.join(t, "index.html") : d = l;
        const j = n.extname(d).toLowerCase();
        e.writeHead(200, { "Content-Type": S[j] || "application/octet-stream" }), h.createReadStream(d).pipe(e);
      } catch {
        e.writeHead(500), e.end("Server error");
      }
    });
    i.once("error", p), i.listen(0, "127.0.0.1", () => {
      const r = i.address(), e = typeof r == "object" && r ? r.port : 0;
      o({ server: i, url: `http://127.0.0.1:${e}` });
    });
  });
}
function U() {
  return [
    n.join(w, "../build/icon.ico"),
    n.join(w, "../build/icon.png"),
    n.join(process.resourcesPath || "", "build/icon.ico")
  ].find((o) => h.existsSync(o));
}
async function v() {
  const t = U();
  if (a = new u({
    width: 1280,
    height: 840,
    minWidth: 960,
    minHeight: 640,
    title: "轻小说推荐系统",
    icon: t,
    webPreferences: {
      nodeIntegration: !1,
      contextIsolation: !0
    },
    autoHideMenuBar: !0,
    show: !1
  }), a.once("ready-to-show", () => a?.show()), x)
    await a.loadURL(process.env.VITE_DEV_SERVER_URL), a.webContents.openDevTools({ mode: "detach" });
  else {
    const o = n.join(w, "../dist"), { server: p, url: i } = await L(o);
    f = p, await a.loadURL(i);
  }
  a.on("closed", () => {
    a = null;
  });
}
m.whenReady().then(() => {
  v();
});
m.on("window-all-closed", () => {
  f && (f.close(), f = null), process.platform !== "darwin" && m.quit();
});
m.on("activate", () => {
  u.getAllWindows().length === 0 && v();
});
