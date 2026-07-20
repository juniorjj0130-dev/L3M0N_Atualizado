// === ROTAS DE AUTENTICAÇÃO (Adicione ou substitua no arquivo de rotas) ===

const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const rateLimit = require('express-rate-limit');
const low = require('lowdb');
const FileSync = require('lowdb/adapters/FileSync');

const adapter = new FileSync('maindb.json');
const db = low(adapter);

async function main() {
    const username = "admin";
    const password = "RedTeam2026!";   // ← MUDE PARA UMA SENHA FORTE

    const hash = await bcrypt.hash(password, 12);

    db.set('admin.username', username).write();
    db.set('admin.password', hash).write();
    db.unset('admin.loginToken').write(); // Remove token antigo

    console.log("✅ Senha atualizada!");
    console.log("Username:", username);
    console.log("Senha:", password);
}

main().catch(console.error);

// Rate Limiting para login
const loginLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutos
    max: 8,
    message: { success: false, error: "Muitas tentativas. Aguarde 15 minutos." },
    standardHeaders: true,
    legacyHeaders: false
});

// Rota de Login Melhorada
app.post('/login', loginLimiter, async (req, res) => {
    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ success: false, error: "Usuário e senha são obrigatórios" });
        }

        const admin = global.db.get('admin').value();

        if (!admin || admin.username !== username) {
            return res.status(401).json({ success: false, error: "Credenciais inválidas" });
        }

        const validPassword = await bcrypt.compare(password, admin.password);
        if (!validPassword) {
            return res.status(401).json({ success: false, error: "Credenciais inválidas" });
        }

        // Gera JWT
        const token = jwt.sign(
            { id: admin.id || 1, username: admin.username },
            process.env.JWT_SECRET,
            { expiresIn: '12h' }
        );

        // Cookie seguro
        res.cookie('authToken', token, {
            httpOnly: true,
            secure: process.env.NODE_ENV === 'production',
            sameSite: 'strict',
            maxAge: 12 * 60 * 60 * 1000
        });

        res.json({ 
            success: true, 
            message: "Login realizado com sucesso" 
        });

    } catch (err) {
        console.error("Erro no login:", err);
        res.status(500).json({ success: false, error: "Erro interno do servidor" });
    }
});

// Rota de Logout
app.post('/logout', (req, res) => {
    res.clearCookie('authToken');
    res.json({ success: true, message: "Logout realizado" });
});