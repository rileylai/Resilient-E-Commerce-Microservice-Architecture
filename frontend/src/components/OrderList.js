import React, { Component } from "react";
import OrderService from "../services/order.service";
import AuthService from "../services/auth.service";
import { Link } from "react-router-dom";

export default class OrderList extends Component {
  constructor(props) {
    super(props);
    this.retrieveOrders = this.retrieveOrders.bind(this);

    this.state = {
      orders: [],
      loading: true,
      message: ""
    };
  }

  componentDidMount() {
    this.retrieveOrders();
  }

  retrieveOrders() {
    const user = AuthService.getCurrentUser();
    if (!user) {
      window.location.href = '/login';
      return;
    }

    OrderService.getUserOrders(user.id)
      .then(response => {
        this.setState({
          orders: response.data.data || [],
          loading: false
        });
      })
      .catch(error => {
        const resMessage =
          (error.response &&
            error.response.data &&
            error.response.data.message) ||
          error.message ||
          error.toString();

        this.setState({
          loading: false,
          message: resMessage
        });
      });
  }

  getStatusClass(status) {
    const statusMap = {
      'PENDING_VALIDATION': 'pending',
      'PENDING_PAYMENT': 'pending',
      'PAYMENT_SUCCESSFUL': 'payment-successful',
      'DELIVERY_REQUESTED': 'delivery-requested',
      'PICKED_UP': 'in-transit',
      'IN_TRANSIT': 'in-transit',
      'DELIVERED': 'delivered',
      'CANCELLED': 'cancelled',
      'FAILED': 'failed',
      'LOST': 'failed'
    };
    return statusMap[status] || 'pending';
  }

  render() {
    const { orders, loading, message } = this.state;

    if (loading) {
      return (
        <div className="text-center">
          <div className="spinner-border" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      );
    }

    return (
      <div>
        <h3>My Orders</h3>

        {message && (
          <div className="alert alert-danger" role="alert">
            {message}
          </div>
        )}

        {orders.length === 0 ? (
          <div className="alert alert-info">
            <p>You don't have any orders yet.</p>
            <a href="/products" className="btn btn-primary">
              Start Shopping
            </a>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table table-striped">
              <thead>
                <tr>
                  <th>Order ID</th>
                  <th>Status</th>
                  <th>Total Amount</th>
                  <th>Created</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order, index) => (
                  <tr key={index}>
                    <td>#{order.orderId}</td>
                    <td>
                      <span className={`order-status ${this.getStatusClass(order.status)}`}>
                        {order.status}
                      </span>
                    </td>
                    <td>${order.totalAmount}</td>
                    <td>{new Date(order.createTime).toLocaleString()}</td>
                    <td>
                      <Link to={`/order/${order.orderId}`} className="btn btn-sm btn-primary">
                        View Details
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    );
  }
}
