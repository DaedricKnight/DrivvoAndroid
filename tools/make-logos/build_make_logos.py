#!/usr/bin/env python3
"""Собирает app/src/main/assets/make_logos.json и картинки make_logos/*.webp из выгрузки fetch_logos.sh.

Значок марки — из Simple Icons (CC0): одноцветные контуры, нарисованные под мелкий размер; есть примерно у пятидесяти
марок. Остальным — логотип бренда из Wikidata (P154, P8972, P2910): файл Wikimedia Commons в общественном достоянии
или под CC0, в приложение едет его миниатюра в WebP. JPEG не берутся — почти всегда это фото значка на машине.
Где свободного логотипа у бренда в Wikidata нет, файл подобран вручную (CHOSEN_FILES): с Commons или свободный локальный
файл раздела Википедии, в том числе под CC BY и CC BY-SA, если лицензия честная. Автор и лицензия таких файлов попадают
в make_logos.json и в список в приложении.

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
# Выбранные вручную бывают и JPEG или GIF: сканы старых логотипов и фото значков.
CHOSEN_IMAGE_TYPES = IMAGE_TYPES + ("image/jpeg", "image/gif")
# CC BY и CC BY-SA допустимы только у выбранных вручную: лицензия требует указать автора.
ATTRIBUTION_LICENSE_PREFIX = "cc-by"
# Локальный файл раздела Википедии в ручном списке: «en:Имя файла».
LOCAL_FILE = re.compile(r"^([a-z-]+):(.+)$")
# В разделах Википедии общественное достояние бывает записано только словами.
PUBLIC_DOMAIN = re.compile(r"public domain|общественное достояние", re.IGNORECASE)

# Одноимённые значки Simple Icons других компаний: Eagle — программа, Mega — облако, Proton — почта, Saturn — магазин.
SIMPLE_ICONS_NOT_CARS = {"Eagle", "Mega", "Proton", "Saturn"}
# Марки, у которых логотип с Commons узнаваемее значка Simple Icons.
PREFER_COMMONS = {
    "Bugatti",  # красный овал с решётки, а не монограмма EB
}
# Логотип, выбранный вручную: эвристика берёт не тот, значок Simple Icons не похож на значок машины или у бренда в Wikidata
# свободного логотипа нет, а на Commons или в разделе Википедии он есть. Сведения об этих файлах fetch_logos.sh выгружает сам.
CHOSEN_FILES = {
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
    # Локальные файлы разделов Википедии в общественном достоянии — нашлись в статьях о бренде.
    "Aero": "en:Aero Logo H-H-Linz.png",
    "Allard": "en:Allard Motor Company Logo.svg",
    "Fangchengbao": "en:Fangchengbao logo.png",
    "GAZ": "ru:GAZ-group-logo-2015.svg",  # логотип «Группы ГАЗ»
    "Genesis": "en:Genesis division emblem.svg",
    "Hongqi": "en:Hongqi logo.svg",
    "Lotus": "en:Lotus Cars logo.svg",
    "Luxeed": "ar:Luxeed.svg",
    "Talbot-Lago": "Talbot brand logo 1954.png",
    "Weltmeister": "en:Weltmeister Logo.png",
    # Логотипы брендов из Wikidata в JPEG и GIF: сканы старых логотипов и фото значков, у фото — обрезка в CROPS.
    "Alfa Romeo": "ALFA ROMEO badge on a car (cropped).jpg",
    "Alpina": "Alpina logo (4069775475).jpg",
    "Apollo": "Apollo logo - Flick - Concorso Italiano 2005.jpg",
    "Austin": "Logo Automobile Austin.jpg",
    "Bandini": "Bandini automobili.JPG",
    "Berkeley": "Emblem Berkeley.JPG",
    "Bestune": "Bestune logo.jpg",
    "Bristol": "Bristol-Signature-2019.jpg",
    "Cord": "CordCrestTalla.jpg",
    "Duesenberg": "Emblem Duesenberg.JPG",
    "Eagle": "Eagle vision (cropped).jpg",
    "Excalibur": "1984 Excalibur Phaeton Hubcap.jpg",
    "Fisker": "Fisker, IAA Mobility 2023, Munich (P1110255).jpg",
    "Glas": "Goggomobil Dart pic14.JPG",  # значок Goggomobil — главной машины Glas
    "Hotchkiss": "Hotchkiss logo.jpg",
    "Hudson": "Hudson-Motoring Magazine-1913-028.jpg",
    "Jowett": "Jowett Short-chassis tourer 1926.JPG",
    "Lagonda": "Aston Martin Lagonda Taraf 2016.jpg",
    "Monteverdi": "Emblem MBM.JPG",
    "Morgan": "Morgan badge - Flickr - exfordy.jpg",
    "Morris": "Morris motor logo.jpg",
    "Panhard": "Pl logo5.gif",
    "Prince": "Prince Motor Company Marque.jpg",
    "Reliant": "Reliant Motors badge.jpg",
    "Rover": "Rover badge 1965.jpg",
    "Shelby": "Shelby American logo.jpg",
    "Standard": "Standard veteran car (4915905345).jpg",
    "Triumph": "Triumph STandard Emblem.jpg",
    "Wartburg": "Wartburg Automobil Logo (Alter Fritz).jpg",
    "WiLL": "WiLL-Markenlogo.jpg",
    "Wolseley": "Wolseley sign.jpg",
    "Zastava": "Zastava Automobiles logo.jpg",
    # Фото значков и вывесок с Commons не из Wikidata: обрезка в CROPS.
    "Arcfox": "ARCFOX αS front face.jpg",
    "Armstrong Siddeley": "1936 Armstrong-Siddeley Atalanta bonnet mascot 49108522413 (cropped).jpg",  # сфинкс с капота
    "Bizzarrini": "Emblem Bizzarrini Schriftzug.JPG",
    "Borgward": "Borgward Logo auf der IAA 2017.jpg",
    "Facel Vega": "1963 Facel Vega Facellia 3.jpg",
    "Hispano-Suiza": "Hispano suiza logo.jpg",
    "Iran Khodro": "IKCO Nishapur Dealership (4).JPG",  # конь с вывески дилера
    "IZh": "ИЖ логотип.jpg",
    "Jensen": "Jensen Logo (46959002074).jpg",
    "Kaiser": "KAISER DARRIN Convertible logo.jpg",
    "Landwind": "Zotye T800 004.jpg",  # вывеска Landwind на соседнем стенде автосалона
    "Mega": "Emblem Mega.JPG",
    "Pagani": "Geneva MotorShow 2013 - Pagani Huayra Pagani sign.jpg",
    "Proton": "Proton showroom in SS15, Subang Jaya.jpg",  # вывеска салона
    "Roewe": "2022 SAIC Roewe RX5 eMAX (front).jpg",
    "Saleen": "Saleen Mustang at the 2014 New York International Auto Show (13938795056).jpg",  # надпись над стендом
    "Vector": "Vector W8 car badge.jpg",
    "Venturi": "Emblem Venturi.JPG",
    "Voisin": "1924 Avions Voisin C4 logo, four cylinder without valve 8CV Coach body, 4 seats, at the Musée Automobile de Vendée.JPG",
    "Volga": "Volga M21 badge sign.JPG",  # олень с капота ГАЗ-21 — символ марки
    "Voyah": "Voyah Dream Logo, Auto 2024, Zurich (PANA0847-2).jpg",
    # Под CC BY и CC BY-SA: вырезки из фото значков и перерисованные простые логотипы. Файлы, где логотип взят с сайта
    # компании и помечен CC BY без разрешения (Borgward, Hennessey, Zenvo, Baojun, Arcfox), не брать.
    "Abarth": "Abarth Logo.png",
    "Amilcar": "Amilcar.svg",
    "Changan": "Changan icon.svg",
    "Ginetta": "Logo ginetta.png",
    "Iso Rivolta": "Emblem Iso Rivolta noBG.png",
    "Salmson": "Salmson text only logo.png",
    "Stoewer": "Emblem Stoewer noBG.png",
}
# Автор для списка в приложении, когда поле Artist на Commons — ссылка или описание, а не имя.
AUTHORS = {
    "1936 Armstrong-Siddeley Atalanta bonnet mascot 49108522413 (cropped).jpg": "Andrew Bone",
    "Apollo logo - Flick - Concorso Italiano 2005.jpg": "Craig Howell",
    "Bandini automobili.JPG": "Ilario Bandini",
    "Eagle vision (cropped).jpg": "W. P. McMeans",
    "Emblem Iso Rivolta noBG.png": "Brian Snelson, Auge=mit",
    "Emblem Stoewer noBG.png": "Buch-t",
    "Logo ginetta.png": "Thomas's Pics",
    "Morgan badge - Flickr - exfordy.jpg": "Brian Snelson",
    "Standard veteran car (4915905345).jpg": "Peter Turvey",
}
# Фото не с Wikimedia (Flickr, найдены через Openverse): адрес картинки, страница, лицензия и автор записаны здесь,
# выгружать о них нечего. В приложении строка автора ведёт на страницу фото.
EXTERNAL_FILES = {
    "Marussia": {"url": "https://live.staticflickr.com/8304/7787903070_9379c50e43_b.jpg",
                 "page": "https://www.flickr.com/photos/22974618@N00/7787903070", "license": "CC BY-SA 2.0", "author": "Sergey Galyonkin"},
    "Panther": {"url": "https://live.staticflickr.com/3389/3671285281_94d4d78409_b.jpg",
                "page": "https://www.flickr.com/photos/32659528@N00/3671285281", "license": "CC BY 2.0", "author": "Brian Snelson"},
    "UAZ": {"url": "https://live.staticflickr.com/1677/25085065193_55dcf39dd7_b.jpg",
            "page": "https://www.flickr.com/photos/133136615@N03/25085065193", "license": "CC0 1.0", "author": "nicifor28"},
}
# Обрезка фото значка: доли картинки — слева, сверху, ширина, высота.
CROPS = {
    "Alfa Romeo": (0.12, 0.13, 0.76, 0.76),
    "Alpina": (0.185, 0.06, 0.61, 0.92),
    "Apollo": (0.27, 0.17, 0.46, 0.62),
    "Arcfox": (0.483, 0.402, 0.072, 0.095),
    "Armstrong Siddeley": (0.27, 0.32, 0.48, 0.42),
    "Berkeley": (0.21, 0.09, 0.66, 0.85),
    "Bestune": (0.07, 0.23, 0.22, 0.52),
    "Bizzarrini": (0.01, 0.06, 0.98, 0.88),
    "Borgward": (0.06, 0.04, 0.89, 0.92),
    "Cord": (0.17, 0.05, 0.71, 0.84),
    "Eagle": (0.13, 0.08, 0.66, 0.84),
    "Excalibur": (0.02, 0.03, 0.96, 0.94),
    "Facel Vega": (0.03, 0.27, 0.89, 0.57),
    "Fisker": (0.40, 0.21, 0.20, 0.27),
    "Ginetta": (0.26, 0.21, 0.455, 0.58),
    "Glas": (0.06, 0.10, 0.90, 0.86),
    "Hispano-Suiza": (0.04, 0.08, 0.92, 0.85),
    "Hotchkiss": (0.06, 0.16, 0.88, 0.80),
    "Iran Khodro": (0.38, 0.06, 0.27, 0.32),
    "IZh": (0.30, 0.17, 0.38, 0.67),
    "Jensen": (0.03, 0.38, 0.91, 0.29),
    "Kaiser": (0.31, 0.23, 0.41, 0.49),
    "Lagonda": (0.10, 0.34, 0.62, 0.48),
    "Landwind": (0.598, 0.132, 0.132, 0.062),
    "Marussia": (0.46, 0.715, 0.38, 0.115),
    "Mega": (0.205, 0.14, 0.555, 0.705),
    "Morgan": (0.06, 0.33, 0.88, 0.35),
    "Morris": (0.18, 0.29, 0.57, 0.57),
    "Pagani": (0.30, 0.41, 0.46, 0.19),
    "Panther": (0.36, 0.14, 0.35, 0.115),
    "Proton": (0.175, 0.27, 0.28, 0.105),
    "Reliant": (0.24, 0.18, 0.54, 0.66),
    "Roewe": (0.160, 0.502, 0.048, 0.104),
    "Saleen": (0.33, 0.02, 0.50, 0.10),
    "Standard": (0.27, 0.16, 0.46, 0.62),
    "Vector": (0.05, 0.40, 0.89, 0.21),
    "Venturi": (0.06, 0.18, 0.83, 0.46),
    "Voisin": (0.11, 0.24, 0.79, 0.46),
    "Volga": (0.35, 0.18, 0.49, 0.44),
    "Voyah": (0.39, 0.21, 0.24, 0.37),
}
# Круглые значки на фото: картинка заливает весь круг аватара, а не вписывается в белый.
FILL = {"Alfa Romeo", "Alpina", "Apollo", "Berkeley", "Excalibur", "Fisker", "Ginetta", "Hotchkiss", "IZh", "Mega", "Standard"}
# Марка без своего логотипа, машины которой носят значок другой марки из списка.
SAME_LOGO = {
    "Dongfeng Liuzhou": "Dongfeng",  # Forthing и Chenglong из Люйчжоу выходят с эмблемой Dongfeng
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


def file_info(info):
    # У файла без сведений о лицензии API отдаёт пустой список вместо словаря.
    metadata = info.get("extmetadata") or {}
    license = metadata.get("License", {}).get("value", "")
    license_name = metadata.get("LicenseShortName", {}).get("value", "")
    return {
        "license": "pd" if not license and PUBLIC_DOMAIN.search(license_name) else license,
        "license_name": license_name,
        "nonfree": bool(metadata.get("NonFree", {}).get("value")),
        "author": " ".join(html.unescape(re.sub(r"<[^>]+>", " ", metadata.get("Artist", {}).get("value", ""))).split()),
        "mime": info["mime"],
        "width": info["width"],
        "height": info["height"],
        "url": info["url"],
        "thumb": (info.get("thumburl") or info["url"]).split("?")[0],
    }


def load_files(raw):
    """Метаданные файлов: имя файла Commons или «язык:имя» локального файла раздела → лицензия, тип, размеры, миниатюра."""
    files, renamed = {}, {}
    for path in sorted(Path(raw, "commons").glob("info_*.json")):
        query = json.loads(path.read_text(encoding="utf-8"))["query"]
        for pair in query.get("normalized", []) + query.get("redirects", []):
            renamed[pair["from"]] = pair["to"]
        for page in query["pages"]:
            if not page.get("missing") and page.get("imageinfo"):
                files[page["title"]] = file_info(page["imageinfo"][0])
    for path in sorted(Path(raw, "local").glob("*.json")):
        for page in json.loads(path.read_text(encoding="utf-8"))["query"]["pages"]:
            if not page.get("missing") and page.get("imageinfo"):
                info = file_info(page["imageinfo"][0])
                # Раздел может показать и несвободный файл «добросовестного использования» — такие не нужны.
                if not info["nonfree"]:
                    files[f"{path.stem}:{page['title'].split(':', 1)[1]}"] = info

    def lookup(name):
        if LOCAL_FILE.match(name):
            return files.get(name)
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
    if not allowed or info["mime"] not in CHOSEN_IMAGE_TYPES:
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


def image_size(data):
    """Ширина и высота PNG, GIF или JPEG по заголовку; None — другой формат."""
    if data[:8] == b"\x89PNG\r\n\x1a\n":
        return struct.unpack(">II", data[16:24])
    if data[:6] in (b"GIF87a", b"GIF89a"):
        return struct.unpack("<HH", data[6:10])
    if data[:2] == b"\xff\xd8":
        offset = 2
        while offset + 9 < len(data):
            marker = data[offset + 1]
            # SOF0–SOF15, кроме DHT, JPG и DAC: в нём размеры кадра.
            if 0xC0 <= marker <= 0xCF and marker not in (0xC4, 0xC8, 0xCC):
                height, width = struct.unpack(">HH", data[offset + 5:offset + 9])
                return width, height
            offset += 2 + struct.unpack(">H", data[offset + 2:offset + 4])[0]
    return None


def to_webp(thumb_url, cache, target, crop=None):
    source = cache / (hashlib.md5(thumb_url.encode()).hexdigest() + Path(thumb_url).suffix.lower())
    if not source.exists():
        # Подряд и с повторами: на параллельные запросы миниатюр Wikimedia отвечает 429.
        # -L: хост миниатюр отвечает на адреса оригиналов редиректом.
        subprocess.run(["curl", "-sfL", "--retry", "5", "--retry-delay", "15", "-A", AGENT, "-o", str(source), thumb_url], check=True)
    data = source.read_bytes()
    size = image_size(data)
    if not size:
        sys.exit(f"не PNG, GIF или JPEG: {thumb_url}")
    if data[:3] == b"GIF":
        # GIF cwebp не читает; миниатюра и так мала, а обрезать сканы логотипов не нужно.
        if crop:
            sys.exit(f"обрезка GIF не поддерживается: {thumb_url}")
        subprocess.run(["gif2webp", "-quiet", str(source), "-o", str(target)], check=True)
        return
    width, height = size
    # Фото значка без потерь весило бы в разы больше, а разницы на 40 dp не видно.
    options = list(WEBP_OPTIONS) if data[:4] == b"\x89PNG" else ["-q", "85"]
    if crop:
        left, top, crop_width, crop_height = (round(part * side) for part, side in zip(crop, (width, height, width, height)))
        options += ["-crop", str(left), str(top), str(crop_width), str(crop_height)]
        width, height = crop_width, crop_height
    scale = min(1, IMAGE_BOX / max(width, height))
    options += ["-resize", str(max(1, round(width * scale))), str(max(1, round(height * scale)))]
    subprocess.run(["cwebp", "-quiet", *options, str(source), "-o", str(target)], check=True)


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
        icon = None if make in SIMPLE_ICONS_NOT_CARS else icons.get(key(make))
        picked = None
        if make in CHOSEN_FILES:
            picked = chosen_file(make, CHOSEN_FILES[make], lookup)
        elif make in EXTERNAL_FILES:
            photo = EXTERNAL_FILES[make]
            credit = photo["license"].startswith("CC BY")
            picked = {"file": photo["page"], "url": photo["url"], "thumb": photo["url"], "width": 0, "license_name": photo["license"],
                      "license": ATTRIBUTION_LICENSE_PREFIX if credit else "pd", "author": photo["author"]}
        elif icon is None or make in PREFER_COMMONS:
            candidates = commons_candidates(make, weights, statements, lookup)
            picked = candidates[0] if candidates else None
        if picked:
            image = f"make_logos/{key(make)}.webp"
            source, crop = picked["thumb"], CROPS.get(make)
            if crop and picked["width"] > 250:
                # Значок бывает малой долей кадра: картинка для обрезки нужна такая, чтобы он вышел хотя бы в 200 px.
                enough = 200 / crop[2] <= 1280
                source = picked["thumb"].replace("/250px-", "/1280px-") if picked["width"] > 1280 and enough else picked["url"]
            to_webp(source, cache, Path(assets, image), crop)
            logo = {"make": make, "image": image, "file": picked["file"]}
            if make in FILL:
                logo["fill"] = True
            if picked["license"].startswith(ATTRIBUTION_LICENSE_PREFIX):
                logo.update(license=picked["license_name"], author=AUTHORS.get(picked["file"], picked["author"]))
            logos.append(logo)
        elif icon:
            logos.append({"make": make, "path": icon["path"], "color": icon["color"], "icon": icon["slug"]})

    for make, other in SAME_LOGO.items():
        same = next((logo for logo in logos if logo["make"] == other), None)
        if same and all(logo["make"] != make for logo in logos):
            logos.append({**same, "make": make})
    order = {make: index for index, make in enumerate(makes)}
    logos.sort(key=lambda logo: order[logo["make"]])

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
