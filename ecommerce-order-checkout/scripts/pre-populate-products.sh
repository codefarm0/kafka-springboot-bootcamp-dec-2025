#!/bin/bash

# Script to pre-populate products in inventory service
# Usage: ./pre-populate-products.sh [inventory-service-url]

INVENTORY_SERVICE_URL=${1:-http://localhost:8082}

echo "Pre-populating products in inventory service at $INVENTORY_SERVICE_URL"
echo ""

# Array of products to add
products=(
    '{"productId":"product-1","productName":"Sample Product 1","description":"A sample product for testing","price":29.99,"quantity":100}'
    '{"productId":"product-2","productName":"Sample Product 2","description":"Another sample product","price":49.99,"quantity":50}'
    '{"productId":"product-3","productName":"Sample Product 3","description":"Yet another sample product","price":19.99,"quantity":200}'
    '{"productId":"product-4","productName":"Premium Product","description":"A premium quality product","price":99.99,"quantity":25}'
    '{"productId":"product-5","productName":"Budget Product","description":"An affordable product","price":9.99,"quantity":500}'
)

# Function to add a product
add_product() {
    local product_json=$1
    local response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$product_json" \
        "$INVENTORY_SERVICE_URL/api/products")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 201 ] || [ "$http_code" -eq 200 ]; then
        echo "✅ Successfully added product"
        return 0
    else
        echo "❌ Failed to add product (HTTP $http_code): $body"
        return 1
    fi
}

# Add each product
success_count=0
fail_count=0

for product in "${products[@]}"; do
    product_id=$(echo "$product" | grep -o '"productId":"[^"]*"' | cut -d'"' -f4)
    product_name=$(echo "$product" | grep -o '"productName":"[^"]*"' | cut -d'"' -f4)
    
    echo "Adding product: $product_name ($product_id)..."
    
    if add_product "$product"; then
        ((success_count++))
    else
        ((fail_count++))
    fi
    echo ""
done

echo "=========================================="
echo "Pre-population complete!"
echo "✅ Successfully added: $success_count products"
echo "❌ Failed: $fail_count products"
echo "=========================================="

if [ $fail_count -eq 0 ]; then
    exit 0
else
    exit 1
fi

