#!/bin/bash

# Deploy Debezium MySQL Connector for simple JSON events
# This script configures the Debezium connector for Order DB
# Events are stored as JSON strings in the payload field with source, eventType, eventId, eventTime, and data

CONNECTOR_NAME="order-service-connector"
KAFKA_CONNECT_URL="http://localhost:8083"

echo "Deploying Debezium MySQL Connector for simple JSON events..."

# Check if connector already exists
EXISTING=$(curl -s "$KAFKA_CONNECT_URL/connectors/$CONNECTOR_NAME")
if [ "$EXISTING" != "null" ] && [ ! -z "$EXISTING" ]; then
    echo "Connector $CONNECTOR_NAME already exists. Deleting..."
    curl -X DELETE "$KAFKA_CONNECT_URL/connectors/$CONNECTOR_NAME"
    sleep 2
fi

# Deploy connector configuration
curl -X POST "$KAFKA_CONNECT_URL/connectors" \
  -H "Content-Type: application/json" \
  -d @- <<EOF
{
  "name": "$CONNECTOR_NAME",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "tasks.max": "1",
    "snapshot.mode": "when_needed",
    "database.hostname": "order-db",
    "database.port": "3306",
    "database.user": "debezium",
    "database.password": "debezium",
    "database.server.id": "184054",
    "database.server.name": "order-db-server",
    "database.include.list": "order_db",
    "table.include.list": "order_db.outbox_events",
    "database.history.kafka.bootstrap.servers": "kafka:29092",
    "database.history.kafka.topic": "order-db-schema-changes",
    "database.history.store.only.monitored.tables.ddl": "true",
    "schema.history.internal.kafka.bootstrap.servers": "kafka:29092",
    "schema.history.internal.kafka.topic": "order-db-schema-changes",
    "topic.prefix": "order-db-server",
    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.route.topic.replacement": "orders",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.id": "id",
    "transforms.outbox.table.field.event.payload": "payload",
    "transforms.outbox.table.field.event.type": "event_type",
    "transforms.outbox.route.by.field": "event_type",
    "transforms.outbox.table.expand.json.payload": "true",
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter.schemas.enable": "false"
  }
}
EOF

echo ""
echo "Connector deployment completed!"
echo "Check connector status: curl $KAFKA_CONNECT_URL/connectors/$CONNECTOR_NAME/status"

