# Lab Security Checklist

Este checklist foca em seguranca defensiva para laboratorio local e ambientes de pesquisa autorizados.

## 1) Isolamento do Ambiente

- Execute o servidor em rede segregada (VLAN/host-only/NAT de laboratorio)
- Bloqueie portas de administracao para redes externas
- Use snapshots de VM/emulador antes de cada rodada de testes
- Nao reutilize dispositivos pessoais no laboratorio

## 2) Endurecimento do Servidor

- Habilite proxy reverso com TLS no acesso web
- Restrinja IPs permitidos para a interface de administracao
- Monitore tentativas de login e bloqueios por excesso de tentativas
- Mantenha o Node.js e dependencias atualizados
- Rode o processo com usuario sem privilegios

## 3) Seguranca de Credenciais

- Nao use senhas padrao
- Defina senha forte para o admin e troque periodicamente
- Evite armazenar segredos em texto puro no repositorio
- Use variaveis de ambiente para configuracoes sensiveis

## 4) Observabilidade e Auditoria

- Centralize logs do servidor com carimbo de data/hora
- Registre eventos de autenticacao (sucesso/falha/logout)
- Defina retencao minima de logs para investigacao
- Crie alertas para picos de erro e tentativas de acesso

## 5) Testes e Qualidade

- Execute testes automatizados antes de cada release
- Adicione testes para autenticacao, sessao e controle de acesso
- Faça revisao de codigo para cada alteracao critica
- Valide regressao de rotas e paginas principais

## 6) Governanca do Laboratorio

- Mantenha termo de autorizacao e escopo por escrito
- Registre responsavel, periodo e objetivo de cada experimento
- Defina criterio claro de encerramento e limpeza do ambiente
- Apague artefatos sensiveis ao final dos testes

## 7) Plano de Resposta

- Tenha procedimento de rollback rapido (snapshot/backup)
- Defina quem acionar em caso de incidente
- Isole imediatamente o ambiente em caso de comportamento inesperado
- Revise causa raiz e atualize este checklist apos cada incidente
