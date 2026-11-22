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
        Rarity(String color, String display, Material pane, String key) { this.color=color; this.display=display; this.pane=pane; this.key=key; }
    }
    public record Relic(String id, String name, Rarity rarity, Material icon, String power, String cooldown, List<String> lore) {}
    private static final List<Relic> ALL = List.of(
        new Relic("pena_vento","Pena do Vento",Rarity.RARO,Material.FEATHER,"Salto ampliado por alguns segundos.","35 segundos",List.of("&7Uma pena leve que desafia a gravidade.")),
        new Relic("coracao_vital","Coração Vital",Rarity.RARO,Material.HEART_OF_THE_SEA,"Recupera uma pequena quantidade de vida.","45 segundos",List.of("&7Um pulso de energia restauradora.")),
        new Relic("olho_noturno","Olho Noturno",Rarity.RARO,Material.SPIDER_EYE,"Concede visão noturna temporária.","60 segundos",List.of("&7Enxerga até nos lugares mais escuros.")),
        new Relic("botas_coelho","Botas do Coelho",Rarity.RARO,Material.RABBIT_FOOT,"Aumenta a velocidade por pouco tempo.","50 segundos",List.of("&7Leves e surpreendentemente ágeis.")),
        new Relic("cristal_aquatico","Cristal Aquático",Rarity.RARO,Material.PRISMARINE_CRYSTALS,"Ajuda a respirar debaixo d'água.","60 segundos",List.of("&7Guarda o fôlego das profundezas.")),
        new Relic("orbe_vazio","Orbe do Vazio",Rarity.EPICO,Material.ENDER_PEARL,"Dispara um impulso curto de teleporte.","75 segundos",List.of("&7Uma esfera que dobra pequenas distâncias.")),
        new Relic("coracao_glacial","Coração Glacial",Rarity.EPICO,Material.PACKED_ICE,"Reduz dano recebido por um curto período.","100 segundos",List.of("&7Frio intenso protege seu portador.")),
        new Relic("nucleo_trovao","Núcleo do Trovão",Rarity.EPICO,Material.LIGHTNING_ROD,"Libera uma descarga visual que afasta criaturas próximas.","90 segundos",List.of("&7Energia elétrica contida em um núcleo.")),
        new Relic("manto_sombras","Manto das Sombras",Rarity.EPICO,Material.PHANTOM_MEMBRANE,"Concede invisibilidade breve, sem esconder armadura.","120 segundos",List.of("&7As sombras ocultam seus movimentos.")),
        new Relic("ampulheta_tempo","Ampulheta do Tempo",Rarity.EPICO,Material.CLOCK,"Concede lentidão breve a criaturas próximas.","110 segundos",List.of("&7A areia parece cair ao contrário.")),
        new Relic("rubi_sangrento","Rubi Sangrento",Rarity.MITICO,Material.REDSTONE_BLOCK,"Fortalece ataques por um curto período.","150 segundos",List.of("&7Um rubi que pulsa com energia de batalha.")),
        new Relic("coroa_fenix","Coroa da Fênix",Rarity.MITICO,Material.BLAZE_POWDER,"Concede resistência ao fogo e pequena cura.","180 segundos",List.of("&7Renascimento em chamas controladas.")),
        new Relic("garra_abismo","Garra do Abismo",Rarity.MITICO,Material.IRON_INGOT,"Puxa uma criatura próxima em direção ao portador.","160 segundos",List.of("&7Uma garra forjada nas profundezas.")),
        new Relic("coracao_dragao","Coração do Dragão",Rarity.MITICO,Material.DRAGON_BREATH,"Concede resistência e força moderadas.","200 segundos",List.of("&7Ainda guarda o calor de um dragão.")),
        new Relic("portal_espectral","Portal Espectral",Rarity.MITICO,Material.END_CRYSTAL,"Teleporte curto para um ponto seguro à frente.","180 segundos",List.of("&7Uma passagem instável entre planos.")),
        new Relic("coracao_rei","Coração do Rei",Rarity.LENDARIO,Material.NETHER_STAR,"Concede proteção e regeneração por tempo limitado.","300 segundos",List.of("&7Relíquia de autoridade ancestral.")),
        new Relic("espada_apocalipse","Espada do Apocalipse",Rarity.LENDARIO,Material.NETHERITE_SWORD,"Ataque especial em área com dano limitado.","240 segundos",List.of("&7Uma lâmina lendária, poderosa mas controlada.")),
        new Relic("olho_supremo","Olho Supremo",Rarity.LENDARIO,Material.ENDER_EYE,"Revela criaturas próximas por alguns segundos.","220 segundos",List.of("&7Nada escapa ao olhar desta relíquia.")),
        new Relic("relogio_destino","Relógio do Destino",Rarity.LENDARIO,Material.CLOCK,"Remove efeitos negativos comuns do portador.","260 segundos",List.of("&7Um instante pode mudar tudo.")),
        new Relic("coracao_infinito","Coração do Infinito",Rarity.LENDARIO,Material.AMETHYST_SHARD,"Concede um escudo temporário com limite de absorção.","360 segundos",List.of("&7Uma fonte rara de energia sem fim aparente."))
    );
    private RelicCatalog() {}
    public static List<Relic> all() { return ALL; }
    public static List<Relic> byRarity(Rarity rarity) { return ALL.stream().filter(r -> r.rarity()==rarity).toList(); }
}
