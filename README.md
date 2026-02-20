# loan-analyzer
Application Analyzer

This repository contains:

- `loan-analyzer-svc`: Backend (Clojure + Clara + Pedestal)
- `loan-analyzer-ui`: Frontend (Solid.js + Vite)

## Backend

```bash
cd loan-analyzer-svc
clojure -M:run
```

Backend listens on **http://localhost:8090**.

## Frontend

```bash
cd loan-analyzer-ui
npm install
npm run dev
```

Open **http://localhost:5173**.

The UI submits the form to `POST /api/v1/evaluate`.
During development, Vite proxies `/api/*` to `http://localhost:8090`.
