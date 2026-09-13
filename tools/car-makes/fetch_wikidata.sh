#!/usr/bin/env bash
# Выгружает из Wikidata модели автомобилей с марками — сырьё для app/src/main/assets/car_makes.json.
# Данные Wikidata распространяются под CC0.
#
#   tools/car-makes/fetch_wikidata.sh /tmp/car-makes
#   python3 tools/car-makes/build_car_makes.py /tmp/car-makes app/src/main/assets/car_makes.json
set -euo pipefail

out="${1:?каталог для выгрузки}"
here="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$out"
agent="User-Agent: CarLogDataBuilder/1.0 (https://github.com/DaedricKnight/DrivvoAndroid)"

for query in models_by_brand models_by_maker series excluded; do
    # Сервис запросов Wikidata под нагрузкой отвечает 502 — curl повторяет такие ответы сам.
    curl -sfG --retry 5 --retry-delay 10 --max-time 300 "https://query.wikidata.org/sparql" \
        --data-urlencode "query=$(cat "$here/$query.rq")" \
        -H "Accept: application/sparql-results+json" \
        -H "$agent" \
        -o "$out/$query.json"
    echo "$query: $(wc -c < "$out/$query.json") bytes"
done
