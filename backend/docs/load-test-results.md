# Load test results

All numbers below were measured on a single development laptop. They describe this setup only and are not production capacity claims.

## Environment

| Item | Value |
|---|---|
| Machine | `<fill in: CPU model, cores, RAM>` |
| OS | Windows 11 |
| Runtime | Docker Desktop (VM memory limit: 3532 MB), `docker compose up` |
| App | Spring Boot, Java 21, default scheduler config (`batch-size: 50`, `tick-ms: 5000`, 10 workers) |
| Database | PostgreSQL 16 (container) |
| Load tool | k6 v2.2.0, running on the same machine |
| Target of monitors | Built-in mock API on the same app (`/mock/inventory/{id}`) |

k6, the app, the database and the mock target all share one machine, so results are conservative for CPU but not representative of a networked deployment.

## Test 1: scheduler throughput

**Setup:** 200 monitors created with a k6 script (`loadtest/create-monitors.js`), each with a 10 s interval, then left running.

**Measurement:** checks per minute, counted from `check_results`:

```sql
SELECT date_trunc('minute', started_at) AS minute, COUNT(*)
FROM check_results
WHERE started_at > NOW() - INTERVAL '8 minutes'
GROUP BY 1 ORDER BY 1;
```

| Minute | Checks |
|---|---|
| 20:39 | 200 (partial minute, ignored) |
| 20:40 | 600 |
| 20:41 | 600 |
| 20:42 | 600 |
| 20:43 | 600 |
| 20:44 | 50 (partial minute, ignored) |

**Result: 600 checks/min**, stable across 4 full minutes. The worker queue was never full (0 "queue is full" log lines) and all checks passed.

**Interpretation:** the ideal load for 200 monitors at 10 s is 1,200 checks/min. The measured 600 equals the scheduler's configured cap (50 monitors per tick x 12 ticks per minute). The bottleneck is configuration (`batch-size`), not hardware: the average check took about 21 ms. Raising `batch-size` or lowering `tick-ms` is expected to raise throughput, but this was not tested.

## Test 2: API latency under concurrent load

**Setup:** `loadtest/read-api.js`, 50 constant virtual users for 1 minute, each requesting `GET /api/v1/monitors/{id}/results?page=0&size=20` for a random monitor id (1 to 200). The scheduler was running in the background at the same time.

*Measured with JWT authentication and per-monitor ownership checks enabled.*

| Metric | Value |
|---|---|
| Total requests | 55,829 |
| Throughput | ~930 req/s |
| Failed requests | 0.00% |
| Latency avg | 53.4 ms |
| Latency median | 43.2 ms |
| Latency p90 | 97.6 ms |
| **Latency p95** | **119.0 ms** |
| Latency max | 1.67 s |

## Limitations

- Single machine, single run per test. No repeated runs or confidence intervals.
- Small dataset: `check_results` held only a few thousand rows during the read test, so this does not show behaviour with millions of rows (indexing and partitioning at that scale are untested).
- Scheduler throughput during the read test was not re-measured separately.
- Mock target only: real targets add network latency and failure modes.
- Not a test of failure scenarios such as slow targets or a full worker queue.

## Reproduce

```bash
docker compose down -v && docker compose up --build -d
k6 run loadtest/create-monitors.js
# wait ~5 minutes, then run the SQL above
k6 run loadtest/read-api.js
```
