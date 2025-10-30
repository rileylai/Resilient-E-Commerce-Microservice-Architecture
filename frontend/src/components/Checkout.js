import React, { Component } from "react";
import OrderService from "../services/order.service";
import AuthService from "../services/auth.service";

export default class Checkout extends Component {
  constructor(props) {
    super(props);
    this.placeOrder = this.placeOrder.bind(this);
    this.proceedWithOrder = this.proceedWithOrder.bind(this);
    this.updateQuantity = this.updateQuantity.bind(this);
    this.cancelOrder = this.cancelOrder.bind(this);
    this.cancelOrderWithId = this.cancelOrderWithId.bind(this);
    this.pollForOrder = this.pollForOrder.bind(this);

    const productStr = localStorage.getItem('selectedProduct');
    const product = productStr ? JSON.parse(productStr) : null;

    this.state = {
      product: product,
      quantity: 1,
      loading: false,
      message: "",
      orderId: null,
      success: false,
      showModal: false,
      orderStatus: null,
      statusHistory: [],
      orderCancelled: false,
      pollingInterval: null,
      previousOrderCount: 0
    };
  }

  componentDidMount() {
    if (!this.state.product) {
      window.location.href = '/products';
    }
  }

  componentWillUnmount() {
    // Clear polling interval when component unmounts
    if (this.state.pollingInterval) {
      clearInterval(this.state.pollingInterval);
    }
  }

  updateQuantity(e) {
    const value = parseInt(e.target.value);
    if (value > 0 && value <= this.state.product.maxStock) {
      this.setState({ quantity: value });
    }
  }

  pollForOrder() {
    const user = AuthService.getCurrentUser();
    if (!user) return;

    console.log("[Polling] Checking for order updates...");

    // If we already have orderId, poll for status updates
    if (this.state.orderId) {
      OrderService.getOrder(this.state.orderId)
        .then(response => {
          const order = response.data.data || response.data;
          console.log("[Polling] Order status:", order.status);

          // Update status if changed
          if (order.status !== this.state.orderStatus) {
            console.log("[Polling] Status changed from", this.state.orderStatus, "to", order.status);

            // Add to status history
            const newStatusHistory = [
              ...this.state.statusHistory,
              { status: order.status, time: new Date().toLocaleTimeString() }
            ];

            this.setState({
              orderStatus: order.status,
              statusHistory: newStatusHistory
            });
          }

          // Stop polling on terminal states
          const terminalStates = ['DELIVERY_REQUESTED', 'DELIVERED', 'CANCELLED', 'FAILED', 'LOST'];
          if (terminalStates.includes(order.status)) {
            console.log("[Polling] Terminal state reached, stopping polling");
            if (this.state.pollingInterval) {
              clearInterval(this.state.pollingInterval);
              this.setState({ pollingInterval: null });
            }

            // Redirect on success states
            if (order.status === 'DELIVERY_REQUESTED') {
              setTimeout(() => {
                window.location.href = `/order/${this.state.orderId}`;
              }, 3000);
            }
          }
        })
        .catch(error => {
          console.error("[Polling] Error getting order status:", error);
        });
    } else {
      // Poll user's orders to find the newly created order
      OrderService.getUserOrders(user.id)
        .then(response => {
          const orders = response.data.data || [];
          console.log("[Polling] Found orders:", orders.length, "Previous count:", this.state.previousOrderCount);

          if (orders.length > this.state.previousOrderCount) {
            // New order detected! Get the most recent one
            const latestOrder = orders[0];
            console.log("[Polling] New order detected! Order ID:", latestOrder.orderId, "Status:", latestOrder.status);

            // Update state with real order data
            const newStatusHistory = [
              { status: latestOrder.status, time: new Date().toLocaleTimeString() }
            ];

            this.setState({
              orderId: latestOrder.orderId,
              orderStatus: latestOrder.status,
              statusHistory: newStatusHistory,
              previousOrderCount: orders.length
            });

            // If user already clicked cancel, cancel the order now
            if (this.state.orderCancelled) {
              console.log("[Polling] User already requested cancellation, cancelling order now...");
              this.cancelOrderWithId(latestOrder.orderId);
            }

            // Continue polling for status updates
            console.log("[Polling] Continuing to poll for status updates...");
          } else {
            console.log("[Polling] No new orders yet");
          }
        })
        .catch(error => {
          console.error("[Polling] Error polling for order:", error);
        });
    }
  }

  cancelOrder() {
    const { orderId } = this.state;
    console.log("[Cancel] Cancel button clicked. Current orderId:", orderId);

    if (!orderId) {
      // Order not created yet, mark as cancelled and wait for polling to get orderId
      console.log("[Cancel] No orderId yet, marking as cancelled and waiting for polling...");
      this.setState({
        orderCancelled: true,
        orderStatus: 'CANCELLED',
        statusHistory: [
          ...this.state.statusHistory,
          { status: 'User requested cancellation', time: new Date().toLocaleTimeString() }
        ]
      });
      // Keep polling to get orderId, then cancel it
      return;
    }

    // If we have orderId, cancel it immediately
    console.log("[Cancel] Have orderId, cancelling immediately...");
    this.cancelOrderWithId(orderId);
  }

  cancelOrderWithId(orderId) {
    const user = AuthService.getCurrentUser();
    if (!user) {
      window.location.href = '/login';
      return;
    }

    console.log("[Cancel] Calling backend API to cancel order:", orderId, "for user:", user.id);

    // Call backend to cancel the order
    OrderService.cancelOrder(orderId, user.id)
      .then(response => {
        console.log("[Cancel] Backend response:", response.data);
        this.setState({
          orderCancelled: true,
          orderStatus: 'CANCELLED',
          statusHistory: [
            ...this.state.statusHistory,
            { status: 'CANCELLED', time: new Date().toLocaleTimeString() }
          ]
        });

        // Stop polling if active
        if (this.state.pollingInterval) {
          clearInterval(this.state.pollingInterval);
          this.setState({ pollingInterval: null });
        }
      })
      .catch(error => {
        console.error("[Cancel] Error cancelling order:", error);
        const resMessage =
          (error.response &&
            error.response.data &&
            error.response.data.message) ||
          error.message ||
          error.toString();

        this.setState({
          message: "Failed to cancel order: " + resMessage
        });

        // Stop polling if active
        if (this.state.pollingInterval) {
          clearInterval(this.state.pollingInterval);
          this.setState({ pollingInterval: null });
        }
      });
  }


  placeOrder() {
    const user = AuthService.getCurrentUser();
    if (!user) {
      window.location.href = '/login';
      return;
    }

    // Get current order count before placing order
    OrderService.getUserOrders(user.id)
      .then(response => {
        const orders = response.data.data || [];
        const currentOrderCount = orders.length;
        console.log("[PlaceOrder] Current order count:", currentOrderCount);

        // Show modal immediately with "Creating order" status
        this.setState({
          loading: false,
          message: "",
          statusHistory: [{ status: 'Creating order', time: new Date().toLocaleTimeString() }],
          showModal: true,
          success: true,
          orderStatus: 'CREATING_ORDER',
          orderId: null,
          orderCancelled: false,
          previousOrderCount: currentOrderCount
        });

        // Start polling for the order (check every 2 seconds)
        const interval = setInterval(this.pollForOrder, 2000);
        this.setState({ pollingInterval: interval });

        this.proceedWithOrder(user);
      })
      .catch(error => {
        console.error("[PlaceOrder] Error getting current orders:", error);
        // Proceed anyway with count = 0
        this.setState({
          loading: false,
          message: "",
          statusHistory: [{ status: 'Creating order', time: new Date().toLocaleTimeString() }],
          showModal: true,
          success: true,
          orderStatus: 'CREATING_ORDER',
          orderId: null,
          orderCancelled: false,
          previousOrderCount: 0
        });

        const interval = setInterval(this.pollForOrder, 2000);
        this.setState({ pollingInterval: interval });

        this.proceedWithOrder(user);
      });
  }

  proceedWithOrder(user) {
    const orderItems = [{
      productId: this.state.product.productId,
      quantity: this.state.quantity
    }];

    console.log("[PlaceOrder] Sending order request to backend...");

    OrderService.placeOrder(user.id, orderItems)
      .then(response => {
        console.log("[PlaceOrder] Backend response received:", response.data);

        // Check if user cancelled while waiting
        if (this.state.orderCancelled) {
          console.log("[PlaceOrder] User cancelled, ignoring response");
          return;
        }

        const data = response.data.data || response.data;

        // If there's no orderId, it means the order failed before creation
        if (!data.orderId && data.message) {
          console.log("[PlaceOrder] Order failed:", data.message);
          this.setState({
            success: false,
            message: data.message || "Order placement failed.",
            orderStatus: 'FAILED',
            statusHistory: [
              { status: 'FAILED', time: new Date().toLocaleTimeString() }
            ]
          });

          // Stop polling
          if (this.state.pollingInterval) {
            clearInterval(this.state.pollingInterval);
            this.setState({ pollingInterval: null });
          }
        }
        // Otherwise, polling will handle status updates
        console.log("[PlaceOrder] Polling will handle status updates");
        localStorage.removeItem('selectedProduct');
      })
      .catch(error => {
        console.error("[PlaceOrder] Error placing order:", error);

        // Check if user cancelled while waiting
        if (this.state.orderCancelled) {
          console.log("[PlaceOrder] User cancelled, ignoring error");
          return;
        }

        const resMessage =
          (error.response &&
            error.response.data &&
            error.response.data.message) ||
          error.message ||
          error.toString();

        // Order placement failed
        this.setState({
          success: false,
          message: resMessage,
          orderStatus: 'FAILED',
          statusHistory: [
            { status: 'FAILED', time: new Date().toLocaleTimeString() }
          ]
        });

        // Stop polling
        if (this.state.pollingInterval) {
          clearInterval(this.state.pollingInterval);
          this.setState({ pollingInterval: null });
        }
      });
  }

  getStatusDisplay(status) {
    const statusMap = {
      'CREATING_ORDER': 'Creating Order',
      'AWAITING_PAYMENT': 'Awaiting Payment',
      'PENDING_VALIDATION': 'Validating Order',
      'PENDING_PAYMENT': 'Processing Payment',
      'PAYMENT_SUCCESSFUL': 'Payment Successful',
      'DELIVERY_REQUESTED': 'Requesting Delivery',
      'PICKED_UP': 'Package Picked Up',
      'IN_TRANSIT': 'In Transit',
      'DELIVERED': 'Delivered',
      'CANCELLED': 'Cancelled',
      'FAILED': 'Payment Failed',
      'LOST': 'Lost'
    };
    return statusMap[status] || status;
  }

  getStatusIcon(status) {
    if (status === 'PAYMENT_SUCCESSFUL') {
      return '✓';
    } else if (status === 'FAILED' || status === 'CANCELLED') {
      return '✗';
    } else if (status === 'AWAITING_PAYMENT') {
      return '...';
    } else {
      return '•';
    }
  }

  render() {
    const { product, quantity, loading, message, success, showModal, orderStatus, orderId, statusHistory, orderCancelled } = this.state;

    if (!product) {
      return null;
    }

    const total = product.price * quantity;
    const isErrorState = orderStatus === 'FAILED';
    const isCancelled = orderStatus === 'CANCELLED';

    // Terminal states where we stop polling
    const terminalStates = ['DELIVERY_REQUESTED', 'DELIVERED', 'CANCELLED', 'FAILED', 'LOST'];
    const isTerminalState = orderStatus && terminalStates.includes(orderStatus);

    // Show spinner for non-terminal states
    const showSpinner = !isTerminalState;

    // Can cancel before delivery request is sent
    const canCancel = orderStatus && [
      'CREATING_ORDER',
      'AWAITING_PAYMENT',
      'PENDING_VALIDATION',
      'PENDING_PAYMENT',
      'PAYMENT_SUCCESSFUL'
    ].includes(orderStatus);

    return (
      <div className="submit-form">
        <h3>Checkout</h3>
        <div className="card">
          <div className="card-body">
            <h5>Order Summary</h5>
            <div className="mb-3">
              <p><strong>Product:</strong> {product.name}</p>
              <p><strong>Price:</strong> ${product.price}</p>
              <div className="mb-2">
                <label htmlFor="quantity"><strong>Quantity:</strong></label>
                <input
                  type="number"
                  id="quantity"
                  className="form-control"
                  style={{ width: '150px' }}
                  value={quantity}
                  min="1"
                  max={product.maxStock}
                  onChange={this.updateQuantity}
                />
                <small className="text-muted">Available stock: {product.maxStock}</small>
              </div>
              <hr />
              <p><strong>Total:</strong> ${total.toFixed(2)}</p>
            </div>
            <button
              className="btn btn-success"
              onClick={this.placeOrder}
              disabled={loading || showModal}
            >
              {loading ? (
                <span>
                  <span className="spinner-border spinner-border-sm"></span>
                  {' '}Processing...
                </span>
              ) : (
                'Place Order'
              )}
            </button>
            <a href="/products" className="btn btn-secondary ms-2">
              Back to Products
            </a>
          </div>
        </div>

        {/* Order Status Modal */}
        {showModal && (
          <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
            <div className="modal-dialog modal-dialog-centered">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title">
                    {isErrorState ? 'Order Update' : success ? 'Order Processing' : 'Order Error'}
                  </h5>
                </div>
                <div className="modal-body">
                  <div>
                    {orderId ? (
                      <div>
                        <p><strong>Order ID:</strong> #{orderId}</p>
                        <hr />
                      </div>
                    ) : (
                      <div className="alert alert-info">
                        <small>Waiting for order to be created... You can cancel anytime.</small>
                      </div>
                    )}

                    <div className="mb-3">
                      <h6>Current Status:</h6>
                      <div className={`alert ${isErrorState ? 'alert-danger' : isCancelled ? 'alert-warning' : orderStatus === 'PAYMENT_SUCCESSFUL' ? 'alert-success' : 'alert-info'} d-flex align-items-center`}>
                        <span className="fs-4 me-2">{this.getStatusIcon(orderStatus)}</span>
                        <strong>{this.getStatusDisplay(orderStatus)}</strong>
                      </div>
                    </div>

                    {isErrorState && (
                      <div className="alert alert-warning">
                        <strong>{message}</strong>
                        <p className="mb-0 mt-2">Your refund will be returned to your bank account.</p>
                      </div>
                    )}

                    {isCancelled && (
                      <div className="alert alert-info">
                        <strong>Order cancelled by user.</strong>
                        <p className="mb-0 mt-2">Any charges will be refunded to your bank account.</p>
                      </div>
                    )}

                    {orderStatus === 'DELIVERY_REQUESTED' && (
                      <div className="alert alert-success">
                        <strong>Order placed successfully! Redirecting to order details in 3 seconds...</strong>
                      </div>
                    )}

                    {showSpinner && (
                      <div className="text-center">
                        <div className="spinner-border text-primary" role="status">
                          <span className="visually-hidden">Loading...</span>
                        </div>
                        <p className="mt-2 text-muted">Processing your order, please wait...</p>
                      </div>
                    )}

                    <div className="mt-3">
                      <h6>Status History:</h6>
                      <ul className="list-group">
                        {statusHistory.map((item, index) => (
                          <li key={index} className="list-group-item d-flex justify-content-between align-items-center">
                            <span>{this.getStatusDisplay(item.status)}</span>
                            <small className="text-muted">{item.time}</small>
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>
                </div>
                <div className="modal-footer">
                  {canCancel ? (
                    <button className="btn btn-danger" onClick={this.cancelOrder}>
                      Cancel Order
                    </button>
                  ) : isErrorState || isCancelled ? (
                    <button className="btn btn-secondary" onClick={() => this.setState({ showModal: false })}>
                      Close
                    </button>
                  ) : (
                    <small className="text-muted">
                      {orderStatus === 'DELIVERY_REQUESTED'
                        ? 'Delivery request sent, order cannot be cancelled'
                        : 'Processing...'}
                    </small>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }
}
