import React, { Component } from "react";
import ProductService from "../services/product.service";
import AuthService from "../services/auth.service";

export default class ProductList extends Component {
  constructor(props) {
    super(props);
    this.retrieveProducts = this.retrieveProducts.bind(this);
    this.handleBuyNow = this.handleBuyNow.bind(this);

    this.state = {
      products: [],
      message: ""
    };
  }

  componentDidMount() {
    this.retrieveProducts();
  }

  retrieveProducts() {
    ProductService.getAllProducts()
      .then(response => {
        this.setState({
          products: response.data.data || []
        });
      })
      .catch(e => {
        console.log(e);
        this.setState({
          message: "Failed to load products"
        });
      });
  }

  handleBuyNow(product) {
    const user = AuthService.getCurrentUser();
    if (!user) {
      window.location.href = '/login';
      return;
    }

    // Store selected product for checkout
    localStorage.setItem('selectedProduct', JSON.stringify({
      productId: product.id,
      name: product.name,
      price: product.price,
      maxStock: product.stock
    }));
    window.location.href = '/checkout';
  }

  render() {
    const { products, message } = this.state;

    return (
      <div>
        <h4>Products</h4>
        {message && (
          <div className="alert alert-danger" role="alert">
            {message}
          </div>
        )}
        <div className="row">
          {products && products.map((product, index) => (
            <div className="col-md-4 mb-3" key={index}>
              <div className="card product-card">
                <div className="card-body">
                  <h5 className="card-title">{product.name}</h5>
                  <p className="card-text">{product.description}</p>
                  <p className="card-text">
                    <strong>Price: ${product.price}</strong>
                  </p>
                  <p className="card-text">
                    <small>Stock: {product.stock}</small>
                  </p>
                </div>
                <div className="card-footer">
                  <button
                    className="btn btn-primary btn-sm w-100"
                    onClick={() => this.handleBuyNow(product)}
                    disabled={product.stock === 0}
                  >
                    {product.stock === 0 ? 'Out of Stock' : 'Buy Now'}
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }
}
