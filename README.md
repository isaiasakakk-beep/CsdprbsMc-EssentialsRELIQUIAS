# CSDPBRS ESSENTIALS RELIQUIAS

Plugin independente para Paper 26.3, com chefes, fragmentos, relíquias colecionáveis, missões e o NPC Forjador.

## Sistemas implementados

- Relógio das Relíquias entregue na primeira entrada do jogador.
- Menus Bukkit por inventário, compatíveis com Java e Bedrock via Geyser/Floodgate.
- Coleção persistente de 20 relíquias: 5 raras, 5 épicas, 5 míticas e 5 lendárias.
- Quatro raridades com identidade visual: branco, roxo, vermelho e laranja.
- Poderes ativados com clique direito e cooldown individual.
- Quatro chefes com aparência e atributos configuráveis.
- Barra de vida dos chefes, habilidades especiais por raridade e surgimento em posições seguras próximas de jogadores online, em qualquer dimensão/mundo carregado.
- Avisos globais para todos os jogadores quando qualquer chefe surge; somente o chefe raro revela mundo e coordenadas exatas.
- Fragmentos próprios de cada raridade como recompensa dos chefes.
- NPC Forjador com trocas por fragmentos e entrega de relíquias ainda não conquistadas.
- Missões persistentes com recompensas de fragmentos:
  - Caçador de Criaturas: elimine 50 criaturas hostis para receber 8 fragmentos raros.
  - Minerador das Profundezas: minere 96 minérios para receber 8 fragmentos épicos.
  - Caçador de Chefes: derrote 3 chefes de relíquias para receber 8 fragmentos míticos.
  - Soberano das Relíquias: derrote 10 chefes de relíquias para receber 8 fragmentos lendários.
- Menus protegidos contra movimentação de itens.

## Requisitos

- Paper 26.3
- Java 25
- Geyser/Floodgate para jogadores Bedrock (os menus usam inventários Bukkit).

## Configuração

Edite `plugins/CSDPBRS-ESSENTIALS-RELIQUIAS/config.yml` para ajustar intervalos, raio de surgimento, vida, dano, entidades, quantidade de fragmentos e custos do Forjador. O chefe raro tem intervalo máximo de 10 minutos; os demais mantêm seus intervalos configurados. Os chefes surgem no mundo/dimensão de um jogador online, a uma distância configurável, em uma posição segura.

## Comandos administrativos

- `/reliquias relogio <jogador>` — entrega outro Relógio.
- `/reliquias give <jogador> <id>` — entrega uma relíquia e registra na coleção.
- `/reliquias boss <raro|epico|mitico|lendario>` — tenta invocar um chefe.
- `/reliquias npc` — cria o Forjador na posição do administrador.
- `/reliquias reload` — recarrega a configuração.

Permissão: `reliquias.admin` (OP por padrão).

## Compilação

Com Gradle e Java 25: `gradle build`. O arquivo JAR é gerado em `build/libs/`. O GitHub Actions também compila o projeto e publica o JAR como artefato de cada execução bem-sucedida.
