"""고객센터 도구: 주문내역·찜·추천을 main-server 에서 조회해 요약(상품 id 포함→링크)."""
import json

from app.services.tools import TOOLS, execute_tool


class FakeMainClient:
    def __init__(self):
        self.orders_called = False
        self.wishlist_user = None
        self.recs_called = False

    async def get_orders(self):
        self.orders_called = True
        return [{
            "id": 1, "status": "PAID", "totalAmount": 9800,
            "items": [{"productId": 7, "productName": "다꾸 키링", "price": 4900,
                       "quantity": 2, "lineTotal": 9800}],
        }]

    async def get_wishlist(self, user_id):
        self.wishlist_user = user_id
        return [{"id": 3, "productId": 8, "productName": "스티커",
                 "productImageUrl": None, "productPrice": 2500}]

    async def get_recommendations(self):
        self.recs_called = True
        return [{"product": {"id": 9, "name": "그립톡", "price": 5900, "imageUrl": "/img/9.jpg"},
                 "score": 0.9}]


def test_tools_expose_three_customer_service_functions():
    names = {t["function"]["name"] for t in TOOLS}
    assert names == {"get_order_history", "get_wishlist", "recommend_products"}


async def test_execute_get_order_history_returns_items_with_product_ids():
    client = FakeMainClient()
    data = json.loads(await execute_tool("get_order_history", client, "1"))
    assert client.orders_called
    assert data["orders"][0]["status"] == "PAID"
    assert data["orders"][0]["items"][0]["productId"] == 7


async def test_execute_get_wishlist_passes_user_id():
    client = FakeMainClient()
    data = json.loads(await execute_tool("get_wishlist", client, "42"))
    assert client.wishlist_user == "42"
    assert data["wishlist"][0]["productId"] == 8


async def test_execute_recommend_products_extracts_product_for_links():
    client = FakeMainClient()
    data = json.loads(await execute_tool("recommend_products", client, "1"))
    assert client.recs_called
    assert data["recommendations"][0]["id"] == 9
    assert data["recommendations"][0]["name"] == "그립톡"


async def test_execute_recommend_products_includes_image_url_for_cards():
    client = FakeMainClient()
    data = json.loads(await execute_tool("recommend_products", client, "1"))
    assert data["recommendations"][0]["imageUrl"] == "/img/9.jpg"


async def test_execute_unknown_tool_returns_error():
    data = json.loads(await execute_tool("nope", FakeMainClient(), "1"))
    assert "error" in data
