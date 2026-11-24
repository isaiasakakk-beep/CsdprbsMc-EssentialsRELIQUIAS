# CSDPBRS ESSENTIALS RELIQUIAS

Plugin Paper 26.3 com coleção de relíquias, Forjador, chefes persistentes e fragmentos.

## Mega atualização

- O catálogo antigo foi substituído por 20 relíquias organizadas em quatro raridades.
- Categorias: Utilidade, Exploração, Mobilidade, Sobrevivência, Ocultação e Visual.
- Poderes passivos: Fazendeiro, Minerador, Invisibilidade e Nether.
- Poderes ativáveis: Sorte, Explorador, Explorador do Nether, Explorador 2.0, Espião, Palhaço, Deus do Vento, Imortalidade, Defesa, Resistência, Dragão, Warden, Stomper, Bizonho, Bizonho 2.0 e Deus.
- A Relíquia do Warden usa um pulso de eco para revelar criaturas próximas; não causa dano.
- O Forjador possui animação de roleta no inventário. A recompensa é garantida: a animação termina na próxima relíquia ainda não conquistada.
- O Relógio das Relíquias é restaurado após a morte.
- Os menus de coleção e guia mostram categoria, poder e raridade.

## Chefes e fragmentos

- A população é mantida entre 5 e 8 chefes por raridade nas áreas elegíveis com jogadores explorando.
- Raro: biomas Cherry Grove e Plains.
- Épico: Dripstone Caves, Lush Caves e Deep Dark.
- Mítico: qualquer bioma do Nether.
- Lendário: dimensão do End.
- Os chefes receberam redução de vida e dano para equilibrar os grupos.
- Todos os chefes garantem fragmentos raros. Existe chance pequena de receber também fragmentos de raridade superior.
- O aviso do primeiro surgimento de cada grupo é global; o Guardião Raro revela coordenadas e entrega a bússola de rastreamento.

## Requisitos

- Paper 26.3
- Java 25
- Geyser/Floodgate para jogadores Bedrock. Os menus usam inventários Bukkit.

## Comandos administrativos

- /reliquias relogio <jogador> — entrega o Relógio.
- /reliquias give <jogador> <id> — entrega uma relíquia e registra na coleção.
- /reliquias boss <raro|epico|mitico|lendario> — tenta invocar um chefe.
- /reliquias npc — cria o Forjador na posição do administrador.
- /reliquias reload — recarrega a configuração.

Permissão: reliquias.admin (OP por padrão).

## Compilação

Com Gradle e Java 25: gradle build. O JAR é gerado em build/libs/. O GitHub Actions compila o projeto e publica o JAR quando o build termina com sucesso.
