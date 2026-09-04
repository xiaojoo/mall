#!/usr/bin/env bash
# ============================================================
# 重建 ES product 索引（修复价格排序）
#
# 背景：
#   旧索引 skuPrice 为 keyword 类型，排序按字典序（如 1000 < 200 < 99），
#   导致列表页「价格排序」看起来不生效。正确映射应为数值类型 long。
#   分类过滤/库存过滤正常（catalogId/hasStock 字段没变），仅 skuPrice 类型错误。
#
# 用法：
#   bash create-product-index.sh            # 默认 localhost:9200
#   bash create-product-index.sh <es-host>  # 指定 ES 地址，如 localhost:9200
#
# ⚠️ 注意：
#   1. 会删除旧索引数据，执行后需在后台「商品管理 → 上架」重新上架商品
#      （或先建 product_v2 用 _reindex 迁移，再删旧索引换名）
#   2. 与 resources/product-mapping.txt 内容一致（skuPrice=long）
# ============================================================

set -e
ES_HOST="${1:-localhost:9200}"
BASE="http://${ES_HOST}"

echo "==> 目标 ES: ${BASE}"

# 1. 删除旧索引
echo "==> 删除旧索引 product"
curl -s -X DELETE "${BASE}/product"
echo

# 2. 用正确 mapping 重建（skuPrice = long，可数值排序）
echo "==> 重建索引 product（skuPrice=long）"
curl -s -X PUT "${BASE}/product" -H 'Content-Type: application/json' -d '{
  "mappings": {
    "properties": {
      "skuId": {
        "type": "long"
      },
      "spuId": {
        "type": "keyword"
      },
      "skuTitle": {
        "type": "text",
        "analyzer": "ik_smart"
      },
      "skuPrice": {
        "type": "long"
      },
      "skuImg": {
        "type": "keyword",
        "index": false,
        "doc_values": false
      },
      "saleCount": {
        "type": "long"
      },
      "hasStock": {
        "type": "boolean"
      },
      "hotScore": {
        "type": "long"
      },
      "brandId": {
        "type": "long"
      },
      "catalogId": {
        "type": "long"
      },
      "brandName": {
        "type": "keyword",
        "index": false,
        "doc_values": false
      },
      "brandImg": {
        "type": "keyword"
      },
      "catalogName": {
        "type": "keyword",
        "index": false,
        "doc_values": false
      },
      "attrs": {
        "type": "nested",
        "properties": {
          "attrId": {
            "type": "long"
          },
          "attrName": {
            "type": "keyword",
            "index": false,
            "doc_values": false
          },
          "attrValue": {
            "type": "keyword"
          }
        }
      }
    }
  }
}'
echo

# 3. 验证映射
echo "==> 验证 skuPrice 类型"
curl -s "${BASE}/product/_mapping?pretty" | grep -A 2 '"skuPrice"'

echo
echo "==> 完成。请到后台重新上架商品（重建后索引为空）。"
