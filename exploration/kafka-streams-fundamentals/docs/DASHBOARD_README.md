# Kafka Streams Dashboard

A web-based dashboard for visualizing Kafka Streams analytics and simulating order load.

## Features

### 📊 Analytics Dashboard
- **Real-time Statistics**: View total orders, revenue, and customer count
- **Order Counts**: See order counts per customer in a table
- **Total Amounts**: View total order amounts per customer
- **Auto-refresh**: Optional 5-second auto-refresh for real-time updates

### 🛒 Order Placement
- **Single Order Form**: Place individual orders with customer, product, quantity, and amount
- **Bulk Order Simulation**: Simulate multiple orders with configurable parameters:
  - Customer ID
  - Number of orders
  - Min/Max amount range
  - Delay between orders

## Accessing the Dashboard

### Start the Application

```bash
cd exploration/kafka-streams-fundamentals
./gradlew bootRun
```

### Open in Browser

- **Dashboard**: http://localhost:8089/dashboard
- **Order Form**: http://localhost:8089/dashboard/order-form
- **Root**: http://localhost:8089/ (redirects to dashboard)

## Usage

### Viewing Analytics

1. Navigate to http://localhost:8089/dashboard
2. View real-time statistics at the top:
   - Total Orders
   - Total Revenue
   - Total Customers
3. Scroll down to see detailed tables:
   - Order Counts by Customer
   - Total Amounts by Customer
4. Click "🔄 Refresh" to manually update or enable "Auto-refresh (5s)"

### Placing Orders

1. Navigate to http://localhost:8089/dashboard/order-form
2. Fill in the form:
   - **Customer ID**: e.g., `customer-123`
   - **Product ID**: e.g., `product-456`
   - **Quantity**: Number of items
   - **Total Amount**: Order total in ₹
3. Click "Place Order"
4. See success/error message

### Bulk Order Simulation

1. Navigate to http://localhost:8089/dashboard/order-form
2. Scroll to "Bulk Order Simulation" section
3. Configure parameters:
   - **Customer ID**: Customer to simulate orders for
   - **Orders**: Number of orders to create (1-100)
   - **Min Amount**: Minimum order amount
   - **Max Amount**: Maximum order amount
   - **Delay (ms)**: Delay between orders in milliseconds
4. Click "🚀 Start Simulation"
5. Watch the loading indicator and wait for completion
6. Navigate back to dashboard to see updated analytics

## Example Workflow

### 1. Start Fresh

```bash
# Start Kafka (if not running)
docker-compose up -d

# Start application
./gradlew bootRun
```

### 2. Simulate Orders

1. Open http://localhost:8089/dashboard/order-form
2. Use bulk simulation:
   - Customer ID: `customer-123`
   - Orders: `20`
   - Min Amount: `50`
   - Max Amount: `500`
   - Delay: `200` ms
3. Click "🚀 Start Simulation"

### 3. View Analytics

1. Open http://localhost:8089/dashboard
2. Enable "Auto-refresh (5s)"
3. Watch analytics update in real-time as orders are processed

### 4. Test Different Customers

1. Go back to order form
2. Change Customer ID to `customer-456`
3. Simulate more orders
4. Return to dashboard to see multiple customers

## Dashboard Features

### Statistics Cards
- **Total Orders**: Sum of all order counts across customers
- **Total Revenue**: Sum of all total amounts across customers
- **Total Customers**: Number of unique customers with orders

### Data Tables
- **Order Counts**: Shows how many orders each customer has placed
- **Total Amounts**: Shows total spending per customer

### Auto-Refresh
- Toggle auto-refresh to update dashboard every 5 seconds
- Useful for monitoring real-time stream processing
- Can be disabled to reduce server load

## Technical Details

### Backend Integration
- Uses existing REST API endpoints (`/api/orders`, `/api/analytics/*`)
- No changes to backend code required
- Dashboard controller fetches data from analytics controller

### Frontend
- **Thymeleaf Templates**: Server-side rendering
- **Vanilla JavaScript**: No external dependencies
- **Responsive Design**: Works on desktop and mobile
- **Modern CSS**: Gradient backgrounds, card layouts, smooth transitions

### State Management
- Dashboard shows current state from Kafka Streams state stores
- Data is fetched on page load and refresh
- Real-time updates require page refresh or auto-refresh

## Troubleshooting

### Dashboard Shows No Data

**Cause**: No orders have been placed yet

**Solution**: 
1. Navigate to order form
2. Place some orders or run bulk simulation
3. Refresh dashboard

### Auto-Refresh Not Working

**Cause**: Browser may block auto-refresh or page is not focused

**Solution**: 
- Manually refresh using "🔄 Refresh" button
- Check browser console for errors

### Orders Not Appearing in Analytics

**Cause**: Kafka Streams may not be in RUNNING state yet

**Solution**:
1. Check application logs for state transitions
2. Wait for streams to reach RUNNING state
3. Ensure Kafka brokers are running
4. Verify orders are being sent to `orders` topic

### 503 Service Unavailable

**Cause**: Kafka Streams not ready or state stores not available

**Solution**:
1. Wait for application to fully start
2. Check that Kafka Streams state is RUNNING
3. Verify state stores are initialized (check logs)

## Next Steps

- Add charts/graphs for better visualization
- Add real-time WebSocket updates
- Add filtering and search capabilities
- Add export functionality (CSV, JSON)
- Add historical data views
- Add windowed analytics visualization

