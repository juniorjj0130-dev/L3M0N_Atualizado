const fs = require("fs");
const path = require("path");
const https = require("https");

/**
 * SSL/TLS Configuration for L3MON
 * Supports both development (self-signed) and production certificates
 */

const SSL_CONFIG = {
  enabled: true,
  // Default cert paths (mkcert style)
  keyPath: path.join(__dirname, "../localhost+2-key.pem"),
  certPath: path.join(__dirname, "../localhost+2.pem"),
  // For production, override with real certs
  // keyPath: '/etc/letsencrypt/live/yourdomain.com/privkey.pem',
  // certPath: '/etc/letsencrypt/live/yourdomain.com/fullchain.pem',

  // Certificate pins (SHA256 of Subject Public Key Info)
  // Generate with: openssl x509 -in cert.pem -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl base64
  // These are example pins - REPLACE WITH YOUR ACTUAL CERT PINS IN PRODUCTION
  pins: [
    // 'sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=', // Example - replace
  ],

  // Allow self-signed in development (client should still pin if possible)
  allowSelfSigned: true,

  // TLS options
  minVersion: "TLSv1.2",
  ciphers: [
    "ECDHE-ECDSA-AES128-GCM-SHA256",
    "ECDHE-RSA-AES128-GCM-SHA256",
    "ECDHE-ECDSA-AES256-GCM-SHA384",
    "ECDHE-RSA-AES256-GCM-SHA384",
    "ECDHE-ECDSA-CHACHA20-POLY1305",
    "ECDHE-RSA-CHACHA20-POLY1305",
    "DHE-RSA-AES128-GCM-SHA256",
    "DHE-RSA-AES256-GCM-SHA384",
  ].join(":"),
};

/**
 * Creates an HTTPS server with proper TLS configuration
 */
function createHttpsServer(expressApp) {
  if (!SSL_CONFIG.enabled) {
    throw new Error("HTTPS is disabled in sslConfig");
  }

  const key = fs.readFileSync(SSL_CONFIG.keyPath);
  const cert = fs.readFileSync(SSL_CONFIG.certPath);

  const httpsOptions = {
    key: key,
    cert: cert,
    minVersion: SSL_CONFIG.minVersion,
    ciphers: SSL_CONFIG.ciphers,
    // For self-signed certs in dev, we don't reject unauthorized here (server side)
    // Client side pinning handles security
  };

  return https.createServer(httpsOptions, expressApp);
}

/**
 * Returns the current SSL configuration
 */
function getConfig() {
  return { ...SSL_CONFIG };
}

/**
 * Updates certificate pins (can be called at runtime or from config)
 */
function setPins(newPins) {
  if (Array.isArray(newPins)) {
    SSL_CONFIG.pins = newPins;
  }
}

module.exports = {
  createHttpsServer,
  getConfig,
  setPins,
  SSL_CONFIG,
};
