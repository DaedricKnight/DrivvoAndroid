#!/usr/bin/env bash
# Выгружает сырьё для app/src/main/assets/make_logos.json: пакет Simple Icons, логотипы брендов из Wikidata
# и метаданные их файлов на Wikimedia Commons. Бренды берутся из выгрузки марок (tools/car-makes/fetch_wikidata.sh).
#
#   tools/make-logos/fetch_logos.sh /tmp/car-makes /tmp/make-logos
#   python3 tools/make-logos/build_make_logos.py /tmp/car-makes /tmp/make-logos app/src/main/assets
set -euo pipefail

makes_raw="${1:?каталог выгрузки марок}"
out="${2:?каталог для выгрузки логотипов}"
here="$(cd "$(dirname "$0")" && pwd)"
agent="User-Agent: CarLogDataBuilder/1.0 (https://github.com/DaedricKnight/DrivvoAndroid)"
mkdir -p "$out/wikidata" "$out/commons" "$out/local"

# Версия закреплена: в новой значки могут переименовать или убрать.
simple_icons=16.31.0
curl -sf --retry 5 --max-time 300 -H "$agent" -o "$out/simple-icons.tgz" \
    "https://registry.npmjs.org/simple-icons/-/simple-icons-$simple_icons.tgz"
rm -rf "$out/simple-icons" && mkdir "$out/simple-icons"
tar -xzf "$out/simple-icons.tgz" -C "$out/simple-icons" --strip-components 1
echo "simple-icons $simple_icons"

# Бренды — все, на кого в выгрузке записаны модели; по 400 в запросе.
rm -f "$out"/wikidata/*
python3 - "$makes_raw" "$out/wikidata" <<'EOF'
import json, sys
raw, out = sys.argv[1:]
items = set()
for name in ("models_by_brand", "models_by_maker", "series"):
    with open(f"{raw}/{name}.json", encoding="utf-8") as file:
        for row in json.load(file)["results"]["bindings"]:
            if "brand" in row:
                items.add("wd:" + row["brand"]["value"].rsplit("/", 1)[1])
items = sorted(items)
for start in range(0, len(items), 400):
    with open(f"{out}/items_{start // 400:02d}.txt", "w") as file:
        file.write(" ".join(items[start:start + 400]))
EOF
for batch in "$out"/wikidata/items_*.txt; do
    name="$(basename "$batch" .txt)"
    curl -sf --retry 5 --retry-delay 10 --max-time 300 "https://query.wikidata.org/sparql" \
        --data-urlencode "query=$(sed "s/ITEMS/$(cat "$batch")/" "$here/logos.rq")" \
        -H "Accept: application/sparql-results+json" -H "$agent" \
        -o "$out/wikidata/logos_${name#items_}.json"
done
echo "wikidata: $(ls "$out"/wikidata/logos_*.json | wc -l | tr -d ' ') batches"

# Лицензия, размеры и миниатюра шириной 250 px каждого файла: других ширин, кроме стандартных, Commons не отдаёт.
rm -f "$out"/commons/* "$out"/local/*
# -B: сборщик подключается модулем, и без этого рядом с ним появился бы __pycache__.
python3 -B - "$out" "$here" <<'EOF'
import glob, importlib.util, json, sys, urllib.parse
out, here = sys.argv[1:]
files = set()
for path in glob.glob(f"{out}/wikidata/logos_*.json"):
    with open(path, encoding="utf-8") as file:
        for row in json.load(file)["results"]["bindings"]:
            files.add("File:" + urllib.parse.unquote(row["file"]["value"].rsplit("/", 1)[1]))
# Выбранные вручную файлы бывают и не записаны в Wikidata, а бывают и локальными файлами разделов Википедии.
spec = importlib.util.spec_from_file_location("build_make_logos", f"{here}/build_make_logos.py")
builder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(builder)
local = {}
for name in builder.CHOSEN_FILES.values():
    match = builder.LOCAL_FILE.match(name)
    if match:
        local.setdefault(match.group(1), set()).add("File:" + match.group(2))
    else:
        files.add("File:" + name)
files = sorted(files)
for start in range(0, len(files), 50):
    with open(f"{out}/commons/titles_{start // 50:03d}.txt", "w", encoding="utf-8") as file:
        file.write("|".join(files[start:start + 50]))
for wiki, titles in local.items():
    with open(f"{out}/local/{wiki}.txt", "w", encoding="utf-8") as file:
        file.write("|".join(sorted(titles)))
EOF
for batch in "$out"/commons/titles_*.txt; do
    name="$(basename "$batch" .txt)"
    curl -sf --retry 5 --retry-delay 10 --max-time 120 "https://commons.wikimedia.org/w/api.php" \
        --data-urlencode action=query --data-urlencode format=json --data-urlencode formatversion=2 \
        --data-urlencode prop=imageinfo --data-urlencode "iiprop=url|size|mime|extmetadata" \
        --data-urlencode "iiextmetadatafilter=License|LicenseShortName|Artist" --data-urlencode iiurlwidth=250 \
        --data-urlencode redirects=1 --data-urlencode "titles@$batch" \
        -H "$agent" -o "$out/commons/info_${name#titles_}.json"
done
echo "commons: $(ls "$out"/commons/info_*.json | wc -l | tr -d ' ') batches"

# Локальные файлы разделов — из API своего раздела; NonFree отличает «добросовестное использование».
for batch in "$out"/local/*.txt; do
    [ -e "$batch" ] || continue
    wiki="$(basename "$batch" .txt)"
    curl -sf --retry 5 --retry-delay 10 --max-time 120 "https://$wiki.wikipedia.org/w/api.php" \
        --data-urlencode action=query --data-urlencode format=json --data-urlencode formatversion=2 \
        --data-urlencode prop=imageinfo --data-urlencode "iiprop=url|size|mime|extmetadata" \
        --data-urlencode "iiextmetadatafilter=License|LicenseShortName|Artist|NonFree" --data-urlencode iiurlwidth=250 \
        --data-urlencode "titles@$batch" -H "$agent" -o "$out/local/$wiki.json"
done
echo "local: $(ls "$out"/local/*.json 2>/dev/null | wc -l | tr -d ' ') wikis"
