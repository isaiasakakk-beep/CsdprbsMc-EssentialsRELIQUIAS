package br.com.csdpbrs.reliquias;

import org.bukkit.Material;
import java.util.List;

public final class RelicCatalog {
    public enum Rarity {
        RARO("&f", "Raras", Material.WHITE_STAINED_GLASS_PANE, "raro"),
        EPICO("&5", "Épicas", Material.PURPLE_STAINED_GLASS_PANE, "epico"),
        MITICO("&c", "Míticas", Material.RED_STAINED_GLASS_PANE, "mitico"),
        LENDARIO("&6", "Lendárias", Material.ORANGE_STAINED_GLASS_PANE, "lendario");
        public final String color, display, key;
        public final Material pane;
        Rarity(String color, String display, Material pane, String key) {
            this.color=color; this.display=display; this.pane=pane; this.key=key;
        }
    }

    public record Relic(String id, String name, Rarity rarity, Material icon,
                        String category, String power, String cooldown,
                        List<String> lore) {}

    private static final List<Relic> ALL = List.of(
        new Relic("fazendeiro","Relíquia do Fazendeiro",Rarity.RARO,Material.LEATHER_LEGGINGS,"Utilidade","Concede Herói da Vila enquanto estiver equipada nas pernas.","Passivo",List.of("&7A vila reconhece seu portador.")),
        new Relic("minerador","Relíquia do Minerador",Rarity.RARO,Material.DIAMOND_PICKAXE,"Utilidade","Concede pressa de mineração enquanto estiver na mão.","Passivo",List.of("&7Acelera o trabalho nas minas.")),
        new Relic("invisibilidade","Relíquia da Invisibilidade",Rarity.RARO,Material.LEATHER_BOOTS,"Ocultação","Concede invisibilidade enquanto estiver equipada nos pés.","Passivo",List.of("&7Oculta o portador de olhares curiosos.")),
        new Relic("nether","Relíquia do Nether",Rarity.RARO,Material.GOLDEN_HELMET,"Sobrevivência","Concede resistência ao fogo enquanto estiver equipada na cabeça.","Passivo",List.of("&7Guarda o calor do Nether.")),
        new Relic("sorte","Relíquia da Sorte",Rarity.RARO,Material.RABBIT_FOOT,"Utilidade","Concede Sorte temporária ao ativar.","45 segundos",List.of("&7Um pequeno toque de fortuna.")),
        new Relic("explorador","Relíquia do Explorador",Rarity.EPICO,Material.COMPASS,"Exploração","Concede visão noturna e respiração aquática ao ativar.","75 segundos",List.of("&7Ajuda a revelar caminhos escondidos.")),
        new Relic("explorador_nether","Relíquia do Explorador do Nether",Rarity.EPICO,Material.MAGMA_CREAM,"Exploração","Concede resistência ao fogo e visão noturna ao ativar.","90 segundos",List.of("&7Feita para atravessar terras vulcânicas.")),
        new Relic("explorador_2","Relíquia do Explorador 2.0",Rarity.EPICO,Material.SPYGLASS,"Exploração","Concede visão noturna, respiração aquática e velocidade ao ativar.","100 segundos",List.of("&7Uma versão aprimorada para longas jornadas.")),
        new Relic("espiao","Relíquia do Espião",Rarity.EPICO,Material.FERMENTED_SPIDER_EYE,"Ocultação","Concede invisibilidade breve ao ativar.","80 segundos",List.of("&7Permite passar despercebido por instantes.")),
        new Relic("palhaco","Relíquia do Palhaço",Rarity.EPICO,Material.CARVED_PUMPKIN,"Visual","Solta partículas festivas e concede salto aprimorado.","60 segundos",List.of("&7Leva um pouco de diversão por onde passa.")),
        new Relic("deus_vento","Relíquia do Deus do Vento",Rarity.MITICO,Material.WIND_CHARGE,"Mobilidade","Impulsiona o portador para a direção em que olha.","120 segundos",List.of("&7Uma corrente de ar acompanha o portador.")),
        new Relic("imortalidade","Relíquia da Imortalidade",Rarity.MITICO,Material.TOTEM_OF_UNDYING,"Sobrevivência","Concede absorção e regeneração temporárias; não concede invulnerabilidade total.","180 segundos",List.of("&7Uma proteção extraordinária, com limites.")),
        new Relic("defesa","Relíquia da Defesa",Rarity.MITICO,Material.SHIELD,"Sobrevivência","Concede resistência temporária ao ativar.","150 segundos",List.of("&7Uma barreira de proteção ancestral.")),
        new Relic("resistencia","Relíquia da Resistência",Rarity.MITICO,Material.IRON_CHESTPLATE,"Sobrevivência","Concede resistência e absorção por tempo limitado.","180 segundos",List.of("&7Fortalece a proteção do portador.")),
        new Relic("dragao","Relíquia do Dragão",Rarity.MITICO,Material.DRAGON_BREATH,"Mobilidade","Concede resistência ao fogo e queda lenta ao ativar.","180 segundos",List.of("&7Uma energia antiga ajuda a atravessar grandes alturas.")),
        new Relic("warden","Relíquia do Warden",Rarity.LENDARIO,Material.SCULK_CATALYST,"Exploração","Emite um pulso de eco visual e revela criaturas próximas por alguns segundos.","240 segundos",List.of("&7O sculk responde aos movimentos ao redor.")),
        new Relic("stomper","Relíquia do Stomper",Rarity.LENDARIO,Material.HEAVY_CORE,"Mobilidade","Concede salto aprimorado e queda lenta por tempo limitado.","210 segundos",List.of("&7Permite saltos altos e aterrissagens seguras.")),
        new Relic("bizonho","Relíquia do Bizonho",Rarity.LENDARIO,Material.SLIME_BLOCK,"Mobilidade","Concede resistência a empurrões e salto aprimorado.","220 segundos",List.of("&7Seu portador mantém o equilíbrio.")),
        new Relic("bizonho_2","Relíquia do Bizonho 2.0",Rarity.LENDARIO,Material.HONEY_BLOCK,"Mobilidade","Concede resistência a empurrões e queda lenta.","260 segundos",List.of("&7Uma versão aprimorada para travessias difíceis.")),
        new Relic("deus","Relíquia do Deus",Rarity.LENDARIO,Material.NETHER_STAR,"Sobrevivência","Concede visão noturna, resistência ao fogo e absorção temporárias.","300 segundos",List.of("&7Reúne bênçãos de sobrevivência em uma só relíquia."))
    );

    private RelicCatalog() {}
    public static List<Relic> all() { return ALL; }
    public static List<Relic> byRarity(Rarity rarity) { return ALL.stream().filter(r -> r.rarity()==rarity).toList(); }
}
