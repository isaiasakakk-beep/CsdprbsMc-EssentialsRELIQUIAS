# CSDPBRS ESSENTIALS RELIQUIAS

Plugin independente para Paper, dedicado a chefes, fragmentos, relíquias e ao NPC Forjador de Relíquias.

## Implementado nesta etapa
- Projeto Java/Gradle e metadados do plugin.
- Relógio das Relíquias e menus com identidade visual escura e cores por raridade.
- Catálogo de 20 conceitos de relíquias: 5 raras (branco), 5 épicas (roxo), 5 míticas (vermelho) e 5 lendárias (laranja), cada uma com ícone, descrição de poder e cooldown planejado.
- Configuração inicial dos quatro tipos de chefes.
- Surgimento aleatório, tentativas de validar o local e intervalos por raridade.
- Avisos alternados: nome + dimensão e aviso genérico sem localização.
- Drops iniciais de fragmentos.
- Aparência inicial dos chefes com armaduras/armas por raridade e brilho.
- NPC Villager Forjador de Relíquias com roupa dourada e menu visual de troca.

## Ainda em desenvolvimento
Os poderes das relíquias ainda são catálogo/design, não habilidades funcionais. As receitas e trocas de fragmentos, persistência dos cooldowns após reinício, guia interativo de chefes, missões, IA/habilidades especiais dos chefes, balanceamento, proteção contra exploits, testes de compatibilidade e compilação final ainda precisam ser implementados.

## Requisitos planejados
- Paper 26.3
- Java 25
- Geyser/Floodgate para jogadores Bedrock (inventários Bukkit são usados nos menus).

## Build
Com Gradle instalado: `gradle build`. O JAR deve sair em `build/libs/`.

## Comandos administrativos iniciais
- `/reliquias relogio <jogador>`
- `/reliquias boss <raro|epico|mitico|lendario>`
- `/reliquias npc` (cria o Forjador na posição do administrador)
- `/reliquias reload`

Permissão: `reliquias.admin` (OP por padrão).

Os mundos padrão configurados são `world`, `world_nether` e `world_the_end`; ajuste em `config.yml` caso seu servidor use nomes diferentes.
