package io.github.magnycopper.datatools.buss.ascp.common;

import lombok.Getter;

@Getter
public enum AppleStoreEnums {

    R532("R532", "杭州万象城", "杭州"),
    R471("R471", "西湖", "杭州"),
    R531("R531", "天一广场", "宁波"),
    R476("R476", "重庆北城天街", "重庆"),
    R480("R480", "解放碑", "重庆"),
    R573("R573", "重庆万象城", "重庆"),
    R448("R448", "王府井", "北京"),
    R645("R645", "朝阳大悦城", "北京"),
    R479("R479", "华贸购物中心", "北京"),
    R320("R320", "三里屯", "北京"),
    R388("R388", "西单大悦城", "北京"),
    R571("R571", "南宁万象城", "南宁"),
    R534("R534", "中街大悦城", "沈阳"),
    R478("R478", "百年城", "大连"),
    R576("R576", "沈阳万象城", "沈阳"),
    R609("R609", "大连恒隆广场", "大连"),
    R688("R688", "苏州", "苏州"),
    R643("R643", "虹悦城", "南京"),
    R493("R493", "南京艾尚天地", "南京"),
    R703("R703", "玄武湖", "南京"),
    R574("R574", "无锡恒隆广场", "无锡"),
    R572("R572", "郑州万象城", "郑州"),
    R644("R644", "厦门新生活广场", "厦门"),
    R646("R646", "泰禾广场", "福州"),
    R617("R617", "长沙", "长沙"),
    R575("R575", "武汉", "武汉"),
    R670("R670", "昆明", "昆明"),
    R648("R648", "济南恒隆广场", "济南"),
    R557("R557", "青岛万象城", "青岛"),
    R502("R502", "成都万象城", "成都"),
    R580("R580", "成都太古里", "成都"),
    R705("R705", "七宝", "上海"),
    R359("R359", "南京东路", "上海"),
    R683("R683", "环球港", "上海"),
    R581("R581", "五角场", "上海"),
    R389("R389", "浦东", "上海"),
    R390("R390", "香港广场", "上海"),
    R401("R401", "上海环贸 iapm ", "上海"),
    R577("R577", "天环广场 ", "广州"),
    R639("R639", "珠江新城", "广州"),
    R484("R484", "深圳益田假日广场", "深圳"),
    R637("R637", "天津大悦城", "天津"),
    R638("R638", "天津万象城", "天津"),
    R579("R579", "天津恒隆广场", "天津");

    private final String id;
    private final String name;
    private final String city;

    AppleStoreEnums(String id, String name, String city) {
        this.id = id;
        this.name = name;
        this.city = city;
    }
}
