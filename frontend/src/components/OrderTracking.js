import React, { Component } from "react";
import { useParams } from "react-router-dom";
import OrderService from "../services/order.service";
import AuthService from "../services/auth.service";

class OrderTrackingBase extends Component {
  constructor(props) {
    super(props);
    this.getOrder = this.getOrder.bind(this);
    this.cancelOrder = this.cancelOrder.bind(this);
    this.startPolling = this.startPolling.bind(this);
    this.stopPolling = this.stopPolling.bind(this);

    this.state = {
      order: null,
      loading: true,
      message: "",
      pollingInterval: null
    };
  }

  componentDidMount() {
    this.getOrder();
    this.startPolling();
  }

  componentWillUnmount() {
    this.stopPolling();
  }

  startPolling() {
    // Poll every 3 seconds for order status updates
    const interval = setInterval(() => {
      this.getOrder(true);
    }, 3000);

    this.setState({ pollingInterval: interval });
  }

  stopPolling() {
    if (this.state.pollingInterval) {
      clearInterval(this.state.pollingInterval);
      this.setState({ pollingInterval: null });
    }
  }

  getOrder(silent = false) {
    const { orderId } = this.props.params;

    if (!silent) {
      this.setState({ loading: true });
    }

    OrderService.getOrder(orderId)
      .then(response => {
        const data = response.data.data || response.data;
        this.setState({
          order: data,
          loading: false,
          message: ""
        });

        // Stop polling if order is in terminal state
        const terminalStates = ['DELIVERED', 'CANCELLED', 'FAILED', 'LOST'];
        if (terminalStates.includes(data.status)) {
          this.stopPolling();
        }
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

  cancelOrder() {
    const user = AuthService.getCurrentUser();
    if (!user) {
      window.location.href = '/login';
      return;
    }

    if (!window.confirm('Are you sure you want to cancel this order?')) {
      return;
    }

    const { orderId } = this.props.params;

    OrderService.cancelOrder(orderId, user.id)
      .then(response => {
        this.setState({
          message: "Order cancelled successfully!"
        });
        this.getOrder();
      })
      .catch(error => {
        const resMessage =
          (error.response &&
            error.response.data &&
            error.response.data.message) ||
          error.message ||
          error.toString();

        this.setState({
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

  getStatusTimeline() {
    const { order } = this.state;
    if (!order) return [];

    const allStatuses = [
      'PENDING_VALIDATION',
      'PENDING_PAYMENT',
      'PAYMENT_SUCCESSFUL',
      'DELIVERY_REQUESTED',
      'PICKED_UP',
      'IN_TRANSIT',
      'DELIVERED'
    ];

    const currentIndex = allStatuses.indexOf(order.status);

    return allStatuses.map((status, index) => ({
      status,
      completed: index <= currentIndex,
      current: status === order.status
    }));
  }

  render() {
    const { order, loading, message } = this.state;
    const canCancel = order && !['DELIVERED', 'CANCELLED', 'FAILED', 'LOST', 'DELIVERY_REQUESTED', 'PICKED_UP', 'IN_TRANSIT'].includes(order.status);

    if (loading && !order) {
      return (
        <div className="text-center">
          <div className="spinner-border" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      );
    }

    return (
      <div className="container">
        <h3>Order Tracking</h3>

        {message && (
          <div className="alert alert-info" role="alert">
            {message}
          </div>
        )}

        {order && (
          <div className="card">
            <div className="card-header">
              <h5>Order #{order.orderId}</h5>
            </div>
            <div className="card-body">
              <div className="row mb-3">
                <div className="col-md-6">
                  <p><strong>Status:</strong> <span className={`order-status ${this.getStatusClass(order.status)}`}>{order.status}</span></p>
                  <p><strong>Total Amount:</strong> ${order.totalAmount}</p>
                  <p><strong>Created:</strong> {new Date(order.createTime).toLocaleString()}</p>
                </div>
                <div className="col-md-6">
                  {order.reservationId && <p><strong>Reservation ID:</strong> {order.reservationId}</p>}
                  {order.transactionId && <p><strong>Transaction ID:</strong> {order.transactionId}</p>}
                </div>
              </div>

              <h6>Order Items:</h6>
              <table className="table table-sm">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Quantity</th>
                    <th>Price</th>
                    <th>Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  {order.items && order.items.map((item, index) => (
                    <tr key={index}>
                      <td>{item.productName}</td>
                      <td>{item.quantity}</td>
                      <td>${item.price}</td>
                      <td>${item.subTotal}</td>
                    </tr>
                  ))}
                </tbody>
              </table>

              <div className="order-timeline mt-4">
                <h6>Order Progress:</h6>
                {this.getStatusTimeline().map((item, index) => (
                  <div
                    key={index}
                    className="timeline-item"
                    style={{
                      borderLeftColor: item.completed ? '#28a745' : '#ccc',
                      opacity: item.completed ? 1 : 0.5
                    }}
                  >
                    <strong style={{ color: item.current ? '#007bff' : '#000' }}>
                      {item.status}
                      {item.current && ' (Current)'}
                    </strong>
                  </div>
                ))}
              </div>

              <div className="mt-4">
                {canCancel && (
                  <button className="btn btn-danger" onClick={this.cancelOrder}>
                    Cancel Order
                  </button>
                )}
                <a href="/orders" className="btn btn-secondary ms-2">
                  Back to Orders
                </a>
                <a href="/products" className="btn btn-primary ms-2">
                  Continue Shopping
                </a>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }
}

// Wrapper to use with React Router v6 hooks
export default function OrderTracking() {
  const params = useParams();
  return <OrderTrackingBase params={params} />;
}
