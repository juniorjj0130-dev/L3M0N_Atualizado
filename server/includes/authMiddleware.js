// includes/authMiddleware.js
const jwt = require('jsonwebtoken');

const authenticateToken = (req, res, next) => {
    const token = req.cookies?.authToken;

    if (!token) {
        return res.status(401).json({ success: false, error: "Não autenticado. Faça login." });
    }

    jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
        if (err) {
            return res.status(403).json({ success: false, error: "Sessão inválida ou expirada" });
        }
        req.user = user;
        next();
    });
};

module.exports = authenticateToken;