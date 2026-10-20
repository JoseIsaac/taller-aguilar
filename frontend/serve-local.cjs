const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');

const root = path.join(__dirname, 'dist', 'taller', 'browser');
const fallback = path.join(__dirname, 'dist', 'taller');
const base = fs.existsSync(path.join(root, 'index.html')) ? root : fallback;
const types = { '.html':'text/html; charset=utf-8', '.js':'text/javascript; charset=utf-8', '.css':'text/css; charset=utf-8', '.svg':'image/svg+xml', '.png':'image/png', '.ico':'image/x-icon' };

http.createServer((req, res) => {
  const pathname = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
  let file = path.resolve(base, `.${pathname}`);
  if (file !== base && !file.startsWith(base + path.sep)) { res.writeHead(403).end('Forbidden'); return; }
  if (!fs.existsSync(file) || fs.statSync(file).isDirectory()) file = path.join(base, 'index.html');
  fs.readFile(file, (err, data) => {
    if (err) { res.writeHead(404).end('Not found'); return; }
    res.writeHead(200, { 'Content-Type': types[path.extname(file)] || 'application/octet-stream' });
    res.end(data);
  });
}).listen(4200, '0.0.0.0', () => console.log('Taller local listo en http://127.0.0.1:4200/'));
