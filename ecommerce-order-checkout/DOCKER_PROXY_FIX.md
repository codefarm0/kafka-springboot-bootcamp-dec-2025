# Docker Proxy Configuration Fix

## Problem
Docker is timing out when pulling images due to proxy configuration:
```
Error: proxyconnect tcp: dial tcp 192.168.65.1:3128: i/o timeout
```

## Solution 1: Disable Proxy in Docker Desktop (Recommended)

1. Open **Docker Desktop**
2. Click the **Settings** (gear) icon
3. Navigate to **Resources** → **Proxies**
4. Select **"No proxy"** or uncheck **"Manual proxy configuration"**
5. Click **"Apply & Restart"**
6. Wait for Docker to restart
7. Retry: `docker-compose up -d`

## Solution 2: Fix Proxy Configuration

If you need a proxy, ensure it's correctly configured:

1. In Docker Desktop → Settings → Resources → Proxies
2. Set:
   - **HTTP Proxy**: `http://your-proxy-host:port` (or leave empty)
   - **HTTPS Proxy**: `http://your-proxy-host:port` (or leave empty)
   - **No Proxy**: `localhost,127.0.0.1,*.local`
3. Click **"Apply & Restart"**

## Solution 3: Pull Images Manually First

If proxy issues persist, pull images manually:

```bash
# Pull all required images
docker pull apache/kafka:4.1.1
docker pull mysql:9.5.0
docker pull debezium/connect:3.0.0.Final
docker pull obsidiandynamics/kafdrop:4.2.0

# Then run docker-compose
docker-compose up -d
```

## Solution 4: Use Alternative Image Registry

If Docker Hub is blocked, you can use alternative registries or mirrors by modifying `docker-compose.yml` to use different image sources.

## Verify Fix

After applying the fix, verify Docker can pull images:
```bash
docker pull hello-world
docker run hello-world
```

If this works, retry your docker-compose command.

