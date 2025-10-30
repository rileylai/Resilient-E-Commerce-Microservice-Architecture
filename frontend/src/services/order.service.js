import http from "../http-common";

class OrderService {
  placeOrder(userId, items) {
    return http.post("/order/place", {
      userId,
      items
    });
  }

  getOrder(orderId) {
    return http.get(`/order/${orderId}`);
  }

  getUserOrders(userId) {
    return http.get(`/order/user/${userId}`);
  }

  cancelOrder(orderId, userId) {
    return http.post(`/order/cancel/${orderId}?userId=${userId}`);
  }
}

export default new OrderService();
