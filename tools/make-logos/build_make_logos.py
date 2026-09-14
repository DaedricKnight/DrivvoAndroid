#!/usr/bin/env python3
"""Собирает app/src/main/assets/make_logos.json и картинки make_logos/*.webp из выгрузки fetch_logos.sh.

Значок марки — из Simple Icons (CC0): одноцветные контуры, нарисованные под мелкий размер; есть примерно у пятидесяти
марок. Остальным — логотип бренда из Wikidata (P154, P8972, P2910): файл Wikimedia Commons в общественном достоянии
или под CC0, в приложение едет его миниатюра в WebP. JPEG не берутся — почти всегда это фото значка на машине.
Где свободного логотипа у бренда в Wikidata нет, файл Commons подобран вручную (COMMONS_CHOICE), в том числе под CC BY
и CC BY-SA, если лицензия честная: автор и лицензия таких файлов попадают в make_logos.json и в список в приложении.

Из логотипов бренда выбирается действующий, затем специальный значок, затем ближе к квадрату (в круге вытянутая
надпись мельчит), затем основной и более новый. Эвристика ошибается: после пересборки просмотреть картинки глазами,
промахи поправить списками ниже. Нужны curl и cwebp (brew install webp).
"""
import datetime
import hashlib
import html
import importlib.util
import json
import re
import struct
import subprocess
import sys
import unicodedata
import urllib.parse
from collections import Counter, defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
AGENT = "CarLogDataBuilder/1.0 (https://github.com/DaedricKnight/DrivvoAndroid)"
# Сторона квадрата, в который вписывается картинка: в приложении значок не крупнее 40 dp.
IMAGE_BOX = 128
WEBP_OPTIONS = ["-lossless", "-z", "9"]
# Годные файлы Commons: свободные от авторского права, векторные или PNG.
FREE_LICENSES = ("pd", "cc0")
IMAGE_TYPES = ("image/svg+xml", "image/png")
# CC BY и CC BY-SA допустимы только у выбранных вручную: лицензия требует указать автора.
ATTRIBUTION_LICENSE_PREFIX = "cc-by"

# Одноимённые значки Simple Icons других компаний: Eagle — программа, Mega — облако, Proton — почта, Saturn — магазин.
SIMPLE_ICONS_NOT_CARS = {"Eagle", "Mega", "Proton", "Saturn"}
# Марки, у которых логотип с Commons узнаваемее значка Simple Icons.
PREFER_COMMONS = {
    "Bugatti",  # красный овал с решётки, а не монограмма EB
}
# Логотип, выбранный вручную: эвристика берёт не тот, значок Simple Icons не похож на значок машины или у бренда в Wikidata
# свободного логотипа нет, а на Commons он есть. Сведения об этих файлах fetch_logos.sh выгружает сам.
COMMONS_CHOICE = {
    "Audi": "Audi-Logo 2016.svg",  # у Simple Icons кольца фирменного красного, на машинах они чёрно-серебряные
    "BMW": "BMW.svg",  # цветная эмблема с машин; серая плоская 2020 года — для рекламы
    "Daewoo": "Logo wordmark DAEWOO Motors (2002-2016).svg",  # иначе надпись Daewoo Electronics
    "Dodge": "Dodge 1962 logo.svg",  # нынешняя тонкая надпись в круге не читается; эта эмблема снова на Charger 2024
    "Leapmotor": "Leapmotor logo en.svg",  # иначе с иероглифами
    "Saab": "Saab wordmark blue.svg",  # серая надпись на белом круге не читается
    "SsangYong": "Ssangyong company logo.svg",  # иначе KGM — так марку называют только с 2023 года
    "Wuling": "Wuling Motor logo.png",  # иначе логотип совместного предприятия SGMW
    # У брендов ниже в Wikidata свободного логотипа нет, файлы найдены поиском по Commons.
    "Auto Union": "Auto Union Logo 1932.svg",
    "Caterham": "Logo of Caterham Cars.png",
    "Delahaye": "Delahayelogo.png",
    "Jinbei": "Jinbei logo.png",
    "Lifan": "Logo Chongqing Lifan.svg",
    "Ligier": "Logo Ligier.svg",
    "Matra": "Matra sports logo.svg",
    "Nash": "Nash Motor Company logo (text).svg",
    "Noble": "Noble wordmark.png",
    "NSU": "NSU 1926 Logo.svg",
    "Rivian": "Rivian Logo Mark Gold.png",
    "Scion": "Scion logo.png",
    "Sunbeam": "Sunbeam talbot logo.png",  # эмблема Sunbeam-Talbot — так марка называлась в 1938–1954 годах
    # Под CC BY и CC BY-SA: вырезки из фото значков и перерисованные простые логотипы. Файлы, где логотип взят с сайта
    # компании и помечен CC BY без разрешения (Borgward, Hennessey, Zenvo, Baojun, Arcfox), не брать.
    "Abarth": "Abarth Logo.png",
    "Amilcar": "Amilcar.svg",
    "Changan": "Changan icon.svg",
    "Iso Rivolta": "Emblem Iso Rivolta noBG.png",
    "Salmson": "Salmson text only logo.png",
    "Stoewer": "Emblem Stoewer noBG.png",
}
# Автор для списка в приложении, когда поле Artist на Commons — ссылка или описание, а не имя.
AUTHORS = {
    "Emblem Iso Rivolta noBG.png": "Brian Snelson, Auge=mit",
    "Emblem Stoewer noBG.png": "Buch-t",
}
# Марки без логотипа: у бренда в Wikidata записан чужой.
NO_LOGO = {
    "IZh",  # логотип Lada: завод позже собирал Lada
}

SLUG_REPLACEMENTS = {"+": "plus", ".": "dot", "&": "and", "đ": "d", "ħ": "h", "ı": "i", "ĸ": "k", "ŀ": "l", "ł": "l",
                     "ß": "ss", "ŧ": "t", "ø": "o"}

# Марки и их бренды находятся ровно так же, как в сборщике списка марок. Он подключается модулем —
# без запрета байткода рядом с ним появился бы __pycache__.
sys.dont_write_bytecode = True
_spec = importlib.util.spec_from_file_location("build_car_makes", HERE.parent / "car-makes" / "build_car_makes.py")
car_makes = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(car_makes)
key = car_makes.key


def brand_items(makes_raw, makes):
    """Бренды Wikidata каждой марки со счётом записанных за ними моделей."""
    by_key = {key(make): make for make in makes}
    weights = defaultdict(Counter)
    for name in ("models_by_brand", "models_by_maker", "series"):
        for row in car_makes.load(f"{makes_raw}/{name}.json"):
            if "brand" not in row or row["brandLabel"]["value"] in car_makes.GROUPS:
                continue
            make = by_key.get(key(car_makes.normalize_brand(row["brandLabel"]["value"])))
            if make:
                weights[make][row["brand"]["value"].rsplit("/", 1)[1]] += 1
    return weights


def load_statements(raw):
    statements = defaultdict(list)
    for path in sorted(Path(raw, "wikidata").glob("logos_*.json")):
        for row in json.loads(path.read_text(encoding="utf-8"))["results"]["bindings"]:
            statements[row["item"]["value"].rsplit("/", 1)[1]].append({
                "prop": row["prop"]["value"],
                "file": urllib.parse.unquote(row["file"]["value"].rsplit("/", 1)[1]),
                "preferred": row["rank"]["value"].endswith("PreferredRank"),
                "start": row.get("start", {}).get("value", "")[:4],
                "ended": "end" in row,
            })
    return statements


def load_files(raw):
    """Метаданные файлов Commons: имя файла → лицензия, тип, размеры, миниатюра."""
    files, renamed = {}, {}
    for path in sorted(Path(raw, "commons").glob("info_*.json")):
        query = json.loads(path.read_text(encoding="utf-8"))["query"]
        for pair in query.get("normalized", []) + query.get("redirects", []):
            renamed[pair["from"]] = pair["to"]
        for page in query["pages"]:
            if page.get("missing") or not page.get("imageinfo"):
                continue
            info = page["imageinfo"][0]
            # У файла без сведений о лицензии API отдаёт пустой список вместо словаря.
            metadata = info.get("extmetadata") or {}
            files[page["title"]] = {
                "license": metadata.get("License", {}).get("value", ""),
                "license_name": metadata.get("LicenseShortName", {}).get("value", ""),
                "author": " ".join(html.unescape(re.sub(r"<[^>]+>", " ", metadata.get("Artist", {}).get("value", ""))).split()),
                "mime": info["mime"],
                "width": info["width"],
                "height": info["height"],
                "thumb": (info.get("thumburl") or info["url"]).split("?")[0],
            }

    def lookup(name):
        title = "File:" + name
        title = renamed.get(title, title)
        return files.get(renamed.get(title, title))
    return lookup


def candidate_order(candidate):
    width, height = candidate["width"], candidate["height"]
    ratio = max(width / height, height / width) if width and height else 99
    shape = 0 if ratio <= 1.35 else 1 if ratio <= 2.2 else 2 if ratio <= 3.5 else 3
    return (candidate["ended"], not candidate["icon"], shape, not candidate["preferred"], -int(candidate["start"] or 0),
            -candidate["weight"])


def commons_candidates(make, weights, statements, lookup):
    found = {}
    for item, weight in weights[make].most_common():
        for statement in statements.get(item, []):
            info = lookup(statement["file"])
            if not info or info["license"] not in FREE_LICENSES or info["mime"] not in IMAGE_TYPES:
                continue
            candidate = found.setdefault(statement["file"], {
                "file": statement["file"], **info, "ended": True, "icon": False, "preferred": False, "start": "", "weight": 0,
            })
            # Один файл бывает записан у нескольких брендов марки: берём лучшее из его записей.
            candidate["ended"] = candidate["ended"] and statement["ended"]
            candidate["icon"] = candidate["icon"] or statement["prop"] != "logo"
            candidate["preferred"] = candidate["preferred"] or statement["preferred"]
            candidate["start"] = max(candidate["start"], statement["start"])
            candidate["weight"] = max(candidate["weight"], weight)
    return sorted(found.values(), key=candidate_order)


def chosen_file(make, name, lookup):
    info = lookup(name)
    allowed = info and (info["license"] in FREE_LICENSES or info["license"].startswith(ATTRIBUTION_LICENSE_PREFIX))
    if not allowed or info["mime"] not in IMAGE_TYPES:
        sys.exit(f"{make}: «{name}» нет в выгрузке или лицензия не подходит — перезапустить fetch_logos.sh и проверить файл")
    return {"file": name, **info}


def simple_icons(raw):
    """Значки по ключу названия; slug считается так же, как в самом пакете."""
    root = Path(raw, "simple-icons")
    version = json.loads((root / "package.json").read_text())["version"]
    icons = {}
    for icon in json.loads((root / "data" / "simple-icons.json").read_text(encoding="utf-8")):
        # Значок со своей лицензией — не CC0.
        if "license" in icon:
            continue
        title = "".join(SLUG_REPLACEMENTS.get(ch, ch) for ch in icon["title"].lower())
        slug = icon.get("slug") or re.sub(r"[^a-z\d]", "", unicodedata.normalize("NFD", title))
        svg = (root / "icons" / f"{slug}.svg").read_text(encoding="utf-8")
        entry = {"slug": slug, "color": icon["hex"], "path": re.search(r'<path d="([^"]+)"', svg).group(1)}
        for name in [icon["title"], *icon.get("aliases", {}).get("aka", [])]:
            icons.setdefault(key(name), entry)
    return version, icons


def to_webp(thumb_url, cache, target):
    png = cache / (hashlib.md5(thumb_url.encode()).hexdigest() + ".png")
    if not png.exists():
        # Подряд и с повторами: на параллельные запросы миниатюр Wikimedia отвечает 429.
        subprocess.run(["curl", "-sf", "--retry", "5", "--retry-delay", "15", "-A", AGENT, "-o", str(png), thumb_url], check=True)
    header = png.read_bytes()[:24]
    if header[:8] != b"\x89PNG\r\n\x1a\n":
        sys.exit(f"не PNG: {thumb_url}")
    width, height = struct.unpack(">II", header[16:24])
    scale = min(1, IMAGE_BOX / max(width, height))
    size = [str(max(1, round(width * scale))), str(max(1, round(height * scale)))]
    subprocess.run(["cwebp", "-quiet", *WEBP_OPTIONS, "-resize", *size, str(png), "-o", str(target)], check=True)


def main(makes_raw, raw, assets):
    makes = [make["name"] for make in json.loads(Path(assets, "car_makes.json").read_text(encoding="utf-8"))["makes"]]
    weights = brand_items(makes_raw, makes)
    statements = load_statements(raw)
    lookup = load_files(raw)
    version, icons = simple_icons(raw)
    cache = Path(raw, "thumbs")
    cache.mkdir(exist_ok=True)
    images = Path(assets, "make_logos")
    images.mkdir(exist_ok=True)

    logos = []
    for make in makes:
        if make in NO_LOGO:
            continue
        icon = None if make in SIMPLE_ICONS_NOT_CARS else icons.get(key(make))
        picked = None
        if make in COMMONS_CHOICE:
            picked = chosen_file(make, COMMONS_CHOICE[make], lookup)
        elif icon is None or make in PREFER_COMMONS:
            candidates = commons_candidates(make, weights, statements, lookup)
            picked = candidates[0] if candidates else None
        if picked:
            image = f"make_logos/{key(make)}.webp"
            to_webp(picked["thumb"], cache, Path(assets, image))
            logo = {"make": make, "image": image, "file": picked["file"]}
            if picked["license"].startswith(ATTRIBUTION_LICENSE_PREFIX):
                logo.update(license=picked["license_name"], author=AUTHORS.get(picked["file"], picked["author"]))
            logos.append(logo)
        elif icon:
            logos.append({"make": make, "path": icon["path"], "color": icon["color"], "icon": icon["slug"]})

    used = {Path(logo["image"]).name for logo in logos if "image" in logo}
    for stale in images.iterdir():
        if stale.name not in used:
            stale.unlink()

    today = datetime.date.today().isoformat()
    source = f"Simple Icons {version} (CC0) и Wikimedia Commons (общественное достояние, CC0), выгрузка {today}"
    with open(Path(assets, "make_logos.json"), "w", encoding="utf-8") as file:
        # По логотипу на строку: изменения читаются в diff.
        file.write('{"source": ' + json.dumps(source, ensure_ascii=False) + ', "logos": [\n')
        file.write(",\n".join(json.dumps(logo, ensure_ascii=False) for logo in logos))
        file.write("\n]}\n")
    print(f"{len(logos)} logos: {len(logos) - len(used)} icons, {len(used)} images -> {assets}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3])
