# Deployment Guide

## Local development

```bash
cd server
npm install
npm start
```

Then use the app with:
- emulator: `http://10.0.2.2:3000`
- physical device: `http://<your-laptop-ip>:3000`

## Cloud deployment (AWS EC2 or similar)

1. Provision Ubuntu VM
2. Install Node.js 20+
3. Copy project to the VM
4. Run:

```bash
cd /path/to/Testt/server
npm install
PORT=3000 npm start
```

5. Open the required port in security group / firewall
6. Update your Android app server URL to the public IP or domain

## Reverse proxy example (Nginx)

```nginx
server {
    listen 80;
    server_name example.com;

    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
```

## Production hardening

- Use HTTPS with certs
- Implement user auth and session tokens
- Add rate limiting and logging
- Add monitoring and health checks
- Move packet forwarding to a proper virtual tunnel
