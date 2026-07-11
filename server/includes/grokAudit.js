const https = require('https');

const DEFAULT_MODEL = process.env.XAI_MODEL || 'grok-4-latest';
const API_HOST = 'api.x.ai';
const API_PATH = '/v1/chat/completions';
const MAX_LOG_ITEMS = 200;

function normalizeLimit(limit) {
    const parsed = parseInt(limit, 10);
    if (Number.isNaN(parsed) || parsed <= 0) return 50;
    return Math.min(parsed, MAX_LOG_ITEMS);
}

function getApiKey() {
    return process.env.XAI_API_KEY || '';
}

function buildPrompt(logs, question) {
    const defaultQuestion = 'Resuma riscos, anomalias e recomendações defensivas a partir destes logs.';
    const instruction = question && question.toString().trim().length > 0
        ? question.toString().trim()
        : defaultQuestion;

    return [
        'Voce e um analista de seguranca defensiva para laboratorio autorizado.',
        'Nao proponha ofensiva e nao gere comandos de controle remoto.',
        'Retorne apenas achados de risco, severidade, evidencias e mitigacoes.',
        '',
        'Pergunta do operador:',
        instruction,
        '',
        'Logs (JSON):',
        JSON.stringify(logs, null, 2)
    ].join('\n');
}

function callXai(apiKey, payload) {
    return new Promise((resolve, reject) => {
        const body = JSON.stringify(payload);
        const options = {
            hostname: API_HOST,
            path: API_PATH,
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${apiKey}`,
                'Content-Type': 'application/json',
                'Content-Length': Buffer.byteLength(body)
            },
            timeout: 20000
        };

        const req = https.request(options, (res) => {
            let rawData = '';
            res.on('data', (chunk) => {
                rawData += chunk;
            });

            res.on('end', () => {
                let parsed;
                try {
                    parsed = rawData ? JSON.parse(rawData) : {};
                } catch (error) {
                    return reject(new Error('Resposta invalida da API do Grok'));
                }

                if (res.statusCode < 200 || res.statusCode >= 300) {
                    const apiMessage = parsed && parsed.error && parsed.error.message
                        ? parsed.error.message
                        : `Falha na API do Grok (${res.statusCode})`;
                    return reject(new Error(apiMessage));
                }

                const text = parsed && parsed.choices && parsed.choices[0]
                    && parsed.choices[0].message && parsed.choices[0].message.content
                    ? parsed.choices[0].message.content
                    : '';

                if (!text) {
                    return reject(new Error('A API do Grok nao retornou conteudo'));
                }

                resolve(text);
            });
        });

        req.on('error', (error) => reject(error));
        req.on('timeout', () => {
            req.destroy(new Error('Tempo limite ao consultar API do Grok'));
        });

        req.write(body);
        req.end();
    });
}

async function summarizeLogs(logs, options) {
    const apiKey = getApiKey();
    if (!apiKey) {
        throw new Error('XAI_API_KEY nao configurada no ambiente');
    }

    const safeLogs = Array.isArray(logs) ? logs : [];
    const limit = normalizeLimit(options && options.limit);
    const sliced = safeLogs.slice(0, limit);
    const question = options && options.question ? options.question : undefined;

    const payload = {
        model: DEFAULT_MODEL,
        temperature: 0.2,
        messages: [
            {
                role: 'system',
                content: 'Voce atua em monitoramento defensivo e resposta a incidentes de laboratorio.'
            },
            {
                role: 'user',
                content: buildPrompt(sliced, question)
            }
        ]
    };

    const summary = await callXai(apiKey, payload);
    return {
        model: DEFAULT_MODEL,
        limit,
        summary
    };
}

module.exports = {
    summarizeLogs
};