"""고객센터 기능을 위한 OpenAI function-calling 도구.

봇이 필요할 때만 main-server 에서 사용자 데이터를 조회한다(유저 JWT 그대로 전달).
상품 추천/찜/주문 결과에는 상품 id 를 포함해, 봇이 `/products/{id}` 링크를 만들 수 있게 한다.
"""
import json

_MAX_ORDERS = 10
_MAX_ITEMS = 20

TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "get_order_history",
            "description": "로그인한 사용자의 주문 내역(상태·금액·주문 상품)을 조회한다. "
                           "주문/배송/구매 이력 관련 질문에 사용한다.",
            "parameters": {"type": "object", "properties": {}, "additionalProperties": False},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_wishlist",
            "description": "로그인한 사용자가 찜한 상품 목록을 조회한다. 찜/위시리스트 관련 질문에 사용한다.",
            "parameters": {"type": "object", "properties": {}, "additionalProperties": False},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "recommend_products",
            "description": "사용자 맞춤 추천 상품 목록을 조회한다. 상품 추천 요청 시 사용한다. "
                           "결과의 상품 id 로 /products/{id} 링크를 만들어 안내한다.",
            "parameters": {"type": "object", "properties": {}, "additionalProperties": False},
        },
    },
]


def _trim_orders(orders: list) -> list:
    trimmed = []
    for order in orders[:_MAX_ORDERS]:
        trimmed.append({
            "id": order.get("id"),
            "status": order.get("status"),
            "totalAmount": order.get("totalAmount"),
            "items": [
                {
                    "productId": item.get("productId"),
                    "productName": item.get("productName"),
                    "quantity": item.get("quantity"),
                }
                for item in (order.get("items") or [])[:_MAX_ITEMS]
            ],
        })
    return trimmed


def _trim_wishlist(items: list) -> list:
    return [
        {
            "productId": item.get("productId"),
            "productName": item.get("productName"),
            "productPrice": item.get("productPrice"),
        }
        for item in items[:_MAX_ITEMS]
    ]


def _trim_recommendations(recs: list) -> list:
    trimmed = []
    for rec in recs[:_MAX_ITEMS]:
        product = rec.get("product", rec)
        trimmed.append({
            "id": product.get("id"),
            "name": product.get("name"),
            "price": product.get("price"),
            "imageUrl": product.get("imageUrl"),
        })
    return trimmed


async def execute_tool(name: str, client, user_id: str) -> str:
    """도구 이름을 main-server 조회로 매핑해 요약 JSON 문자열을 반환한다."""
    if name == "get_order_history":
        orders = await client.get_orders()
        return json.dumps({"orders": _trim_orders(orders)}, ensure_ascii=False)
    if name == "get_wishlist":
        items = await client.get_wishlist(user_id)
        return json.dumps({"wishlist": _trim_wishlist(items)}, ensure_ascii=False)
    if name == "recommend_products":
        recs = await client.get_recommendations()
        return json.dumps({"recommendations": _trim_recommendations(recs)}, ensure_ascii=False)
    return json.dumps({"error": f"알 수 없는 도구: {name}"}, ensure_ascii=False)
