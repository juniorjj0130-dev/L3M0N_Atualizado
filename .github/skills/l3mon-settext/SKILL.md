# SetText (Injeção de Texto) - L3MON

## Visão Geral

O recurso de **Injeção de Texto (SetText)** permite enviar texto para campos de texto editáveis em um dispositivo Android usando o método `ACTION_SET_TEXT` do Accessibility Service do Android.

Este recurso funciona apenas em dispositivos que têm:
- Accessibility Service ativado
- Permissions para Accessibility concedidas

## Arquitetura

### Fluxo de Dados

```
Web UI (text_injection.ejs)
    ↓ HTTP POST /manage/:deviceid/0xST
    ↓
Express Route (expressRoutes.js)
    ↓ Valida parâmetros
    ↓
ClientManager.sendCommand (clientManager.js)
    ↓ Envia via Socket.IO com tipo '0xST'
    ↓
Dispositivo Android - ConnectionManager.java
    ↓ Recebe "order" com type '0xST'
    ↓
ConnectionManager.ST() (ConnectionManager.java)
    ↓
AccessibilityCaptureService.setTextIntoField()
    ↓ Executa ACTION_SET_TEXT
    ↓
Campo de Texto é preenchido
    ↓
Emite resposta de sucesso/erro
```

## Implementação

### 1. **Servidor (Node.js/Express)**

#### Chave de Mensagem
- **Arquivo**: `server/includes/const.js`
- **Chave**: `setText: '0xST'`
- **Tipo**: Hex (notação hexadecimal de 16 bits)

#### Validação de Parâmetros
- **Arquivo**: `server/includes/clientManager.js` - Função `checkCorrectParams()`
- **Parâmetro obrigatório**: `text` (string não-vazia)
- **Parâmetro opcional**: `viewId` (para especificar campo alvo)

#### Listener de Resposta
- **Arquivo**: `server/includes/clientManager.js` - Função `setupListeners()`
- **Log**: Registra sucesso/erro no sistema de logging

#### Rota Express
- **Arquivo**: `server/includes/expressRoutes.js`
- **Método**: POST
- **Rota**: `/manage/:deviceid/0xST`
- **Parâmetros**: `text`, `viewId` (opcional)

#### Interface Web
- **Arquivo**: `server/assets/views/deviceManagerPages/text_injection.ejs`
- **Menu**: Adiciona item "Text Injection" ao menu lateral
- **Funcionalidades**:
  - Textarea para digitar o texto a injetar
  - Campo de entrada para especificar View ID (opcional)
  - Listagem dinâmica de campos editáveis encontrados
  - Feedback visual de sucesso/erro

### 2. **Cliente Android (Java)**

#### Imports Necessários
```java
import android.os.Bundle;
```

#### Métodos Principais
- **`setTextIntoField(AccessibilityNodeInfo node, String text)`**
  - Injeta texto em um nó de acessibilidade específico
  - Executa `ACTION_SET_TEXT` com Bundle de argumentos

- **`setTextIntoFieldByViewId(String viewId, String text)`**
  - Busca campo por ID e injeta texto
  - Retorna boolean indicando sucesso

- **`findAndSetTextByViewId(AccessibilityNodeInfo node, String targetViewId, String text)`**
  - Função recursiva para encontrar campo pelo View ID
  - Percorre toda a árvore de acessibilidade

#### Integração com ConnectionManager
- **Arquivo**: `client/app/src/main/java/com/etechd/l3mon/ConnectionManager.java`
- **Método**: `ST(JSONObject data)`
- **Ação**: Recebe comando, executa injeção, emite resposta

#### Tratamento de Dados JSON
```javascript
{
    "type": "0xST",
    "text": "Texto a injetar",
    "viewId": "com.example:id/editText"  // Opcional
}
```

## Usar via API

### Exemplo 1: Injetar em Qualquer Campo Editável
```bash
curl -X POST "http://localhost:22533/manage/device123/0xST?text=Minha%20senha" \
  -H "Cookie: loginToken=seu_token"
```

### Exemplo 2: Injetar em Campo Específico
```bash
curl -X POST "http://localhost:22533/manage/device123/0xST" \
  -d "text=usuario@example.com&viewId=com.example:id/emailField" \
  -H "Cookie: loginToken=seu_token"
```

### Resposta de Sucesso
```json
{
    "error": false,
    "message": "Requested"
}
```

### Resposta de Erro
```json
{
    "error": "SetText Missing `text` Parameter"
}
```

## Fluxo de Uso pela Interface Web

1. **Acesse** o dispositivo gerenciado
2. **Clique** em "Text Injection" no menu lateral
3. **Visualize** os campos editáveis da tela atual
4. **Digite** o texto desejado na textarea
5. **(Opcional)** Especifique o View ID se quiser injetar em campo específico
6. **Clique** em "Enviar Texto"
7. **Aguarde** a resposta de sucesso ou erro

## Limitações

- ✅ Funciona com campos `EditText` e similares
- ✅ Suporta injeção em campos visivelmente ativos
- ❌ Não funciona se Accessibility Service não estiver ativo
- ❌ Requer permissões de Accessibility no dispositivo
- ❌ Não funciona em campos protegidos ou customizados
- ❌ Texto injetado substitui conteúdo anterior

## Segurança

- ✅ Requer autenticação (loginToken)
- ✅ Apenas campos editáveis via Accessibility são alvo
- ✅ Requer ativação explícita de Accessibility Service
- ✅ Validação de entrada no servidor
- ⚠️ Usar com cautela em ambientes de produção

## Debugging

### Habilitar Logs de Debug
Em `server/includes/const.js`:
```javascript
exports.debug = true;
```

### Verificar Eventos Socket.IO
- Monitor em `clientManager.js` quando mensagens '0xST' chegam
- Verifique se `AccessibilityNodeInfo` está acessível

### Validar Acessibilidade no Android
```bash
adb shell dumpsys accessibility
```

## Testes

Execute testes de validação:
```bash
cd server
npm test
```

O teste `settext-module.test.js` valida:
- Presença da chave '0xST'
- Formato correto da chave

## Referências

- [Android Accessibility Service API](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [AccessibilityNodeInfo ACTION_SET_TEXT](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo#ACTION_SET_TEXT)
- [Bundle (Android)](https://developer.android.com/reference/android/os/Bundle)
