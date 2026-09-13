#!/usr/bin/env python3
"""Собирает app/src/main/assets/car_makes.json из выгрузки Wikidata (см. fetch_wikidata.sh).

Wikidata знает почти все модели, но вперемешку с концептами, гоночными машинами, грузовиками, довоенными моделями
и поколениями; бывают и ошибки — товары электроники, записанные моделями автомобилей. Поэтому: марки приводятся
к одному имени и чистятся по списку, марка модели уточняется по её названию, из названия убираются марка
и обозначение поколения, отбрасываются модели с малым числом статей в Википедиях и явный шум по словам.
После пересборки просмотреть diff файла: правила эвристические.
"""
import datetime
import json
import re
import sys
import unicodedata
from collections import Counter, defaultdict

# Сколько статей в Википедиях нужно модели, чтобы попасть в список: отсекает довоенные и совсем редкие машины.
MIN_SITELINKS = 3
# Марка остаётся, если у неё хотя бы столько моделей.
MIN_MODELS_PER_MAKE = 2

BRAND_ALIASES = {
    "AB Volvo": "Volvo",
    "Adam Opel AG": "Opel",
    "Alfa Romeo Automobiles": "Alfa Romeo",
    "Alvis Car and Engineering": "Alvis",
    "American Motors Corporation": "AMC",
    "Aston Martin Lagonda": "Aston Martin",
    "Audi AG": "Audi",
    "Automobiles Citroën": "Citroën",
    "Automobiles Peugeot": "Peugeot",
    "Avatr Technology": "Avatr",
    "Avions Voisin": "Voisin",
    "AvtoVAZ": "Lada",
    "BMW Group": "BMW",
    "BYD Auto": "BYD",
    "Bandini Automobili": "Bandini",
    "Carroll Shelby International": "Shelby",
    "Changan Automobile": "Changan",
    "Chery Automobile": "Chery",
    "Chevrolet Corvette": "Chevrolet",
    "DR Automobiles Groupe": "DR",
    "DS Automobiles": "DS",
    "Dongfeng Motor Corporation": "Dongfeng",
    "Dr. Ing. h.c. F. Porsche AG": "Porsche",
    "Fabryka Samochodów Osobowych": "FSO",
    "Ford Motor Company": "Ford",
    "Fuji Heavy Industries": "Subaru",
    "GAC Group": "GAC",
    "Geely Auto": "Geely",
    "Genesis Motor": "Genesis",
    "Great Wall Motor": "Great Wall",
    "Gumpert Apollo": "Apollo",
    "Hennessey Performance Engineering": "Hennessey",
    "Hotchkiss et Cie": "Hotchkiss",
    "Hyundai Motor Company": "Hyundai",
    "IzhAvto": "IZh",
    "JAC Group": "JAC",
    "JAC Motors": "JAC",
    # Марку переименовали в 2023 году, но на дорогах почти все машины — SsangYong.
    "KG Mobility": "SsangYong",
    "Kia Motors": "Kia",
    "Li Auto": "Li Auto",
    "Lincoln Motor Company": "Lincoln",
    "Lotus Cars": "Lotus",
    "Lynk & Co": "Lynk & Co",
    "MG Motor": "MG",
    "MG marque": "MG",
    "Mahindra & Mahindra": "Mahindra",
    "Mercedes-Benz Group": "Mercedes-Benz",
    "Mitsubishi Motors": "Mitsubishi",
    "Morgan Motor Company": "Morgan",
    "Pagani Automobili": "Pagani",
    "Panoz Auto Development": "Panoz",
    "Panther Westwinds": "Panther",
    "Proton Holdings": "Proton",
    "Ram Trucks": "RAM",
    "Range Rover": "Land Rover",
    "SAIC-GM-Wuling": "Wuling",
    "SEAT S.A.": "SEAT",
    "Saab Automobile": "Saab",
    "Scuderia Cameron Glickenhaus": "Glickenhaus",
    "Seat": "SEAT",
    "Seres Auto (Hubei)": "Seres",
    "SsangYong Motor": "SsangYong",
    "Suzuki Motor Corporation": "Suzuki",
    "Tata Motors": "Tata",
    "Tata Motors Ltd": "Tata",
    "Tesla, Inc.": "Tesla",
    "Toyota Motor Corporation": "Toyota",
    "Ultima Sports": "Ultima",
    "Vauxhall Motors": "Vauxhall",
    "Volvo Cars": "Volvo",
    "Voyah Automobiles Technology": "Voyah",
    "Westfield Sportscars": "Westfield",
    "Zastava Automobiles": "Zastava",
    "Zhiguli": "Lada",
    "Škoda Auto": "Škoda",
}

# Не легковые марки. Первая группа попала сюда ошибками Wikidata: товары записаны моделями автомобилей.
NOT_CAR_MAKES = {
    "Acer", "AMD", "Apple", "Canon", "Casio", "Dell", "DJI", "Fender Musical Instruments", "Foxconn", "Gibson Brands",
    "Hewlett-Packard", "Ibanez", "IBM", "Intel", "Logitech", "Microsoft", "Miele", "Motorola", "Nikon", "Nintendo",
    "Palm", "Panasonic Holdings", "Pentium", "Samsung Electronics", "Sharp", "Sony", "TESLA", "Texas Instruments", "TSMC",
    "Yamaha",
    # Грузовики, автобусы, мотоциклы, спецтехника.
    "Avia", "Bedford Vehicles", "Fabryka Samochodów Ciężarowych", "Federal Signal", "FSC Star", "Hanomag", "Higer", "Hino",
    "International", "International Harvester", "Jawa", "Kamaz", "King Long", "KrAZ", "LIAZ", "Mack Trucks",
    "MAN Truck & Bus", "Minsk Automobile Plant", "Multicar", "Piaggio", "Riga Autobus Factory", "Saviem", "Scania", "Sisu",
    "Steyr-Daimler-Puch", "Switch Mobility", "UD Trucks", "Unimog", "Ural Automotive Plant", "Weichai (Chongqing) Automotive",
    "Wrightbus",
    # Гоночные команды, ателье, давно исчезнувшие объединения.
    "Austro-Daimler", "Autocars", "Benz & Cie.", "Bertone", "Elfin Sports", "Footwork Arrows", "Goliath",
    "Industrieverband Fahrzeugbau", "Italdesign Giugiaro", "Kandi Technologies", "Lion-Peugeot", "March Engineering",
    "Nesselsdorfer Wagenbau-Fabriks-Gesellschaft", "North German Automobile and Engine", "Pininfarina", "Rinspeed",
    "SG Automotive", "Sharp's Commercials", "TechArt", "Zbrojovka Brno",
}

# Концерны и их подразделения: марку модели берём только из её названия.
GROUPS = {
    "British Leyland", "British Motor Corporation", "Chrysler Corporation", "Daimler AG", "FCA US",
    "Fiat Chrysler Automobiles", "General Motors", "Groupe PSA", "Hyundai Motor Group", "Rootes Group",
    "SAIC Motor", "Stellantis", "Stellantis Europe", "Stellantis North America", "Volkswagen Group",
    "Zhejiang Geely Holding Group",
}

# Как ещё пишут марку в начале названия модели.
PREFIX_ALIASES = {
    "Alfa Romeo": ["Alfa"],
    "Citroën": ["Citroen"],
    "Li Auto": ["Li"],
    "Mercedes-Benz": ["Mercedes"],
    "SsangYong": ["KGM"],
    "Volkswagen": ["VW", "SAIC-Volkswagen", "FAW-Volkswagen"],
    "Škoda": ["Skoda"],
}

# Заметный шум популярных марок, который общими правилами не поймать: грузовики, рекордные и опытные машины.
MODEL_DENYLIST = {
    "Ford": {"2GA", "7W", "7Y", "999", "C 100", "De Luxe Ford", "F-250 Super Chief", "Forty-Nine", "GPA", "GT70", "M151 MUTT",
             "M656", "Quadricycle", "T Serie", "Zebra Three"},
    "Mercedes-Benz": {"Accelo", "AMG G 63 6x6", "CLK LM", "F700", "F-Cell", "Harburger Transporter", "LA 911", "Lo", "Necar 5",
                      "Renntransporter", "Sterling Bullet", "T80", "Unimog", "Unimog 435"},
    "Opel": {"8M21", "GM HydroGen4", "Insignia A Sedan", "Insignia B Grand Sport", "KAD", "Patent Motor Car"},
    "Škoda": {"LIAZ", "Sentinel"},
    "Tesla": {"Model", "Powerwall"},
    "Volkswagen": {"Kommandeurswagen", "Plattenwagen", "Schwimmwagen", "Type 14A", "Type 18A", "Type 147 Kleinlieferwagen",
                   "Volksrod", "Worker"},
}

COMPANY_SUFFIX = re.compile(
    r"[,\s]+(AG|S\.?p\.?A\.?|S\.?A\.?|Inc\.?|Ltd\.?|Limited|GmbH|Corporation|Corp\.?|Company|Co\.|Group|"
    r"Motor Company|Motor Corporation|Motors?|Automobiles?|Auto|Cars|Car Company|marque)$"
)

NOISE = re.compile(
    r"\b(concept|prototype|show ?car|study|vision|experimental|research|safety vehicle|race|racing|racer|rally|rallye|"
    r"wrc|r5|rally2|gt1|gt2|gt3|gte|dtm|le mans|lmp\d?|formula|rekordwagen|truck|lorry|tractor|tank|military|attack|"
    r"police|ministerial|ambulance|bus|hearse|replica|kit|engine|platform|chassis|brabus|lotec|carlsson)\b",
    re.IGNORECASE,
)
OLD_NAMING = re.compile(r"\b\d+\s?[/-]\s?\d+\s?(PS|CV|HP|hp|KS)\b|\b\d+\s?(PS|CV|HP|hp|KS)$|\bType [A-Z]{1,3}\b|\d[.,]\d\s?(litre|liter|Liter)\b")
GENERATION_SUFFIXES = [
    re.compile(r"\s*\([^()]*\)$"),
    re.compile(r"\s+(I|II|III|IV|V|VI|VII|VIII|IX|X|XI|XII)$"),
    re.compile(r"\s+(Mk|MK|Mark)\.?\s?[0-9IVX]+$"),
    re.compile(r"\s+\d+(st|nd|rd|th) generation$", re.IGNORECASE),
    re.compile(
        r"\s+(first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|eleventh|twelfth|thirteenth|fourteenth|"
        r"fifteenth) generation$",
        re.IGNORECASE,
    ),
    re.compile(r"\s+(19|20)\d{2}$"),
]
# Код поколения после названия модели, которая и так есть в списке: Passat B8, Corolla E110, Transporter T5, Focus Mk.
GENERATION_CODE = re.compile(r"^(?:[A-Z]{1,3}\d{1,3}[A-Z]?|Mk\.?)$")
# Поколение буквой: Opel Astra G, Vectra B; Peugeot 3008 B.
LETTER_GENERATION = re.compile(r"\s+[A-M]$")
LETTER_GENERATION_MAKES = {"Opel", "Peugeot", "Vauxhall"}
NON_LATIN = re.compile(r"[Ͱ-ϿЀ-ӿ֐-ۿ฀-๿぀-ヿ㐀-鿿가-힯]")


def key(text):
    decomposed = unicodedata.normalize("NFD", text)
    return "".join(ch for ch in decomposed if unicodedata.category(ch) != "Mn" and ch.isalnum()).lower()


def normalize_brand(label):
    name = label
    while True:
        if name in BRAND_ALIASES:
            return BRAND_ALIASES[name]
        shorter = COMPANY_SUFFIX.sub("", name).strip()
        if shorter == name or not shorter:
            return name
        name = shorter


def load(path):
    with open(path, encoding="utf-8") as file:
        return json.load(file)["results"]["bindings"]


def strip_prefix(label, names):
    """Название без марки в начале; None — не начинается ни с одного из имён."""
    for name in sorted(names, key=len, reverse=True):
        if label.lower().startswith(name.lower()):
            rest = label[len(name):]
            if rest == "" or rest[0] in " -":
                return rest.strip(" -")
    return None


def natural(text):
    return [int(part) if part.isdigit() else part for part in re.split(r"(\d+)", key(text) or text.lower())]


def clean_model(make, model, known):
    model = re.sub(r"^\([^()]*\)\s*", "", model)
    if "/" in model:
        # «Duster/Dacia Duster» — одна статья на две марки.
        for brand in known:
            model = re.sub(rf"\s*/\s*{re.escape(brand)}\b.*$", "", model, flags=re.IGNORECASE)
    while True:
        before = model
        for pattern in GENERATION_SUFFIXES:
            model = pattern.sub("", model).strip()
        if make in LETTER_GENERATION_MAKES:
            model = LETTER_GENERATION.sub("", model).strip()
        if model == before:
            return model


def is_noise(make, model):
    return (
        model in MODEL_DENYLIST.get(make, ())
        or " and " in model
        or re.match(r"^(18|19|20)\d{2}\b", model) is not None
        # «full-size Ford», «police vehicles» — описания, а не названия.
        or re.match(r"^[a-z][\w-]*\s", model) is not None
        or model.endswith(" series")
        or model.endswith(" Models")
        or (len(model) == 1 and model.isalpha())
        or (make != "Volkswagen" and re.fullmatch(r"Typ(e)? \d+[A-Z]?", model, re.IGNORECASE) is not None)
        # Двузначные W — болиды Формулы-1 и довоенные гоночные Mercedes; «Benz …» — машины Benz & Cie. до 1926 года.
        or (make == "Mercedes-Benz" and (re.fullmatch(r"W ?\d{2}", model) is not None or model.startswith("Benz ")))
        or not any(ch.isalnum() for ch in model)
        or len(model) > 40
    )


def without_generations(make, names):
    """Убирает поколения модели, которая есть в списке и сама: «Passat B8», «Caddy Typ 2K», «AU Falcon»."""
    keys = set(names)
    result = {}
    for model_key, name in names.items():
        base, _, code = name.rpartition(" ")
        if base and key(base) in keys and (
            GENERATION_CODE.match(code) or (make in LETTER_GENERATION_MAKES and re.fullmatch(r"[A-M]", code))
        ):
            continue
        typ = re.match(r"^(.+)\s+Typ\s+\S+$", name)
        if typ and key(typ.group(1)) in keys:
            continue
        falcon = re.match(r"^[A-Z]{2}\s+(.+)$", name) if make == "Ford" else None
        if falcon and key(falcon.group(1)) in keys:
            continue
        result[model_key] = name
    return result


def main(raw_dir, out_path):
    rows = []
    for name in ("models_by_brand", "models_by_maker", "series"):
        rows.extend(load(f"{raw_dir}/{name}.json"))
    excluded = {row["model"]["value"] for row in load(f"{raw_dir}/excluded.json")}

    items = {}
    for row in rows:
        item_id = row["model"]["value"]
        if item_id in excluded:
            continue
        item = items.setdefault(item_id, {"label": row["modelLabel"]["value"], "links": int(row["links"]["value"]), "brands": set()})
        item["brands"].add(row["brandLabel"]["value"])

    # Признанные марки и их написание: «TESLA» и «Tesla» — разные компании, поэтому сначала чистка, потом слияние.
    counts = Counter()
    for item in items.values():
        for brand in item["brands"]:
            name = normalize_brand(brand)
            if brand not in GROUPS and not re.fullmatch(r"Q\d+", brand) and name not in NOT_CAR_MAKES:
                counts[name] += 1
    spellings = defaultdict(Counter)
    for name, count in counts.items():
        spellings[key(name)][name] += count
    canonical = {}
    for brand_key, names in spellings.items():
        if sum(names.values()) >= 3:
            preferred = [name for name in names if name in BRAND_ALIASES.values()]
            canonical[brand_key] = preferred[0] if preferred else names.most_common(1)[0][0]
    known = set(canonical.values())
    prefixes = {brand: [brand] + PREFIX_ALIASES.get(brand, []) for brand in known}

    makes = defaultdict(dict)
    for item in items.values():
        label = item["label"].strip()
        brands = {normalize_brand(brand) for brand in item["brands"] if brand not in GROUPS}
        if brands and brands <= NOT_CAR_MAKES:
            continue
        if item["links"] < MIN_SITELINKS or NON_LATIN.search(label) or re.fullmatch(r"Q\d+", label):
            continue
        if NOISE.search(label) or OLD_NAMING.search(label):
            continue
        # Марка по названию надёжнее связи: «Dacia Logan III» бывает записана за Renault.
        make, model = None, None
        for brand, names in prefixes.items():
            rest = strip_prefix(label, names)
            if rest is not None and (make is None or len(brand) > len(make)):
                make, model = brand, rest
        if make is None:
            candidates = {canonical[key(name)] for name in brands if key(name) in canonical}
            if len(candidates) != 1:
                continue
            make, model = candidates.pop(), label
        model = clean_model(make, model, known)
        if model and not is_noise(make, model):
            makes[make].setdefault(key(model), model)

    result = []
    for make in sorted(makes, key=natural):
        models = sorted(without_generations(make, makes[make]).values(), key=natural)
        if len(models) >= MIN_MODELS_PER_MAKE:
            result.append({"name": make, "models": models})

    source = f"Wikidata (CC0), выгрузка {datetime.date.today().isoformat()}"
    with open(out_path, "w", encoding="utf-8") as file:
        # По марке на строку: изменения списка читаются в diff.
        file.write('{"source": ' + json.dumps(source, ensure_ascii=False) + ', "makes": [\n')
        file.write(",\n".join(json.dumps(make, ensure_ascii=False) for make in result))
        file.write("\n]}\n")
    print(f"{len(result)} makes, {sum(len(m['models']) for m in result)} models -> {out_path}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
