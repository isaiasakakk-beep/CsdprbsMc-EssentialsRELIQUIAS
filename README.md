# CSDPBRS ESSENTIALS RELIQUIAS

Plugin independente para Paper, dedicado a chefes, fragmentos, relíquias e ao NPC Forjador de Relíquias.

## Estado do desenvolvimento

Esta é a primeira estrutura do projeto. Já estão iniciados:

- Projeto Java/Gradle e metadados do plugin.
- Relógio das Relíquias e primeiro menu de coleção.
- Configuração inicial de quatro raridades de chefes.
- Gerenciador de surgimento aleatório e intervalo de reaparecimento.
- Avisos alternados: nome + dimensão e aviso genérico sem localização.
- Drops iniciais de fragmentos por raridade.

Ainda em desenvolvimento: as 20 relíquias e seus poderes, menus completos, missões, persistência dos tempos de reaparecimento, NPC Forjador funcional, proteção contra exploração dos itens e testes/compilação na versão exata do servidor.

## Requisitos planejados

- Paper 26.3
- Java 25
- Geyser/Floodgate para jogadores Bedrock (os menus usam inventários Bukkit compartilhados)

## Build

Com Gradle instalado, execute:

```bash
gradle build
```

O JAR será gerado em `build/libs/`.

## Comandos administrativos iniciais

- `/reliquias relogio <jogador>`
- `/reliquias boss <raro|epico|mitico|lendario>`
- `/reliquias reload`

Permissão: `reliquias.admin` (OP por padrão).

## Configuração

Edite `plugins/CSDPBRS-ESSENTIALS-RELIQUIAS/config.yml` após a primeira inicialização. Os nomes dos mundos padrão são `world`, `world_nether` e `world_the_end`; ajuste-os se seu servidor usar outros nomes.
