import http from "../http-common";

class ProductService {
  getAllProducts() {
    return http.get("/product/all");
  }
}

export default new ProductService();
