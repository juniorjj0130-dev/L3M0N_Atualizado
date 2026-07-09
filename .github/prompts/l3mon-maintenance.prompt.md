---
description: "Implementar ou ajustar recurso no L3MON de ponta a ponta (server/client), com validacao e testes"
name: "L3MON Maintenance Workflow"
argument-hint: "Descreva a tarefa (ex: adicionar chave 0xXX, rota, view e teste)"
agent: "agent"
---
Voce e um mantenedor do projeto L3MON. Execute a tarefa descrita em {{$ARGUMENTS}}.

Objetivo:
- Implementar a mudanca de forma consistente com a arquitetura atual.
- Evitar regressao de comportamento.
- Validar com testes existentes e/ou novos testes quando necessario.

Contexto obrigatorio:
- Leia [AGENTS](../../AGENTS.md) antes de editar.
- Priorize padroes existentes em server/includes, server/assets/views e client/app/src/main/java.
- Mantenha compatibilidade com as chaves/protocolo ja definidos.

Fluxo de execucao:
1. Entenda o estado atual e localize os arquivos impactados.
2. Implemente a mudanca no menor conjunto de arquivos necessario.
3. Atualize rotas, managers, views e cliente Android quando a tarefa exigir ponta a ponta.
4. Crie/ajuste testes em server/test quando houver mudanca de comportamento.
5. Rode validacoes e relacione resultados.

Criterios de qualidade:
- Nao reverter alteracoes nao relacionadas.
- Nao introduzir refatoracao ampla sem necessidade.
- Preservar estilo do codigo e convencoes existentes.

Formato de saida:
- Resumo curto da solucao.
- Lista de arquivos alterados e motivo.
- Resultado de validacao (testes/comandos executados).
- Riscos residuais e proximos passos, se houver.
