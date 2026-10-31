package cn.blockforge.generated.generatedmod.yujian;

/** Cosmetic identifiers are stable on disk and on the wire. */
public enum HuahongSkin {
    METEOR_PRISM("虹核流星", "白色亮核、虹色伴流与细碎余芒"),
    METEOR_FLAME("燃尾流星", "尖长燃核、沿路径弯曲的火舌与脱落短芒"),
    NEEDLE("贯空长虹", "凝聚金白尖核与单束连续长虹"),
    FEATHER("开放流羽", "错落展开的蓝紫光膜与分叉羽丝"),
    SMOKE("红黑离烟", "暗烟包裹、内部赤红裂光与离体烟团"),
    FLAME("赤焰撕裂", "宽厚赤焰、开放撕裂火舌与残焰"),
    FAN("扇形流霞", "错位后展的暖金薄纱与长散丝"),
    DENSE("蓝白虹流", "三维蓝白主流、起伏截面与高亮虹筋");
    public static final String KEY="wuxingxiantu:huahong_skin";
    public final String label,description;
    HuahongSkin(String label,String description){this.label=label;this.description=description;}
    public static HuahongSkin fromId(int id){return id>=0&&id<values().length?values()[id]:METEOR_PRISM;}
}
